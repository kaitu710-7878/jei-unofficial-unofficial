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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Main Crafting Tree GUI Screen.
 * Fully interactive node hierarchy with pan, zoom, collapse/expand, manual recipe selection,
 * manual quantity overrides, bottom stats bar, bidirectional chain export, and complete BigInteger precision.
 *
 * Ported and heavily adapted/refactored from nei-features-for-jei and GTNH NotEnoughItems (LGPL-3.0-or-later).
 */
public final class CraftingTreeScreen extends Screen {
	private static final int TOP_BAR_HEIGHT = 30;
	private static final int STATS_BAR_HEIGHT = 32;

	private final @Nullable Screen parentScreen;
	private final BookmarkList bookmarks;
	private final int groupId;
	private final IIngredientManager ingredientManager;
	private final PlayerInventoryRecipeChainTooltipInventoryProvider inventoryProvider;

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

		rebuildTreeMath();
	}

	private void rebuildTreeMath() {
		Map<BookmarkIngredientKey, BigInteger> stock = checkInventory ? readInventory() : Map.of();
		this.treeMath = TreeChainBridge.createTreeMathFromGroup(bookmarks, groupId, stock);
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
						treeMath.getCollapsedRecipes(); // refreshed on recalculation
					}
				}
			}
		}
		recalculateTree(null);
	}

	private void exportToBookmarkChain() {
		TreeChainBridge.exportTreeToBookmarkGroup(bookmarks, groupId, graph, treeMath.getCollapsedRecipes());
		if (minecraft != null && minecraft.player != null) {
			minecraft.player.displayClientMessage(Component.translatable("jei.tree.save_chain.success").withStyle(ChatFormatting.GREEN), false);
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
					continue; // Frustum culled!
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
		String uid = slot.getKey().ingredientUid().toLowerCase(Locale.ROOT);
		return uid.contains(searchQuery);
	}

	private void renderTypedIngredient(GuiGraphics g, ITypedIngredient<?> typed, int x, int y) {
		Object ingredient = typed.getIngredient();
		if (ingredient instanceof ItemStack stack) {
			g.renderItem(stack, x, y);
			g.renderItemDecorations(font, stack, x, y, "");
		} else {
			// JEI Generic IIngredientRenderer
			IIngredientRenderer<Object> renderer = ingredientManager.getIngredientRenderer(cast(typed.getType()));
			renderer.render(g, cast(ingredient));
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T cast(Object obj) {
		return (T) obj;
	}

	private void renderSlotAmount(GuiGraphics g, TreeSlot slot, int x, int y, int slotSize) {
		BigInteger amt = slot.getLeftAmount().signum() > 0 ? slot.getLeftAmount() : slot.getRequestedAmount();
		String text = BookmarkAmountFormatter.formatItemAmount(amt);

		int textW = font.width(text);
		int textX = x + slotSize - textW - 1;
		int textY = y + slotSize - font.lineHeight;

		int color = slot.isResolved() ? 0xFF81C784 : (slot.hasRecipe() ? 0xFF90CAF9 : 0xFFE57373);
		g.drawString(font, text, textX, textY, color, true);
	}

	private void renderStatsBar(GuiGraphics g, int mouseX, int mouseY) {
		int barY = height - STATS_BAR_HEIGHT;
		g.fill(0, barY, width, height, 0xEE1E1E1E);
		g.fill(0, barY, width, barY + 1, 0xFF444444);

		hoveredStatsSlot = null;

		g.enableScissor(0, barY, width, height);

		for (TreeStatsLayout.PlacedSlot placed : statsLayout.getPlacedSlots()) {
			int slotX = placed.getX() - statsScrollOffset;
			int slotY = barY + placed.getY();

			if (slotX + TreeStatsLayout.SLOT_SIZE < 0 || slotX > width) {
				continue;
			}

			boolean isHover = mouseX >= slotX && mouseX < slotX + TreeStatsLayout.SLOT_SIZE &&
				mouseY >= slotY && mouseY < slotY + TreeStatsLayout.SLOT_SIZE;

			if (isHover) {
				hoveredStatsSlot = placed;
			}

			// Background
			g.fill(slotX, slotY, slotX + TreeStatsLayout.SLOT_SIZE, slotY + TreeStatsLayout.SLOT_SIZE, 0xAA2A2A2A);
			g.fill(slotX, slotY, slotX + TreeStatsLayout.SLOT_SIZE, slotY + 1, 0xFF555555);
			g.fill(slotX, slotY + TreeStatsLayout.SLOT_SIZE - 1, slotX + TreeStatsLayout.SLOT_SIZE, slotY + TreeStatsLayout.SLOT_SIZE, 0xFF555555);
			g.fill(slotX, slotY, slotX + 1, slotY + TreeStatsLayout.SLOT_SIZE, 0xFF555555);
			g.fill(slotX + TreeStatsLayout.SLOT_SIZE - 1, slotY, slotX + TreeStatsLayout.SLOT_SIZE, slotY + TreeStatsLayout.SLOT_SIZE, 0xFF555555);

			if (isHover) {
				g.fill(slotX + 1, slotY + 1, slotX + TreeStatsLayout.SLOT_SIZE - 1, slotY + TreeStatsLayout.SLOT_SIZE - 1, 0x44FFFFFF);
			}

			// Render ingredient
			TreeStats.Row row = placed.getRow();
			row.getTypedIngredient().ifPresent(typed -> {
				renderTypedIngredient(g, typed, slotX + 1, slotY + 1);
			});

			// Render amount
			String amtStr = BookmarkAmountFormatter.formatItemAmount(row.getAmount());
			int tw = font.width(amtStr);
			g.drawString(font, amtStr, slotX + TreeStatsLayout.SLOT_SIZE - tw - 1, slotY + TreeStatsLayout.SLOT_SIZE - font.lineHeight, 0xFFFFFFFF, true);
		}

		g.disableScissor();
	}

	private void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
		if (hoveredNode != null) {
			List<Component> tooltip = new ArrayList<>();
			BookmarkIngredientKey key = hoveredNode.getKey();

			tooltip.add(Component.literal(key.ingredientUid()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

			tooltip.add(Component.translatable("jei.tree.tooltip.requested", hoveredNode.getRequestedAmount().toString()).withStyle(ChatFormatting.GRAY));
			tooltip.add(Component.translatable("jei.tree.tooltip.remaining", hoveredNode.getLeftAmount().toString())
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
					treeMath.setManualRecipe(target.getKey(), selectedRecipe);
					recalculateTree(target);
				}));
				return true;
			} else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && Screen.hasShiftDown())) {
				// Open Quantity Adjustment Dialog
				TreeSlot target = hoveredNode;
				Minecraft.getInstance().setScreen(new TreeAdjustQuantityScreen(this, target, result -> {
					if (result.removeNode()) {
						var unused = graph.collectUnusedRecipes(target);
						for (ResourceLocation rec : unused) {
							treeMath.setManualRecipe(target.getKey(), null);
						}
					} else {
						treeMath.setManualAmount(target.getKey(), result.amountOverride());
						treeMath.setForceCraft(target.getKey(), result.forceCraft());
					}
					recalculateTree(null);
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
			// Scroll bottom stats panel
			statsScrollOffset = Math.max(0, Math.min(statsLayout.getTotalWidth() - width, (int) (statsScrollOffset - delta * 24)));
			return true;
		}

		if (camera.isInViewport(mouseX, mouseY)) {
			boolean shift = Screen.hasShiftDown();
			boolean ctrl = Screen.hasControlDown();
			camera.scroll(mouseX, mouseY, delta, shift, ctrl);
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

		if (hoveredNode != null) {
			// Recipe (R) or Usage (U) lookup in JEI
			if (keyCode == GLFW.GLFW_KEY_R) {
				hoveredNode.getTypedIngredient().ifPresent(typed -> {
					var recipesGui = Internal.getJeiRuntime().getRecipesGui();
					IFocusFactory focusFactory = Internal.getJeiRuntime().getJeiHelpers().getFocusFactory();
					IFocus<?> focus = focusFactory.createFocus(RecipeIngredientRole.OUTPUT, typed);
					recipesGui.show(List.of(focus));
				});
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_U) {
				hoveredNode.getTypedIngredient().ifPresent(typed -> {
					var recipesGui = Internal.getJeiRuntime().getRecipesGui();
					IFocusFactory focusFactory = Internal.getJeiRuntime().getJeiHelpers().getFocusFactory();
					IFocus<?> focus = focusFactory.createFocus(RecipeIngredientRole.INPUT, typed);
					recipesGui.show(List.of(focus));
				});
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				// Delete node
				TreeSlot target = hoveredNode;
				treeMath.setManualRecipe(target.getKey(), null);
				recalculateTree(null);
				return true;
			}
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (searchBox != null && searchBox.isFocused()) {
			return searchBox.charTyped(codePoint, modifiers);
		}
		return super.charTyped(codePoint, modifiers);
	}
}
