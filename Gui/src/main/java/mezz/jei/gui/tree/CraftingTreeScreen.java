package mezz.jei.gui.tree;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.Internal;
import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import mezz.jei.gui.bookmarks.BookmarkItemMetadataFactory;
import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.bookmarks.chain.RecipeChainInput;
import mezz.jei.gui.overlay.bookmarks.BookmarkAmountFormatter;
import mezz.jei.gui.overlay.bookmarks.PlayerInventoryRecipeChainTooltipInventoryProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Main Crafting Tree GUI Screen.
 * Fully interactive node hierarchy with pan, zoom, collapse/expand, manual recipe selection,
 * manual quantity overrides, bottom stats bar, bidirectional chain export, and complete BigInteger precision.
 *
 * Supports opening directly from bookmark groups, or standalone starting from any target ingredient/recipe.
 *
 * Ported and heavily adapted/refactored from nei-features-for-jei and GTNH NotEnoughItems (LGPL-3.0-or-later).
 */
public final class CraftingTreeScreen extends Screen {
	private static final int TOP_BAR_HEIGHT = 30;
	private static final int STATS_BAR_HEIGHT = 32;

	private final @Nullable Screen parentScreen;
	private final BookmarkList bookmarks;
	private int groupId;
	private final IIngredientManager ingredientManager;
	private final PlayerInventoryRecipeChainTooltipInventoryProvider inventoryProvider;

	// Target-ingredient mode fields
	private final @Nullable ITypedIngredient<?> targetIngredient;
	private BigInteger targetAmount;
	private @Nullable ResourceLocation targetRecipeOverride;
	private final Map<BookmarkIngredientKey, ResourceLocation> manualRecipes = new LinkedHashMap<>();
	private final Set<BookmarkIngredientKey> prunedKeys = new HashSet<>();

	private final TreeCamera camera = new TreeCamera();
	private final TreeLayout layout = new TreeLayout();
	private final TreeStatsLayout statsLayout = new TreeStatsLayout();

	private TreeMath treeMath;
	private TreeGraph graph;
	private TreeStats stats;

	private boolean checkInventory = true;
	private boolean showStats = true;
	private String searchQuery = "";
	private @Nullable EditBox searchBox;

	private @Nullable TreeSlot hoveredNode;
	private @Nullable TreeSlot hoveredCollapseBtn;
	private @Nullable TreeStatsLayout.PlacedSlot hoveredStatsSlot;
	private int statsScrollOffset = 0;

	public static void open(Screen parent, BookmarkList bookmarks, int groupId) {
		IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();
		Minecraft.getInstance().setScreen(new CraftingTreeScreen(parent, bookmarks, groupId, ingredientManager));
	}

	public static void open(Screen parent, BookmarkList bookmarks, int groupId, IIngredientManager ingredientManager) {
		Minecraft.getInstance().setScreen(new CraftingTreeScreen(parent, bookmarks, groupId, ingredientManager));
	}

	public static void openForIngredient(Screen parent, BookmarkList bookmarks, ITypedIngredient<?> target, IIngredientManager ingredientManager) {
		Minecraft.getInstance().setScreen(new CraftingTreeScreen(parent, bookmarks, target, BigInteger.ONE, null, ingredientManager));
	}

	public static void openForRecipe(Screen parent, BookmarkList bookmarks, ITypedIngredient<?> target, ResourceLocation recipeUid, IIngredientManager ingredientManager) {
		Minecraft.getInstance().setScreen(new CraftingTreeScreen(parent, bookmarks, target, BigInteger.ONE, recipeUid, ingredientManager));
	}

	public CraftingTreeScreen(
		@Nullable Screen parentScreen,
		BookmarkList bookmarks,
		int groupId,
		IIngredientManager ingredientManager
	) {
		super(Component.translatable("jei.tree.title"));
		this.parentScreen = parentScreen;
		this.bookmarks = bookmarks;
		this.groupId = groupId;
		this.ingredientManager = ingredientManager;
		this.inventoryProvider = new PlayerInventoryRecipeChainTooltipInventoryProvider(Minecraft.getInstance(), ingredientManager);
		this.targetIngredient = null;
		this.targetAmount = BigInteger.ONE;
		this.targetRecipeOverride = null;

		rebuildTreeMath();
	}

	public CraftingTreeScreen(
		@Nullable Screen parentScreen,
		BookmarkList bookmarks,
		ITypedIngredient<?> targetIngredient,
		BigInteger targetAmount,
		@Nullable ResourceLocation targetRecipeOverride,
		IIngredientManager ingredientManager
	) {
		super(Component.translatable("jei.tree.title"));
		this.parentScreen = parentScreen;
		this.bookmarks = bookmarks;
		this.groupId = -1;
		this.ingredientManager = ingredientManager;
		this.inventoryProvider = new PlayerInventoryRecipeChainTooltipInventoryProvider(Minecraft.getInstance(), ingredientManager);
		this.targetIngredient = targetIngredient;
		this.targetAmount = targetAmount.max(BigInteger.ONE);
		this.targetRecipeOverride = targetRecipeOverride;

		rebuildTreeMath();
	}

	private void rebuildTreeMath() {
		Map<BookmarkIngredientKey, BigInteger> stock = checkInventory ? readInventory() : Map.of();
		if (targetIngredient != null) {
			List<RecipeChainInput> inputs = TreeRecipeBuilder.buildInputsForTarget(
				targetIngredient,
				targetAmount,
				targetRecipeOverride,
				manualRecipes,
				prunedKeys,
				ingredientManager
			);
			this.treeMath = new TreeMath(inputs, stock);
		} else {
			this.treeMath = TreeChainBridge.createTreeMathFromGroup(bookmarks, groupId, stock);
		}
		this.graph = treeMath.calculate();
		this.layout.recalculate(graph, null);
		this.stats = TreeStats.compute(graph);
		this.statsLayout.recalculate(stats);
	}

	private Map<BookmarkIngredientKey, BigInteger> readInventory() {
		Map<BookmarkIngredientKey, BigInteger> stock = new HashMap<>();
		for (ItemStack stack : inventoryProvider.getAvailableStacks()) {
			if (!stack.isEmpty()) {
				ingredientManager.createTypedIngredient(VanillaTypes.ITEM_STACK, stack.copy())
					.ifPresent(typed -> {
						BookmarkIngredientKey key = BookmarkItemMetadataFactory.createPermutationKey(typed, ingredientManager);
						stock.merge(key, BigInteger.valueOf(stack.getCount()), BigInteger::add);
					});
			}
		}
		return stock;
	}

	@Override
	protected void init() {
		int viewportH = height - TOP_BAR_HEIGHT - (showStats ? STATS_BAR_HEIGHT : 0);
		camera.setViewport(0, TOP_BAR_HEIGHT, width, viewportH);

		// Back Button
		addRenderableWidget(Button.builder(Component.literal("<"), b -> onClose())
			.bounds(6, 5, 20, 20)
			.tooltip(Tooltip.create(Component.translatable("gui.back")))
			.build());

		// Fit to View Button
		addRenderableWidget(Button.builder(Component.literal("[]"), b -> fitToView())
			.bounds(28, 5, 20, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.fit")))
			.build());

		// Zoom Out Button
		addRenderableWidget(Button.builder(Component.literal("-"), b -> camera.zoomAt(width / 2.0, height / 2.0, -1))
			.bounds(50, 5, 20, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.zoom_out")))
			.build());

		// Zoom In Button
		addRenderableWidget(Button.builder(Component.literal("+"), b -> camera.zoomAt(width / 2.0, height / 2.0, 1))
			.bounds(72, 5, 20, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.zoom_in")))
			.build());

		// Inventory Toggle Button
		addRenderableWidget(Button.builder(
				Component.literal(checkInventory ? "Inv:ON" : "Inv:OFF"),
				b -> {
					checkInventory = !checkInventory;
					b.setMessage(Component.literal(checkInventory ? "Inv:ON" : "Inv:OFF"));
					recalculateTree(null);
				}
			)
			.bounds(94, 5, 48, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.inventory_toggle")))
			.build());

		// Collapse/Expand All Button
		addRenderableWidget(Button.builder(Component.literal("+/-"), b -> toggleAllCollapse())
			.bounds(144, 5, 24, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.toggle_all")))
			.build());

		// Stats Bar Toggle Button
		addRenderableWidget(Button.builder(
				Component.literal("="),
				b -> {
					showStats = !showStats;
					int newH = height - TOP_BAR_HEIGHT - (showStats ? STATS_BAR_HEIGHT : 0);
					camera.setViewport(0, TOP_BAR_HEIGHT, width, newH);
				}
			)
			.bounds(170, 5, 20, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.stats_toggle")))
			.build());

		// Save/Export to Chain Button
		addRenderableWidget(Button.builder(
				Component.translatable("jei.tree.save_chain"),
				b -> exportToBookmarkChain()
			)
			.bounds(192, 5, 52, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.save_chain.tooltip")))
			.build());

		// Search Box
		searchBox = new EditBox(font, width - 130, 6, 124, 18, Component.literal("Search"));
		searchBox.setMaxLength(32);
		searchBox.setValue(searchQuery);
		searchBox.setResponder(text -> this.searchQuery = text.trim().toLowerCase(Locale.ROOT));
		addRenderableWidget(searchBox);

		fitToView();
	}

	private void recalculateTree(@Nullable TreeSlot anchor) {
		this.graph = treeMath.calculate();
		this.layout.recalculate(graph, anchor);
		this.stats = TreeStats.compute(graph);
		this.statsLayout.recalculate(stats);
	}

	private void fitToView() {
		if (layout.getTreeWidth() <= 0 || layout.getTreeHeight() <= 0) {
			return;
		}
		float scaleX = (float) camera.getViewportW() / (layout.getTreeWidth() + 32);
		float scaleY = (float) camera.getViewportH() / (layout.getTreeHeight() + 32);
		float targetZoom = Math.min(1.5f, Math.min(scaleX, scaleY));
		camera.setZoomLevel(targetZoom);
		camera.centerOn(layout.getTreeWidth() / 2.0f, layout.getTreeHeight() / 2.0f);
	}

	private void toggleAllCollapse() {
		boolean anyExpanded = false;
		for (TreeSlot node : graph.getItems()) {
			if (graph.hasChildren(node) && !node.isCollapsed()) {
				anyExpanded = true;
				break;
			}
		}

		boolean nextCollapsed = anyExpanded;
		for (TreeSlot node : graph.getItems()) {
			if (graph.hasChildren(node)) {
				node.setCollapsed(nextCollapsed);
				if (node.getRecipeUid() != null) {
					if (nextCollapsed) {
						treeMath.toggleRecipeCollapsed(node.getRecipeUid());
					} else {
						treeMath.getCollapsedRecipes();
					}
				}
			}
		}
		recalculateTree(null);
	}

	private void exportToBookmarkChain() {
		if (groupId >= 0) {
			TreeChainBridge.exportTreeToBookmarkGroup(bookmarks, groupId, graph, treeMath.getCollapsedRecipes());
			if (minecraft != null && minecraft.player != null) {
				minecraft.player.displayClientMessage(Component.translatable("jei.tree.save_chain.success").withStyle(ChatFormatting.GREEN), false);
			}
		} else {
			Optional<Integer> newGid = TreeChainBridge.saveTreeAsNewBookmarkGroup(bookmarks, graph, ingredientManager);
			if (newGid.isPresent()) {
				this.groupId = newGid.get();
				if (minecraft != null && minecraft.player != null) {
					minecraft.player.displayClientMessage(Component.translatable("jei.tree.save_chain.success").withStyle(ChatFormatting.GREEN), false);
				}
			}
		}
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parentScreen);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);

		// 1. Draw Canvas (Tree nodes and connection lines)
		renderCanvas(g, mouseX, mouseY);

		// 2. Draw Top Bar Background
		g.fill(0, 0, width, TOP_BAR_HEIGHT, 0xEE1E1E1E);
		g.fill(0, TOP_BAR_HEIGHT - 1, width, TOP_BAR_HEIGHT, 0xFF444444);

		// 3. Draw Bottom Stats Bar
		if (showStats) {
			renderStatsBar(g, mouseX, mouseY);
		}

		// 4. Render UI Widgets (Buttons and Search Box)
		super.render(g, mouseX, mouseY, partialTick);

		// 5. Draw Tooltips
		renderTooltips(g, mouseX, mouseY);
	}

	private void renderCanvas(GuiGraphics g, int mouseX, int mouseY) {
		hoveredNode = null;
		hoveredCollapseBtn = null;

		double[] worldMouse = camera.toWorldPoint(mouseX, mouseY);
		int wmX = (int) Math.floor(worldMouse[0]);
		int wmY = (int) Math.floor(worldMouse[1]);

		// Enable scissor clipping for viewport
		g.enableScissor(camera.getViewportX(), camera.getViewportY(), camera.getViewportX() + camera.getViewportW(), camera.getViewportY() + camera.getViewportH());

		// 1. Draw Connection Lines (Optimized Frustum Culling)
		for (TreeSlot parent : graph.getItems()) {
			TreeLayout.Point parentPos = layout.getPosition(parent);
			if (parentPos == null || parent.isCollapsed()) {
				continue;
			}

			int pCenterX = parentPos.x + TreeLayout.SLOT_SIZE / 2;
			int pBottomY = parentPos.y + TreeLayout.SLOT_SIZE;

			for (TreeSlot child : graph.getChildren(parent)) {
				TreeLayout.Point childPos = layout.getPosition(child);
				if (childPos == null) {
					continue;
				}

				int cCenterX = childPos.x + TreeLayout.SLOT_SIZE / 2;
				int cTopY = childPos.y;

				int minX = Math.min(pCenterX, cCenterX);
				int maxX = Math.max(pCenterX, cCenterX);
				if (!camera.isWorldRectVisible(minX, pBottomY, maxX - minX + 2, cTopY - pBottomY + 2)) {
					continue;
				}

				int lineColor = child.isResolved() ? TreeDraw.COLOR_LINE_DEFAULT : (!child.hasRecipe() ? TreeDraw.COLOR_LINE_MISSING : TreeDraw.COLOR_LINE_CRAFTING);

				int screenPcenterX = camera.toScreenX(pCenterX);
				int screenPbottomY = camera.toScreenY(pBottomY);
				int screenCcenterX = camera.toScreenX(cCenterX);
				int screenCtopY = camera.toScreenY(cTopY);

				TreeDraw.drawConnectionLine(g, screenPcenterX, screenPbottomY, screenCcenterX, screenCtopY, lineColor);
			}
		}

		// 2. Draw Nodes
		for (TreeSlot slot : graph.getItems()) {
			TreeLayout.Point pos = layout.getPosition(slot);
			if (pos == null) {
				continue;
			}

			// Frustum culling check
			if (!camera.isWorldRectVisible(pos.x, pos.y, TreeLayout.SLOT_SIZE, TreeLayout.SLOT_SIZE + 10)) {
				continue;
			}

			int screenX = camera.toScreenX(pos.x);
			int screenY = camera.toScreenY(pos.y);
			int scaledSlotSize = Math.round(TreeLayout.SLOT_SIZE * camera.getZoomLevel());

			boolean isHover = camera.isInViewport(mouseX, mouseY) &&
				mouseX >= screenX && mouseX < screenX + scaledSlotSize &&
				mouseY >= screenY && mouseY < screenY + scaledSlotSize;

			if (isHover) {
				hoveredNode = slot;
			}

			boolean searchMatch = matchesSearch(slot);

			// Draw Slot Background & Borders
			TreeDraw.drawSlot(g, screenX, screenY, scaledSlotSize, slot, searchMatch, isHover);

			// Render Item inside Slot
			Optional<ITypedIngredient<?>> typedOpt = slot.getTypedIngredient();
			if (typedOpt.isPresent()) {
				ITypedIngredient<?> typed = typedOpt.get();
				int itemPad = Math.max(1, (scaledSlotSize - 16) / 2);
				renderTypedIngredient(g, typed, screenX + itemPad, screenY + itemPad);
			}

			// Render BigInteger Quantity Text
			renderSlotAmount(g, slot, screenX, screenY, scaledSlotSize);

			// Draw Collapse/Expand Button if node has children
			if (graph.hasChildren(slot)) {
				int collapseX = screenX + (scaledSlotSize - 9) / 2;
				int collapseY = screenY + scaledSlotSize;
				boolean isCollapseHover = camera.isInViewport(mouseX, mouseY) &&
					mouseX >= collapseX && mouseX < collapseX + 9 &&
					mouseY >= collapseY && mouseY < collapseY + 9;

				if (isCollapseHover) {
					hoveredCollapseBtn = slot;
				}

				TreeDraw.drawCollapseButton(g, font, collapseX, collapseY, slot.isCollapsed(), isCollapseHover);
			}
		}

		g.disableScissor();
	}

	private boolean matchesSearch(TreeSlot slot) {
		if (searchQuery.isEmpty()) {
			return false;
		}
		Optional<ITypedIngredient<?>> typedOpt = slot.getTypedIngredient();
		if (typedOpt.isPresent()) {
			ITypedIngredient<?> typed = typedOpt.get();
			// using getIngredientTooltip helper
			String name = getIngredientTooltip(typed).stream()
				.map(Component::getString)
				.reduce("", (a, b) -> a + " " + b)
				.toLowerCase(Locale.ROOT);
			return name.contains(searchQuery);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private <T> void renderTypedIngredient(GuiGraphics g, ITypedIngredient<T> typed, int x, int y) {
		IIngredientRenderer<T> renderer = ingredientManager.getIngredientRenderer(typed.getType());
		renderer.render(g, typed.getIngredient());
	}

	private <T> List<Component> getIngredientTooltip(ITypedIngredient<T> typed) {
		IIngredientRenderer<T> renderer = ingredientManager.getIngredientRenderer(typed.getType());
		return renderer.getTooltip(typed.getIngredient(), net.minecraft.world.item.TooltipFlag.NORMAL);
	}

	private void renderSlotAmount(GuiGraphics g, TreeSlot slot, int screenX, int screenY, int scaledSlotSize) {
		BigInteger amount = slot.getProducedAmount();
		if (amount.compareTo(BigInteger.ONE) <= 0) {
			return;
		}
		String text = BookmarkAmountFormatter.formatItemAmount(amount);
		int textWidth = font.width(text);
		int textX = screenX + scaledSlotSize - textWidth - 1;
		int textY = screenY + scaledSlotSize - 8;

		g.pose().pushPose();
		g.pose().translate(0, 0, 200);
		g.drawString(font, text, textX, textY, 0xFFFFFFFF, true);
		g.pose().popPose();
	}

	private void renderStatsBar(GuiGraphics g, int mouseX, int mouseY) {
		int barY = height - STATS_BAR_HEIGHT;
		g.fill(0, barY, width, height, 0xEE1A1A1A);
		g.fill(0, barY, width, barY + 1, 0xFF444444);

		hoveredStatsSlot = null;
		int drawX = 6 - statsScrollOffset;
		int drawY = barY + 4;

		for (TreeStatsLayout.PlacedSlot placed : statsLayout.getPlacedSlots()) {
			int slotScreenX = drawX + placed.getX();
			if (slotScreenX + TreeStatsLayout.SLOT_SIZE < 0 || slotScreenX > width) {
				continue;
			}

			boolean isHover = mouseX >= slotScreenX && mouseX < slotScreenX + TreeStatsLayout.SLOT_SIZE &&
				mouseY >= drawY && mouseY < drawY + TreeStatsLayout.SLOT_SIZE;

			if (isHover) {
				hoveredStatsSlot = placed;
			}

			// Slot Background
			int bg = isHover ? 0xFF444444 : 0xFF282828;
			g.fill(slotScreenX, drawY, slotScreenX + TreeStatsLayout.SLOT_SIZE, drawY + TreeStatsLayout.SLOT_SIZE, bg);

			// Render Ingredient
			TreeStats.Row row = placed.getRow();
			if (row.getKey() != null && row.getKey().typedIngredient() != null) {
				renderTypedIngredient(g, row.getKey().typedIngredient(), slotScreenX + 1, drawY + 1);
			}

			// Render Row Amount
			if (row.getAmount().compareTo(BigInteger.ONE) > 0) {
				String amtText = BookmarkAmountFormatter.formatItemAmount(row.getAmount());
				g.pose().pushPose();
				g.pose().translate(0, 0, 200);
				g.drawString(font, amtText, slotScreenX + 1, drawY + 9, 0xFFE0E0E0, true);
				g.pose().popPose();
			}
		}
	}

	private void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
		if (hoveredNode != null) {
			List<Component> tooltip = new ArrayList<>();
			Optional<ITypedIngredient<?>> typedOpt = hoveredNode.getTypedIngredient();
			if (typedOpt.isPresent()) {
				ITypedIngredient<?> typed = typedOpt.get();
				// using getIngredientTooltip helper
				for (Component line : getIngredientTooltip(typed)) {
					tooltip.add(line);
				}
			}

			tooltip.add(Component.translatable("jei.tree.tooltip.requested", hoveredNode.getRequestedAmount().toString()).withStyle(ChatFormatting.GRAY));
			tooltip.add(Component.translatable("jei.tree.tooltip.produced", hoveredNode.getProducedAmount().toString())
				.withStyle(hoveredNode.isResolved() ? ChatFormatting.GREEN : ChatFormatting.RED));

			if (hoveredNode.hasRecipe()) {
				tooltip.add(Component.translatable("jei.tree.tooltip.multiplier", hoveredNode.getMultiplier().toString()).withStyle(ChatFormatting.BLUE));
				tooltip.add(Component.translatable("jei.tree.tooltip.recipe", hoveredNode.getRecipeUid().getPath()).withStyle(ChatFormatting.DARK_AQUA));
			} else {
				tooltip.add(Component.translatable("jei.tree.tooltip.no_recipe").withStyle(ChatFormatting.DARK_RED));
			}

			if (hoveredNode.isForceCraft()) {
				tooltip.add(Component.translatable("jei.tree.tooltip.force_craft").withStyle(ChatFormatting.YELLOW));
			}

			// Action hints
			tooltip.add(Component.literal(""));
			tooltip.add(Component.translatable("jei.tree.hint.right_click_recipe").withStyle(ChatFormatting.AQUA));
			tooltip.add(Component.translatable("jei.tree.hint.shift_click_amount").withStyle(ChatFormatting.AQUA));
			tooltip.add(Component.translatable("jei.tree.hint.recipe_keys").withStyle(ChatFormatting.DARK_GRAY));

			g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
		} else if (hoveredStatsSlot != null) {
			TreeStats.Row row = hoveredStatsSlot.getRow();
			List<Component> tooltip = new ArrayList<>();

			tooltip.add(Component.translatable("jei.tree.stats." + row.getKind().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			if (row.getKey() != null) {
				tooltip.add(Component.literal(row.getKey().ingredientUid()).withStyle(ChatFormatting.WHITE));
			}
			tooltip.add(Component.translatable("jei.tree.tooltip.amount", row.getAmount().toString()).withStyle(ChatFormatting.GRAY));

			if (row.getRecipeUid() != null) {
				tooltip.add(Component.translatable("jei.tree.tooltip.recipe", row.getRecipeUid().getPath()).withStyle(ChatFormatting.DARK_AQUA));
			}

			g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (searchBox != null && searchBox.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		// 1. Collapse toggle button
		if (hoveredCollapseBtn != null) {
			hoveredCollapseBtn.toggleCollapsed();
			if (hoveredCollapseBtn.getRecipeUid() != null) {
				treeMath.toggleRecipeCollapsed(hoveredCollapseBtn.getRecipeUid());
			}
			recalculateTree(hoveredCollapseBtn);
			return true;
		}

		// 2. Node Click Interactions
		if (hoveredNode != null) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				// Open Recipe Selector Dialog
				TreeSlot target = hoveredNode;
				Minecraft.getInstance().setScreen(new TreeRecipeSelectorScreen(this, target, selectedRecipe -> {
					if (targetIngredient != null) {
						if (target.equals(graph.getRootSlot())) {
							this.targetRecipeOverride = selectedRecipe;
						} else {
							if (selectedRecipe == null) {
								this.manualRecipes.remove(target.getKey());
							} else {
								this.manualRecipes.put(target.getKey(), selectedRecipe);
							}
						}
						rebuildTreeMath();
					} else {
						treeMath.setManualRecipe(target.getKey(), selectedRecipe);
						recalculateTree(target);
					}
				}));
				return true;
			} else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && Screen.hasShiftDown())) {
				// Open Quantity Adjustment Dialog
				TreeSlot target = hoveredNode;
				Minecraft.getInstance().setScreen(new TreeAdjustQuantityScreen(this, target, result -> {
					if (result.removeNode()) {
						if (targetIngredient != null) {
							prunedKeys.add(target.getKey());
							rebuildTreeMath();
						} else {
							var unused = graph.collectUnusedRecipes(target);
							for (ResourceLocation rec : unused) {
								treeMath.setManualRecipe(target.getKey(), null);
							}
							recalculateTree(null);
						}
					} else {
						if (targetIngredient != null && target.equals(graph.getRootSlot())) {
							this.targetAmount = result.amountOverride().max(BigInteger.ONE);
							rebuildTreeMath();
						} else {
							treeMath.setManualAmount(target.getKey(), result.amountOverride());
							treeMath.setForceCraft(target.getKey(), result.forceCraft());
							recalculateTree(null);
						}
					}
				}));
				return true;
			}
		}

		// 3. Canvas Dragging
		if (camera.isInViewport(mouseX, mouseY)) {
			camera.startDrag(mouseX, mouseY);
			return true;
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (camera.isDragging()) {
			camera.drag(mouseX, mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (camera.isDragging()) {
			camera.endDrag();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (showStats && mouseY >= height - STATS_BAR_HEIGHT) {
			statsScrollOffset = Math.max(0, statsScrollOffset - (int) Math.round(delta * 20));
			return true;
		}

		if (camera.isInViewport(mouseX, mouseY)) {
			camera.zoomAt(mouseX, mouseY, (int) Math.signum(delta));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (searchBox != null && searchBox.isFocused()) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				searchBox.setFocused(false);
				return true;
			}
			return searchBox.keyPressed(keyCode, scanCode, modifiers);
		}

		// Recipe & Uses shortcuts on hovered node
		if (hoveredNode != null) {
			Optional<ITypedIngredient<?>> typedOpt = hoveredNode.getTypedIngredient();
			if (typedOpt.isPresent()) {
				ITypedIngredient<?> typed = typedOpt.get();
				if (keyCode == GLFW.GLFW_KEY_R) {
					openRecipesGui(typed, RecipeIngredientRole.OUTPUT);
					return true;
				} else if (keyCode == GLFW.GLFW_KEY_U) {
					openRecipesGui(typed, RecipeIngredientRole.INPUT);
					return true;
				}
			}
		}

		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void openRecipesGui(ITypedIngredient<?> ingredient, RecipeIngredientRole role) {
		IFocusFactory focusFactory = Internal.getJeiRuntime().getJeiHelpers().getFocusFactory();
		IFocus<?> focus = focusFactory.createFocus(role, ingredient);
		Internal.getJeiRuntime().getRecipesGui().show(List.of(focus));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
