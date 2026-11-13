package mezz.jei.gui.tree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.Internal;
import mezz.jei.common.config.file.serializers.TypedIngredientSerializer;
import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import mezz.jei.gui.bookmarks.BookmarkItemMetadataFactory;
import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.bookmarks.chain.RecipeChainInput;
import mezz.jei.gui.config.file.serializers.BookmarkIngredientKeySerializer;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
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
	private final Set<BookmarkIngredientKey> uncraftedKeys = new HashSet<>();
	private final Set<String> prunedPaths = new HashSet<>();
	private final Map<BookmarkIngredientKey, List<ITypedIngredient<?>>> customRecipeInputs = new LinkedHashMap<>();
	private final Map<BookmarkIngredientKey, String> searchTextCache = new HashMap<>();
	private final Map<ResourceLocation, mezz.jei.api.recipe.category.IRecipeCategory<?>> categoryCache = new HashMap<>();

	private final TreeCamera camera = new TreeCamera();
	private boolean cameraInitialized = false;
	private final TreeLayout layout = new TreeLayout();
	private final TreeStatsLayout statsLayout = new TreeStatsLayout();

	private TreeMath treeMath;
	private TreeGraph graph;
	private TreeStats stats;

	private boolean checkInventory = true;
	private boolean showStats = true;
	private boolean statsLabelMode = true;
	private String searchQuery = "";
	private @Nullable EditBox searchBox;

	private @Nullable TreeSlot hoveredNode;
	private @Nullable TreeSlot hoveredCollapseBtn;

	public static final class TreeSession {
		public int groupId = -1;
		public @Nullable ITypedIngredient<?> targetIngredient;
		public @Nullable BookmarkIngredientKey targetIngredientKey;
		public BigInteger targetAmount = BigInteger.ONE;
		public @Nullable ResourceLocation targetRecipeOverride;
		public final Map<BookmarkIngredientKey, ResourceLocation> manualRecipes = new LinkedHashMap<>();
		public final Set<BookmarkIngredientKey> prunedKeys = new HashSet<>();
		public final Set<BookmarkIngredientKey> uncraftedKeys = new HashSet<>();
		public final Set<String> prunedPaths = new HashSet<>();
		public final Map<BookmarkIngredientKey, List<ITypedIngredient<?>>> customRecipeInputs = new LinkedHashMap<>();
		public boolean checkInventory = true;
		public boolean showStats = true;
		public boolean statsLabelMode = true;
		public float cameraPanX = 0;
		public float cameraPanY = 0;
		public float cameraZoom = 1.0f;

		public static Path getSessionFilePath() {
			return Minecraft.getInstance().gameDirectory.toPath()
				.resolve("config")
				.resolve("jei")
				.resolve("crafting_tree_session.json");
		}

		public void saveToFile(IIngredientManager ingredientManager) {
			try {
				Path path = getSessionFilePath();
				Files.createDirectories(path.getParent());
				JsonObject root = new JsonObject();
				root.addProperty("groupId", groupId);
				root.addProperty("targetAmount", targetAmount.toString());
				if (targetRecipeOverride != null) {
					root.addProperty("targetRecipeOverride", targetRecipeOverride.toString());
				}
				if (targetIngredientKey != null) {
					root.add("targetIngredientKey", BookmarkIngredientKeySerializer.serialize(targetIngredientKey));
				} else if (targetIngredient != null) {
					BookmarkIngredientKey key = BookmarkItemMetadataFactory.createPermutationKey(targetIngredient, ingredientManager);
					root.add("targetIngredientKey", BookmarkIngredientKeySerializer.serialize(key));
				}
				if (targetIngredient != null) {
					TypedIngredientSerializer serializer = new TypedIngredientSerializer(ingredientManager);
					root.addProperty("targetIngredient", serializer.serialize(targetIngredient));
				}

				JsonArray manualArr = new JsonArray();
				for (Map.Entry<BookmarkIngredientKey, ResourceLocation> entry : manualRecipes.entrySet()) {
					JsonObject item = new JsonObject();
					item.add("key", BookmarkIngredientKeySerializer.serialize(entry.getKey()));
					item.addProperty("recipe", entry.getValue().toString());
					manualArr.add(item);
				}
				root.add("manualRecipes", manualArr);

				JsonArray prunedArr = new JsonArray();
				for (BookmarkIngredientKey pruned : prunedKeys) {
					prunedArr.add(BookmarkIngredientKeySerializer.serialize(pruned));
				}
				root.add("prunedKeys", prunedArr);

				JsonArray uncraftedArr = new JsonArray();
				for (BookmarkIngredientKey uncrafted : uncraftedKeys) {
					uncraftedArr.add(BookmarkIngredientKeySerializer.serialize(uncrafted));
				}
				root.add("uncraftedKeys", uncraftedArr);

				JsonArray prunedPathsArr = new JsonArray();
				for (String pathStr : prunedPaths) {
					prunedPathsArr.add(pathStr);
				}
				root.add("prunedPaths", prunedPathsArr);

				JsonArray customInputsArr = new JsonArray();
				for (Map.Entry<BookmarkIngredientKey, List<ITypedIngredient<?>>> entry : customRecipeInputs.entrySet()) {
					JsonObject obj = new JsonObject();
					obj.add("key", BookmarkIngredientKeySerializer.serialize(entry.getKey()));
					JsonArray inputsArr = new JsonArray();
					for (ITypedIngredient<?> input : entry.getValue()) {
						BookmarkIngredientKey inKey = BookmarkItemMetadataFactory.createPermutationKey(input, ingredientManager);
						inputsArr.add(BookmarkIngredientKeySerializer.serialize(inKey));
					}
					obj.add("inputs", inputsArr);
					customInputsArr.add(obj);
				}
				root.add("customRecipeInputs", customInputsArr);

				root.addProperty("checkInventory", checkInventory);
				root.addProperty("showStats", showStats);
				root.addProperty("statsLabelMode", statsLabelMode);
				root.addProperty("cameraPanX", cameraPanX);
				root.addProperty("cameraPanY", cameraPanY);
				root.addProperty("cameraZoom", cameraZoom);

				Files.writeString(path, root.toString(), StandardCharsets.UTF_8);
			} catch (Throwable ignored) {}
		}

		public static @Nullable TreeSession loadFromFile(IIngredientManager ingredientManager) {
			try {
				Path path = getSessionFilePath();
				if (!Files.exists(path)) {
					return null;
				}
				String content = Files.readString(path, StandardCharsets.UTF_8);
				if (content.isBlank()) {
					return null;
				}
				JsonObject root = JsonParser.parseString(content).getAsJsonObject();
				TreeSession session = new TreeSession();
				session.groupId = root.has("groupId") ? root.get("groupId").getAsInt() : -1;
				if (root.has("targetAmount")) {
					try {
						session.targetAmount = new BigInteger(root.get("targetAmount").getAsString());
					} catch (Exception ignored) {}
				}
				if (root.has("targetRecipeOverride")) {
					session.targetRecipeOverride = new ResourceLocation(root.get("targetRecipeOverride").getAsString());
				}
				if (root.has("targetIngredientKey")) {
					BookmarkIngredientKey key = BookmarkIngredientKeySerializer.deserialize(root.get("targetIngredientKey"));
					session.targetIngredientKey = key;
					session.targetIngredient = key.resolveIngredient(ingredientManager).orElse(null);
				}
				if (session.targetIngredient == null && root.has("targetIngredient")) {
					TypedIngredientSerializer serializer = new TypedIngredientSerializer(ingredientManager);
					var res = serializer.deserialize(root.get("targetIngredient").getAsString());
					session.targetIngredient = res.getResult().orElse(null);
					if (session.targetIngredient != null && session.targetIngredientKey == null) {
						session.targetIngredientKey = BookmarkItemMetadataFactory.createPermutationKey(session.targetIngredient, ingredientManager);
					}
				}
				if (root.has("manualRecipes") && root.get("manualRecipes").isJsonArray()) {
					for (JsonElement elem : root.getAsJsonArray("manualRecipes")) {
						if (elem.isJsonObject()) {
							JsonObject obj = elem.getAsJsonObject();
							BookmarkIngredientKey key = BookmarkIngredientKeySerializer.deserialize(obj.get("key"));
							ResourceLocation rec = new ResourceLocation(obj.get("recipe").getAsString());
							session.manualRecipes.put(key, rec);
						}
					}
				}
				if (root.has("prunedKeys") && root.get("prunedKeys").isJsonArray()) {
					for (JsonElement elem : root.getAsJsonArray("prunedKeys")) {
						BookmarkIngredientKey key = BookmarkIngredientKeySerializer.deserialize(elem);
						session.prunedKeys.add(key);
					}
				}
				if (root.has("uncraftedKeys") && root.get("uncraftedKeys").isJsonArray()) {
					for (JsonElement elem : root.getAsJsonArray("uncraftedKeys")) {
						BookmarkIngredientKey key = BookmarkIngredientKeySerializer.deserialize(elem);
						session.uncraftedKeys.add(key);
					}
				}
				if (root.has("prunedPaths") && root.get("prunedPaths").isJsonArray()) {
					for (JsonElement elem : root.getAsJsonArray("prunedPaths")) {
						session.prunedPaths.add(elem.getAsString());
					}
				}
				if (root.has("customRecipeInputs") && root.get("customRecipeInputs").isJsonArray()) {
					for (JsonElement elem : root.getAsJsonArray("customRecipeInputs")) {
						if (elem.isJsonObject()) {
							JsonObject obj = elem.getAsJsonObject();
							BookmarkIngredientKey key = BookmarkIngredientKeySerializer.deserialize(obj.get("key"));
							if (obj.has("inputs") && obj.get("inputs").isJsonArray()) {
								List<ITypedIngredient<?>> resolvedList = new ArrayList<>();
								JsonArray inArr = obj.getAsJsonArray("inputs");
								for (JsonElement inElem : inArr) {
									BookmarkIngredientKey inKey = BookmarkIngredientKeySerializer.deserialize(inElem);
									inKey.resolveIngredient(ingredientManager).ifPresent(resolvedList::add);
								}
								// Only store custom inputs if all elements were safely resolved to prevent ingredient loss
								if (!resolvedList.isEmpty() && resolvedList.size() == inArr.size()) {
									session.customRecipeInputs.put(key, resolvedList);
								}
							}
						}
					}
				}
				if (root.has("checkInventory")) {
					session.checkInventory = root.get("checkInventory").getAsBoolean();
				}
				if (root.has("showStats")) {
					session.showStats = root.get("showStats").getAsBoolean();
				}
				if (root.has("statsLabelMode")) {
					session.statsLabelMode = root.get("statsLabelMode").getAsBoolean();
				}
				if (root.has("cameraPanX")) {
					session.cameraPanX = root.get("cameraPanX").getAsFloat();
				}
				if (root.has("cameraPanY")) {
					session.cameraPanY = root.get("cameraPanY").getAsFloat();
				}
				if (root.has("cameraZoom")) {
					session.cameraZoom = root.get("cameraZoom").getAsFloat();
				}
				if (session.targetIngredient != null || session.targetIngredientKey != null || session.groupId >= 0) {
					return session;
				}
			} catch (Throwable ignored) {}
			return null;
		}
	}

	private static @Nullable TreeSession activeSession = null;
	private @Nullable TreeSlot pendingRecipeTarget;

	private @Nullable TreeStatsLayout.PlacedSlot hoveredStatsSlot;
	private int statsScrollOffset = 0;

	private static void ensureActiveSessionLoaded() {
		if (activeSession == null) {
			try {
				IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();
				activeSession = TreeSession.loadFromFile(ingredientManager);
			} catch (Throwable ignored) {}
		}
	}

	public static void open(Screen parent, BookmarkList bookmarks, int groupId) {
		IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();
		Minecraft.getInstance().setScreen(new CraftingTreeScreen(parent, bookmarks, groupId, ingredientManager));
	}

	public static void open(Screen parent, BookmarkList bookmarks, int groupId, IIngredientManager ingredientManager) {
		Minecraft.getInstance().setScreen(new CraftingTreeScreen(parent, bookmarks, groupId, ingredientManager));
	}

	public static void openForIngredient(Screen parent, BookmarkList bookmarks, ITypedIngredient<?> target, IIngredientManager ingredientManager) {
		CraftingTreeScreen screen = new CraftingTreeScreen(parent, bookmarks, target, BigInteger.ONE, null, ingredientManager);
		Minecraft.getInstance().setScreen(screen);
	}

	public static void clearSession() {
		activeSession = null;
		try {
			Path path = TreeSession.getSessionFilePath();
			if (Files.exists(path)) {
				Files.delete(path);
			}
		} catch (Throwable ignored) {}
	}

	public static boolean hasActiveSession() {
		ensureActiveSessionLoaded();
		return activeSession != null && (activeSession.targetIngredient != null || activeSession.groupId >= 0);
	}

	public static void openSessionOrNew(Screen parent, BookmarkList bookmarks, IIngredientManager ingredientManager) {
		ensureActiveSessionLoaded();
		if (hasActiveSession()) {
			if (activeSession.targetIngredient != null) {
				Minecraft.getInstance().setScreen(new CraftingTreeScreen(
					parent,
					bookmarks,
					activeSession.targetIngredient,
					activeSession.targetAmount,
					activeSession.targetRecipeOverride,
					ingredientManager
				));
				return;
			} else if (activeSession.groupId >= 0) {
				Minecraft.getInstance().setScreen(new CraftingTreeScreen(
					parent,
					bookmarks,
					activeSession.groupId,
					ingredientManager
				));
				return;
			}
		}

		if (!bookmarks.getBookmarks().isEmpty()) {
			var firstBm = bookmarks.getBookmarks().get(0);
			int gid = bookmarks.getBookmarkGroupId(firstBm);
			if (gid >= 0) {
				Minecraft.getInstance().setScreen(new CraftingTreeScreen(parent, bookmarks, gid, ingredientManager));
				return;
			}
			var typed = firstBm.getElement().getTypedIngredient();
			Minecraft.getInstance().setScreen(new CraftingTreeScreen(parent, bookmarks, typed, BigInteger.ONE, null, ingredientManager));
		}
	}

	public CraftingTreeScreen(
		@Nullable Screen parentScreen,
		BookmarkList bookmarks,
		int groupId,
		IIngredientManager ingredientManager
	) {
		super(Component.translatable("jei.tree.title"));
		ensureActiveSessionLoaded();
		this.parentScreen = parentScreen;
		this.bookmarks = bookmarks;
		this.groupId = groupId;
		this.ingredientManager = ingredientManager;
		this.inventoryProvider = new PlayerInventoryRecipeChainTooltipInventoryProvider(Minecraft.getInstance(), ingredientManager);
		this.targetIngredient = null;
		this.targetAmount = BigInteger.ONE;
		this.targetRecipeOverride = null;

		if (activeSession != null && activeSession.groupId == groupId && groupId >= 0) {
			this.manualRecipes.putAll(activeSession.manualRecipes);
			this.customRecipeInputs.putAll(activeSession.customRecipeInputs);
			this.prunedKeys.addAll(activeSession.prunedKeys);
			this.uncraftedKeys.addAll(activeSession.uncraftedKeys);
			this.prunedPaths.addAll(activeSession.prunedPaths);
			this.checkInventory = activeSession.checkInventory;
			this.showStats = activeSession.showStats;
			this.statsLabelMode = activeSession.statsLabelMode;
			this.camera.setScroll(activeSession.cameraPanX, activeSession.cameraPanY);
			this.camera.setZoomLevel(activeSession.cameraZoom);
			this.cameraInitialized = true;
		}

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
		ensureActiveSessionLoaded();
		this.parentScreen = parentScreen;
		this.bookmarks = bookmarks;
		this.groupId = -1;
		this.ingredientManager = ingredientManager;
		this.inventoryProvider = new PlayerInventoryRecipeChainTooltipInventoryProvider(Minecraft.getInstance(), ingredientManager);
		this.targetIngredient = targetIngredient;
		this.targetAmount = targetAmount.max(BigInteger.ONE);
		this.targetRecipeOverride = targetRecipeOverride;

		// Restore session if working on the same target ingredient
		if (activeSession != null) {
			BookmarkIngredientKey activeKey = activeSession.targetIngredientKey != null
				? activeSession.targetIngredientKey
				: (activeSession.targetIngredient != null ? BookmarkItemMetadataFactory.createPermutationKey(activeSession.targetIngredient, ingredientManager) : null);
			BookmarkIngredientKey currentKey = BookmarkItemMetadataFactory.createPermutationKey(targetIngredient, ingredientManager);
			if (activeKey != null && (activeKey.equals(currentKey) || activeKey.matchesCraftingAvailable(currentKey))) {
				this.manualRecipes.putAll(activeSession.manualRecipes);
				this.customRecipeInputs.putAll(activeSession.customRecipeInputs);
				this.prunedKeys.addAll(activeSession.prunedKeys);
				this.uncraftedKeys.addAll(activeSession.uncraftedKeys);
				this.prunedPaths.addAll(activeSession.prunedPaths);
				if (activeSession.targetRecipeOverride != null) {
					this.targetRecipeOverride = activeSession.targetRecipeOverride;
				}
				this.targetAmount = activeSession.targetAmount.max(BigInteger.ONE);
				this.checkInventory = activeSession.checkInventory;
				this.showStats = activeSession.showStats;
				this.statsLabelMode = activeSession.statsLabelMode;
				this.camera.setScroll(activeSession.cameraPanX, activeSession.cameraPanY);
				this.camera.setZoomLevel(activeSession.cameraZoom);
				this.cameraInitialized = true;
			}
		}

		rebuildTreeMath();
	}

	private void saveSession() {
		if (targetIngredient == null && groupId < 0) {
			return;
		}
		if (activeSession == null) {
			activeSession = new TreeSession();
		}
		activeSession.groupId = this.groupId;
		activeSession.targetIngredient = this.targetIngredient;
		activeSession.targetIngredientKey = this.targetIngredient != null ? BookmarkItemMetadataFactory.createPermutationKey(this.targetIngredient, this.ingredientManager) : null;
		activeSession.targetAmount = this.targetAmount;
		activeSession.targetRecipeOverride = this.targetRecipeOverride;
		activeSession.manualRecipes.clear();
		activeSession.manualRecipes.putAll(this.manualRecipes);
		activeSession.customRecipeInputs.clear();
		activeSession.customRecipeInputs.putAll(this.customRecipeInputs);
		activeSession.prunedKeys.clear();
		activeSession.prunedKeys.addAll(this.prunedKeys);
		activeSession.uncraftedKeys.clear();
		activeSession.uncraftedKeys.addAll(this.uncraftedKeys);
		activeSession.prunedPaths.clear();
		activeSession.prunedPaths.addAll(this.prunedPaths);
		activeSession.checkInventory = this.checkInventory;
		activeSession.showStats = this.showStats;
		activeSession.statsLabelMode = this.statsLabelMode;
		activeSession.cameraPanX = this.camera.getScrollX();
		activeSession.cameraPanY = this.camera.getScrollY();
		activeSession.cameraZoom = this.camera.getZoomLevel();

		activeSession.saveToFile(this.ingredientManager);
	}

	private void rebuildTreeMath() {
		rebuildTreeMath(null);
	}

	private void rebuildTreeMath(@Nullable TreeSlot anchor) {
		Map<BookmarkIngredientKey, BigInteger> stock = checkInventory ? readInventory() : Map.of();
		TreeSlot prevRoot = graph != null ? graph.getRootSlot() : null;
		if (targetIngredient != null) {
			List<RecipeChainInput> inputs = TreeRecipeBuilder.buildInputsForTarget(
				targetIngredient,
				targetAmount,
				targetRecipeOverride,
				manualRecipes,
				customRecipeInputs,
				uncraftedKeys,
				prunedKeys,
				ingredientManager
			);
			this.treeMath = new TreeMath(inputs, stock, prunedKeys);
			BookmarkIngredientKey rootKey = BookmarkItemMetadataFactory.createPermutationKey(targetIngredient, ingredientManager);
			this.treeMath.setManualAmount(rootKey, targetAmount);
		} else {
			this.treeMath = TreeChainBridge.createTreeMathFromGroup(bookmarks, groupId, stock, prunedKeys);
		}
		// Synchronize all manually assigned recipes, custom inputs, uncrafted states, and pruned paths into treeMath
		this.uncraftedKeys.forEach(k -> this.treeMath.setUncrafted(k, true));
		this.treeMath.setPrunedPaths(this.prunedPaths);
		this.manualRecipes.forEach(this.treeMath::setManualRecipe);
		this.customRecipeInputs.forEach(this.treeMath::setCustomRecipeInputs);
		this.graph = treeMath.calculate();
		TreeSlot anchorToTrack = anchor != null ? anchor : (prevRoot != null ? prevRoot : graph.getRootSlot());
		int[] displacement = this.layout.recalculate(graph, anchorToTrack);
		if (displacement != null) {
			camera.pan(displacement[0], displacement[1]);
		}
		this.stats = TreeStats.compute(graph);
		this.statsLayout.recalculate(stats, font != null ? font : Minecraft.getInstance().font, statsLabelMode);
		saveSession();
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

		// Home Button (Center on Root Target)
		addRenderableWidget(Button.builder(Component.literal("H"), b -> centerOnRoot())
			.bounds(50, 5, 20, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.home")))
			.build());

		// Zoom Out Button
		addRenderableWidget(Button.builder(Component.literal("-"), b -> camera.zoomAt(width / 2.0, height / 2.0, -1))
			.bounds(72, 5, 20, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.zoom_out")))
			.build());

		// Zoom In Button
		addRenderableWidget(Button.builder(Component.literal("+"), b -> camera.zoomAt(width / 2.0, height / 2.0, 1))
			.bounds(94, 5, 20, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.zoom_in")))
			.build());

		// Inventory Toggle Button
		addRenderableWidget(Button.builder(
				Component.literal(checkInventory ? "Inv:ON" : "Inv:OFF"),
				b -> {
					checkInventory = !checkInventory;
					b.setMessage(Component.literal(checkInventory ? "Inv:ON" : "Inv:OFF"));
					rebuildTreeMath(null);
				}
			)
			.bounds(116, 5, 48, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.inventory_toggle")))
			.build());

		// Collapse/Expand All Button
		addRenderableWidget(Button.builder(Component.literal("+/-"), b -> toggleAllCollapse())
			.bounds(166, 5, 24, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.toggle_all")))
			.build());

		// Stats Bar Toggle Button
		addRenderableWidget(Button.builder(
				Component.literal("="),
				b -> {
					showStats = !showStats;
					int newH = height - TOP_BAR_HEIGHT - (showStats ? STATS_BAR_HEIGHT : 0);
					camera.setViewport(0, TOP_BAR_HEIGHT, width, newH);
					saveSession();
				}
			)
			.bounds(192, 5, 20, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.stats_toggle")))
			.build());

		// Stats Labels Mode Toggle Button
		addRenderableWidget(Button.builder(
				Component.literal(statsLabelMode ? "Lbl:ON" : "Lbl:OFF"),
				b -> {
					statsLabelMode = !statsLabelMode;
					b.setMessage(Component.literal(statsLabelMode ? "Lbl:ON" : "Lbl:OFF"));
					statsLayout.recalculate(stats, font, statsLabelMode);
					saveSession();
				}
			)
			.bounds(214, 5, 48, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.stats_mode_toggle")))
			.build());

		// Save/Export to Chain Button
		addRenderableWidget(Button.builder(
				Component.translatable("jei.tree.save_chain"),
				b -> exportToBookmarkChain()
			)
			.bounds(264, 5, 44, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.save_chain.tooltip")))
			.build());

		// Reset/New Working Tree Button
		addRenderableWidget(Button.builder(
				Component.translatable("jei.tree.reset"),
				b -> resetTree()
			)
			.bounds(310, 5, 38, 20)
			.tooltip(Tooltip.create(Component.translatable("jei.tree.reset.tooltip")))
			.build());

		// Search Box
		searchBox = new EditBox(font, width - 130, 6, 124, 18, Component.literal("Search"));
		searchBox.setMaxLength(32);
		searchBox.setValue(searchQuery);
		searchBox.setResponder(text -> this.searchQuery = text.trim().toLowerCase(Locale.ROOT));
		addRenderableWidget(searchBox);

		if (!cameraInitialized) {
			fitToView();
			cameraInitialized = true;
			saveSession();
		}
	}

	private void recalculateTree(@Nullable TreeSlot anchor) {
		TreeSlot anchorToTrack = anchor != null ? anchor : (graph != null ? graph.getRootSlot() : null);
		this.graph = treeMath.calculate();
		if (anchorToTrack == null) {
			anchorToTrack = graph.getRootSlot();
		}
		int[] displacement = this.layout.recalculate(graph, anchorToTrack);
		if (displacement != null) {
			camera.pan(displacement[0], displacement[1]);
		}
		this.stats = TreeStats.compute(graph);
		this.statsLayout.recalculate(stats, font != null ? font : Minecraft.getInstance().font, statsLabelMode);
		saveSession();
	}

	public void centerOnRoot() {
		TreeSlot root = graph.getRootSlot();
		if (root != null) {
			TreeLayout.Point p = layout.getPosition(root);
			if (p != null) {
				camera.centerOn(p.x + TreeLayout.SLOT_SIZE / 2.0f, p.y + TreeLayout.SLOT_SIZE / 2.0f);
				saveSession();
			}
		}
	}

	private void resetTree() {
		clearSession();
		TreeRecipeBuilder.clearCache();
		manualRecipes.clear();
		customRecipeInputs.clear();
		prunedKeys.clear();
		uncraftedKeys.clear();
		prunedPaths.clear();
		targetRecipeOverride = null;
		rebuildTreeMath();
		fitToView();
	}

	public void openRecipeGuiForNode(TreeSlot target) {
		this.pendingRecipeTarget = target;
		Optional<ITypedIngredient<?>> typedOpt = target.getTypedIngredient();
		if (typedOpt.isPresent()) {
			saveSession();
			IFocusFactory focusFactory = Internal.getJeiRuntime().getJeiHelpers().getFocusFactory();
			IFocus<?> focus = focusFactory.createFocus(RecipeIngredientRole.OUTPUT, typedOpt.get());
			Internal.getJeiRuntime().getRecipesGui().show(List.of(focus));
		}
	}

	public void applySelectedRecipeLayout(IRecipeLayoutDrawable<?> layout) {
		// Determine output ingredient of the selected layout
		TreeRecipeBuilder.RecipeInfo<?> preliminaryInfo = TreeRecipeBuilder.createFromLayout(
			layout,
			pendingRecipeTarget != null ? pendingRecipeTarget.getTypedIngredient().orElse(null) : null
		);
		if (preliminaryInfo == null) {
			return;
		}

		ITypedIngredient<?> newOutputIng = !preliminaryInfo.outputs().isEmpty() ? preliminaryInfo.outputs().get(0) : null;
		BookmarkIngredientKey newOutputKey = newOutputIng != null
			? BookmarkItemMetadataFactory.createPermutationKey(newOutputIng, ingredientManager)
			: null;

		// Resolve which node in the tree this recipe selection applies to
		TreeSlot effectiveTarget = resolveEffectiveRecipeTarget(preliminaryInfo.outputs(), newOutputKey);
		if (effectiveTarget == null) {
			return;
		}

		BookmarkIngredientKey targetKey = effectiveTarget.getKey();
		ITypedIngredient<?> matchTarget = effectiveTarget.getTypedIngredient().orElse(newOutputIng);
		TreeRecipeBuilder.RecipeInfo<?> info = TreeRecipeBuilder.createFromLayout(layout, matchTarget);
		if (info == null) {
			info = preliminaryInfo;
		}
		ResourceLocation recipeUid = info.recipeUid();

		// Case A: User opened recipe GUI from a parent node, then navigated to a deleted/pruned child ingredient's recipe and pressed '+'
		if (newOutputKey != null && !matchesOutputKey(info.outputs(), targetKey) && isIngredientOfNode(effectiveTarget, newOutputKey)) {
			String parentPath = effectiveTarget.getNodePath();
			if (parentPath != null && !parentPath.isEmpty()) {
				String childPrefix = parentPath + "/";
				prunedPaths.removeIf(p -> p.startsWith(childPrefix) && (p.endsWith("/" + newOutputKey.stableKey()) || p.endsWith("/" + newOutputKey.ingredientUid())));
			}
			this.prunedKeys.remove(newOutputKey);
			this.uncraftedKeys.remove(newOutputKey);
			this.manualRecipes.put(newOutputKey, recipeUid);
			applyCyclingCustomInputs(layout, info, newOutputKey);
			unpruneSubtreeInputs(info);
			rebuildTreeMath(effectiveTarget);
			saveSession();
			this.pendingRecipeTarget = null;
			return;
		}

		// Clear pruned paths for effectiveTarget AND all of its child/descendant paths so deleted children are restored!
		clearPrunedPathsForSubtree(effectiveTarget);

		// Case B: Root node or exact match on effectiveTarget
		if (effectiveTarget.equals(graph.getRootSlot()) && (newOutputKey == null || matchesOutputKey(info.outputs(), targetKey))) {
			this.targetRecipeOverride = recipeUid;
		} else if (newOutputKey != null && !matchesOutputKey(info.outputs(), targetKey)) {
			// Case C: Tag ingredient substitution on a child node (e.g. replacing clear glass with red stained glass)
			TreeSlot parent = graph.getParent(effectiveTarget);
			if (parent != null) {
				BookmarkIngredientKey parentKey = parent.getKey();
				List<ITypedIngredient<?>> currentParentInputs = customRecipeInputs.get(parentKey);
				if (currentParentInputs == null) {
					ResourceLocation parentRecipe = parent.getRecipeUid();
					if (parentRecipe != null) {
						TreeRecipeBuilder.RecipeInfo<?> pInfo = TreeRecipeBuilder.findRecipe(parentRecipe, parent.getTypedIngredient().orElse(null));
						if (pInfo != null) {
							currentParentInputs = new ArrayList<>(pInfo.inputs());
						}
					}
				} else {
					currentParentInputs = new ArrayList<>(currentParentInputs);
				}
				if (currentParentInputs != null) {
					for (int i = 0; i < currentParentInputs.size(); i++) {
						BookmarkIngredientKey inK = BookmarkItemMetadataFactory.createPermutationKey(currentParentInputs.get(i), ingredientManager);
						if (inK.equals(targetKey) || inK.matchesCraftingAvailable(targetKey)) {
							currentParentInputs.set(i, newOutputIng);
							break;
						}
					}
					this.customRecipeInputs.put(parentKey, currentParentInputs);
				}
			}
			this.manualRecipes.remove(targetKey);
			targetKey = newOutputKey;
		}

		this.manualRecipes.put(targetKey, recipeUid);
		applyCyclingCustomInputs(layout, info, targetKey);

		// Ensure target and all inputs of the newly selected recipe (plus base recipe inputs) are unpruned
		this.uncraftedKeys.remove(targetKey);
		this.prunedKeys.remove(targetKey);
		unpruneSubtreeInputs(info);
		TreeRecipeBuilder.RecipeInfo<?> baseInfo = TreeRecipeBuilder.findRecipe(recipeUid, matchTarget);
		if (baseInfo != null) {
			unpruneSubtreeInputs(baseInfo);
		}

		rebuildTreeMath(effectiveTarget);
		saveSession();
		this.pendingRecipeTarget = null;
	}

	private void applyCyclingCustomInputs(IRecipeLayoutDrawable<?> layout, TreeRecipeBuilder.RecipeInfo<?> info, BookmarkIngredientKey targetKey) {
		boolean hasCyclingChoice = false;
		for (IRecipeSlotView slot : layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.INPUT)) {
			if (slot.getAllIngredients().count() > 1) {
				hasCyclingChoice = true;
				break;
			}
		}
		if (hasCyclingChoice && !info.inputs().isEmpty()) {
			this.customRecipeInputs.put(targetKey, new ArrayList<>(info.inputs()));
		} else {
			this.customRecipeInputs.remove(targetKey);
		}
	}

	private @Nullable TreeSlot resolveEffectiveRecipeTarget(List<ITypedIngredient<?>> recipeOutputs, @Nullable BookmarkIngredientKey primaryOutputKey) {
		if (pendingRecipeTarget != null && matchesOutputKey(recipeOutputs, pendingRecipeTarget.getKey())) {
			return pendingRecipeTarget;
		}
		// Search existing tree nodes for a matching output key (handles 'U' key navigation or jumping between recipes in JEI)
		if (graph != null) {
			for (TreeSlot slot : graph.getItems()) {
				if (matchesOutputKey(recipeOutputs, slot.getKey())) {
					return slot;
				}
			}
		}
		if (pendingRecipeTarget != null) {
			return pendingRecipeTarget;
		}
		return graph != null ? graph.getRootSlot() : null;
	}

	private boolean matchesOutputKey(List<ITypedIngredient<?>> outputs, @Nullable BookmarkIngredientKey targetKey) {
		if (targetKey == null) {
			return false;
		}
		for (ITypedIngredient<?> out : outputs) {
			BookmarkIngredientKey outKey = BookmarkItemMetadataFactory.createPermutationKey(out, ingredientManager);
			if (targetKey.equals(outKey) || targetKey.matchesCraftingAvailable(outKey)) {
				return true;
			}
		}
		return false;
	}

	private boolean isIngredientOfNode(TreeSlot node, BookmarkIngredientKey candidateInputKey) {
		ResourceLocation recUid = node.getRecipeUid();
		if (recUid == null) {
			return false;
		}
		TreeRecipeBuilder.RecipeInfo<?> info = TreeRecipeBuilder.findRecipe(recUid, node.getTypedIngredient().orElse(null));
		if (info == null) {
			return false;
		}
		for (ITypedIngredient<?> in : info.inputs()) {
			BookmarkIngredientKey inKey = BookmarkItemMetadataFactory.createPermutationKey(in, ingredientManager);
			if (inKey.equals(candidateInputKey) || inKey.matchesCraftingAvailable(candidateInputKey)) {
				return true;
			}
		}
		return false;
	}

	private void clearPrunedPathsForSubtree(TreeSlot target) {
		String targetPath = target.getNodePath();
		if (targetPath != null && !targetPath.isEmpty()) {
			String prefix = targetPath + "/";
			prunedPaths.removeIf(p -> p.equals(targetPath) || p.startsWith(prefix));
		}
		if (graph != null && target.equals(graph.getRootSlot())) {
			prunedPaths.removeIf(p -> p.equals("0") || p.startsWith("0/"));
		}
	}

	public void applySelectedRecipe(ResourceLocation recipeUid) {
		TreeSlot effectiveTarget = pendingRecipeTarget != null ? pendingRecipeTarget : (graph != null ? graph.getRootSlot() : null);
		if (effectiveTarget != null) {
			BookmarkIngredientKey targetKey = effectiveTarget.getKey();
			clearPrunedPathsForSubtree(effectiveTarget);

			if (effectiveTarget.equals(graph.getRootSlot())) {
				this.targetRecipeOverride = recipeUid;
			}
			this.manualRecipes.put(targetKey, recipeUid);
			this.customRecipeInputs.remove(targetKey);

			// Ensure target is active and not marked uncrafted or pruned
			this.uncraftedKeys.remove(targetKey);
			this.prunedKeys.remove(targetKey);

			// Unprune the direct and recursive inputs of the newly selected recipe
			ITypedIngredient<?> matchTarget = effectiveTarget.getTypedIngredient().orElse(null);
			TreeRecipeBuilder.RecipeInfo<?> newRecipeInfo = TreeRecipeBuilder.findRecipe(recipeUid, matchTarget);
			if (newRecipeInfo != null) {
				unpruneSubtreeInputs(newRecipeInfo);
			}

			rebuildTreeMath(effectiveTarget);
			saveSession();
			this.pendingRecipeTarget = null;
		}
	}

	private void unpruneSubtreeInputs(TreeRecipeBuilder.RecipeInfo<?> recipeInfo) {
		Set<ResourceLocation> visited = new HashSet<>();
		unpruneSubtreeInputsRecursive(recipeInfo, visited);
	}

	private void unpruneSubtreeInputsRecursive(TreeRecipeBuilder.RecipeInfo<?> recipeInfo, Set<ResourceLocation> visited) {
		if (!visited.add(recipeInfo.recipeUid())) {
			return;
		}
		for (ITypedIngredient<?> input : recipeInfo.inputs()) {
			BookmarkIngredientKey key = BookmarkItemMetadataFactory.createPermutationKey(input, ingredientManager);
			prunedKeys.remove(key);
			uncraftedKeys.remove(key);
			ResourceLocation subRec = manualRecipes.get(key);
			if (subRec != null) {
				TreeRecipeBuilder.RecipeInfo<?> subInfo = TreeRecipeBuilder.findRecipe(subRec, input);
				if (subInfo != null) {
					unpruneSubtreeInputsRecursive(subInfo, visited);
				}
			}
		}
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

		if (anyExpanded) {
			Set<ResourceLocation> toCollapse = new HashSet<>();
			for (TreeSlot node : graph.getItems()) {
				if (graph.hasChildren(node)) {
					node.setCollapsed(true);
					if (node.getRecipeUid() != null) {
						toCollapse.add(node.getRecipeUid());
					}
				}
			}
			treeMath.setCollapsedRecipes(toCollapse);
		} else {
			for (TreeSlot node : graph.getItems()) {
				node.setCollapsed(false);
			}
			treeMath.setCollapsedRecipes(Collections.emptySet());
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
		saveSession();
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

		// 2. Collect visible nodes & draw Slot Backgrounds / Borders first
		List<VisibleTreeNode> visibleNodes = new ArrayList<>();
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
			visibleNodes.add(new VisibleTreeNode(slot, screenX, screenY, scaledSlotSize));
		}

		// Flush lines and slot backgrounds before rendering custom ingredient renderers (fluids, gases, chemicals)
		g.flush();

		// 3. Render Ingredient Icons
		for (VisibleTreeNode node : visibleNodes) {
			Optional<ITypedIngredient<?>> typedOpt = node.slot().getTypedIngredient();
			if (typedOpt.isPresent()) {
				float scale = (float) node.scaledSlotSize() / (float) TreeLayout.SLOT_SIZE;
				float itemPad = Math.max(0.0f, (node.scaledSlotSize() - 16.0f * scale) / 2.0f);
				renderTypedIngredient(g, typedOpt.get(), node.screenX() + itemPad, node.screenY() + itemPad, scale);
			}
		}

		// 4. Render Amount Overlays & Collapse Buttons on top
		for (VisibleTreeNode node : visibleNodes) {
			TreeSlot slot = node.slot();
			BigInteger amount = slot.hasRecipe() ? slot.getProducedAmount() : slot.getRequestedAmount();
			drawFormattedAmountOverlay(g, amount, slot.getKey(), node.screenX(), node.screenY(), node.scaledSlotSize());

			if (graph.hasChildren(slot)) {
				int collapseX = node.screenX() + (node.scaledSlotSize() - 9) / 2;
				int collapseY = node.screenY() + node.scaledSlotSize();
				boolean isCollapseHover = camera.isInViewport(mouseX, mouseY) &&
					mouseX >= collapseX && mouseX < collapseX + 9 &&
					mouseY >= collapseY && mouseY < collapseY + 9;

				if (isCollapseHover) {
					hoveredCollapseBtn = slot;
				}

				TreeDraw.drawCollapseButton(g, font, collapseX, collapseY, slot.isCollapsed(), isCollapseHover);
			}
		}

		g.flush();
		g.disableScissor();
	}

	private record VisibleTreeNode(TreeSlot slot, int screenX, int screenY, int scaledSlotSize) {}

	private boolean matchesSearch(TreeSlot slot) {
		if (searchQuery.isEmpty()) {
			return false;
		}
		BookmarkIngredientKey key = slot.getKey();
		if (key != null && searchTextCache.containsKey(key)) {
			return searchTextCache.get(key).contains(searchQuery);
		}
		Optional<ITypedIngredient<?>> typedOpt = slot.getTypedIngredient();
		if (typedOpt.isPresent()) {
			ITypedIngredient<?> typed = typedOpt.get();
			String name = getIngredientTooltip(typed).stream()
				.map(Component::getString)
				.reduce("", (a, b) -> a + " " + b)
				.toLowerCase(Locale.ROOT);
			if (key != null) {
				searchTextCache.put(key, name);
			}
			return name.contains(searchQuery);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private <T> void renderTypedIngredient(GuiGraphics g, ITypedIngredient<T> typed, float x, float y, float scale) {
		IIngredientRenderer<T> renderer = ingredientManager.getIngredientRenderer(typed.getType());
		mezz.jei.api.ingredients.IIngredientHelper<T> helper = ingredientManager.getIngredientHelper(typed.getType());
		T ingredient = helper.normalizeIngredient(typed.getIngredient());
		if (ingredient instanceof ItemStack stack && stack.getCount() > 1) {
			ItemStack single = stack.copy();
			single.setCount(1);
			ingredient = (T) single;
		}
		int rendererW = Math.max(1, renderer.getWidth());
		int rendererH = Math.max(1, renderer.getHeight());
		float scaleX = scale * (16.0f / rendererW);
		float scaleY = scale * (16.0f / rendererH);

		PoseStack pose = g.pose();
		pose.pushPose();
		pose.translate(x, y, 100.0f);
		if (scaleX != 1.0f || scaleY != 1.0f) {
			pose.scale(scaleX, scaleY, 1.0f);
		}
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.enableBlend();
		renderer.render(g, ingredient);
		// Flush immediately so Tesselator-based fluid/chemical renderers stay ordered and do not leak GL state
		g.flush();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.disableBlend();
		pose.popPose();
	}

	@SuppressWarnings("removal")
	private <T> List<Component> getIngredientTooltip(ITypedIngredient<T> typed) {
		IIngredientRenderer<T> renderer = ingredientManager.getIngredientRenderer(typed.getType());
		return renderer.getTooltip(typed.getIngredient(), net.minecraft.world.item.TooltipFlag.NORMAL);
	}

	private void drawFormattedAmountOverlay(
		GuiGraphics g,
		BigInteger amount,
		@Nullable BookmarkIngredientKey key,
		int screenX,
		int screenY,
		int slotSize
	) {
		drawFormattedAmountOverlay(g, amount, key, screenX, screenY, slotSize, false);
	}

	private void drawFormattedAmountOverlay(
		GuiGraphics g,
		BigInteger amount,
		@Nullable BookmarkIngredientKey key,
		int screenX,
		int screenY,
		int slotSize,
		boolean alwaysShowPositive
	) {
		boolean isFluidOrGas = key != null && BookmarkAmountFormatter.usesFluidAmountUnits(key.ingredientTypeUid());
		boolean shouldRender = (alwaysShowPositive || isFluidOrGas) ? amount.signum() > 0 : amount.compareTo(BigInteger.ONE) > 0;
		if (!shouldRender) {
			return;
		}
		String text = key != null
			? BookmarkAmountFormatter.formatTypedAmount(amount, key.ingredientTypeUid())
			: BookmarkAmountFormatter.formatItemAmount(amount);
		int textWidth = font.width(text);
		float scale = 1.0f;
		int availableWidth = slotSize - 2;
		if (textWidth > availableWidth && availableWidth > 0) {
			scale = (float) availableWidth / textWidth;
		}
		int textX = screenX + slotSize - Math.round(textWidth * scale) - 1;
		int textY = screenY + slotSize - Math.round(8 * scale);

		g.pose().pushPose();
		g.pose().translate(textX, textY, 300);
		if (scale != 1.0f) {
			g.pose().scale(scale, scale, 1.0f);
		}
		g.fill(-1, -1, textWidth + 1, 9, 0xC0000000);
		g.drawString(font, text, 0, 0, 0xFFFFFFFF, true);
		g.pose().popPose();
	}

	private void renderStatsBar(GuiGraphics g, int mouseX, int mouseY) {
		int barY = height - STATS_BAR_HEIGHT;
		g.fill(0, barY, width, height, 0xEE1A1A1A);
		g.fill(0, barY, width, barY + 1, 0xFF444444);

		hoveredStatsSlot = null;
		int drawX = 6 - statsScrollOffset;
		int drawY = barY + 4;

		// Render section labels if label mode is enabled
		for (TreeStatsLayout.PlacedLabel placedLabel : statsLayout.getPlacedLabels()) {
			int labelScreenX = drawX + placedLabel.getX();
			if (labelScreenX + font.width(placedLabel.getLabel()) >= 0 && labelScreenX < width) {
				g.drawString(font, placedLabel.getLabel(), labelScreenX, drawY + (TreeStatsLayout.SLOT_SIZE - font.lineHeight) / 2, 0xFFFFAA00, true);
			}
		}

		record VisibleStatsSlot(TreeStatsLayout.PlacedSlot placed, int screenX) {}
		List<VisibleStatsSlot> visibleSlots = new ArrayList<>();

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
			visibleSlots.add(new VisibleStatsSlot(placed, slotScreenX));
		}

		g.flush();

		for (VisibleStatsSlot visible : visibleSlots) {
			TreeStats.Row row = visible.placed().getRow();
			ITypedIngredient<?> rowIng = row.getTypedIngredient().orElseGet(() -> row.getKey() != null ? row.getKey().typedIngredient() : null);
			if (rowIng != null) {
				renderTypedIngredient(g, rowIng, visible.screenX() + 1.0f, drawY + 1.0f, 1.0f);
			} else if (row.getKind() == TreeStats.Kind.HANDLER && row.getHandlerUid() != null) {
				renderHandlerIcon(g, row.getHandlerUid(), visible.screenX() + 1.0f, drawY + 1.0f);
			}
		}

		for (VisibleStatsSlot visible : visibleSlots) {
			TreeStats.Row row = visible.placed().getRow();
			boolean alwaysShow = row.getKind() == TreeStats.Kind.HANDLER || row.getKind() == TreeStats.Kind.CRAFT_NEEDED;
			drawFormattedAmountOverlay(g, row.getAmount(), row.getKey(), visible.screenX(), drawY, TreeStatsLayout.SLOT_SIZE, alwaysShow);
		}

		g.flush();
	}

	private @Nullable mezz.jei.api.recipe.category.IRecipeCategory<?> findCategoryByUid(ResourceLocation handlerUid) {
		if (categoryCache.isEmpty()) {
			try {
				Internal.getJeiRuntime().getRecipeManager().createRecipeCategoryLookup().get()
					.forEach(cat -> categoryCache.put(cat.getRecipeType().getUid(), cat));
			} catch (Throwable ignored) {}
		}
		return categoryCache.get(handlerUid);
	}

	private void renderHandlerIcon(GuiGraphics g, ResourceLocation handlerUid, float x, float y) {
		mezz.jei.api.recipe.category.IRecipeCategory<?> category = findCategoryByUid(handlerUid);
		if (category == null) {
			return;
		}
		mezz.jei.api.gui.drawable.IDrawable icon = category.getIcon();
		if (icon != null && icon.getWidth() > 0 && icon.getHeight() > 0) {
			float scaleX = 16.0f / icon.getWidth();
			float scaleY = 16.0f / icon.getHeight();
			PoseStack pose = g.pose();
			pose.pushPose();
			pose.translate(x, y, 100.0f);
			if (scaleX != 1.0f || scaleY != 1.0f) {
				pose.scale(scaleX, scaleY, 1.0f);
			}
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			RenderSystem.enableBlend();
			icon.draw(g, 0, 0);
			g.flush();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			RenderSystem.disableBlend();
			pose.popPose();
			return;
		}
		try {
			Internal.getJeiRuntime().getRecipeManager().createRecipeCatalystLookup(category.getRecipeType()).get()
				.findFirst()
				.ifPresent(catalyst -> renderTypedIngredient(g, catalyst, x, y, 1.0f));
		} catch (Throwable ignored) {}
	}

	private Set<BookmarkIngredientKey> collectSubtreeKeys(TreeSlot root, boolean includeRoot) {
		Set<BookmarkIngredientKey> result = new HashSet<>();
		Queue<TreeSlot> queue = new ArrayDeque<>();
		if (includeRoot) {
			queue.add(root);
		} else {
			queue.addAll(graph.getChildren(root));
		}
		while (!queue.isEmpty()) {
			TreeSlot current = queue.poll();
			if (current.getKey() != null) {
				result.add(current.getKey());
			}
			if (current.getTypedIngredient().isPresent()) {
				result.add(BookmarkItemMetadataFactory.createPermutationKey(current.getTypedIngredient().get(), ingredientManager));
			}
			for (TreeSlot child : graph.getChildren(current)) {
				queue.add(child);
			}
		}
		return result;
	}

	/**
	 * Removes the target node and its branch.
	 * @param target The target node in the crafting tree.
	 * @param removeAllIdentical If true, removes all nodes sharing the same ingredient key across the entire tree.
	 *                           If false, removes only this specific node instance (by nodePath) and its subtree.
	 */
	private void removeNodeAndBranch(TreeSlot target, boolean removeAllIdentical) {
		BookmarkIngredientKey targetKey = target.getKey();
		if (targetKey == null && target.getTypedIngredient().isPresent()) {
			targetKey = BookmarkItemMetadataFactory.createPermutationKey(target.getTypedIngredient().get(), ingredientManager);
		}

		if (target.equals(graph.getRootSlot())) {
			if (targetKey != null) {
				uncraftedKeys.add(targetKey);
				manualRecipes.remove(targetKey);
				customRecipeInputs.remove(targetKey);
			}
			this.targetRecipeOverride = null;
			rebuildTreeMath();
		} else if (removeAllIdentical) {
			// Mode 1: Remove ALL identical items and their branches across the entire tree
			if (targetKey != null) {
				prunedKeys.add(targetKey);
				manualRecipes.remove(targetKey);
				customRecipeInputs.remove(targetKey);
				uncraftedKeys.remove(targetKey);
			}
			rebuildTreeMath(graph.getParent(target));
		} else {
			// Mode 2: Remove ONLY this specific node and its branch (path-based single pruning)
			if (target.getNodePath() != null && !target.getNodePath().isEmpty()) {
				prunedPaths.add(target.getNodePath());
			}
			// Only clean up manualRecipe/customInputs for targetKey if no other active branch uses targetKey
			if (targetKey != null && countOccurrencesInGraph(targetKey) <= 1) {
				manualRecipes.remove(targetKey);
				customRecipeInputs.remove(targetKey);
				uncraftedKeys.remove(targetKey);
			}
			rebuildTreeMath(graph.getParent(target));
		}
		saveSession();
	}

	private int countOccurrencesInGraph(BookmarkIngredientKey key) {
		if (graph == null) {
			return 0;
		}
		int count = 0;
		for (TreeSlot slot : graph.getItems()) {
			if (key.equals(slot.getKey())) {
				count++;
			}
		}
		return count;
	}

	private void renderTooltips(GuiGraphics g, int mouseX, int mouseY) {
		if (hoveredNode != null) {
			List<Component> tooltip = new ArrayList<>();
			Optional<ITypedIngredient<?>> typedOpt = hoveredNode.getTypedIngredient();
			if (typedOpt.isPresent()) {
				ITypedIngredient<?> typed = typedOpt.get();
				tooltip.addAll(getIngredientTooltip(typed));
			}

			String reqStr = hoveredNode.getKey() != null ? BookmarkAmountFormatter.formatTypedAmount(hoveredNode.getRequestedAmount(), hoveredNode.getKey().ingredientTypeUid()) : hoveredNode.getRequestedAmount().toString();
			String prodStr = hoveredNode.getKey() != null ? BookmarkAmountFormatter.formatTypedAmount(hoveredNode.getProducedAmount(), hoveredNode.getKey().ingredientTypeUid()) : hoveredNode.getProducedAmount().toString();
			tooltip.add(Component.translatable("jei.tree.tooltip.requested", reqStr).withStyle(ChatFormatting.GRAY));
			tooltip.add(Component.translatable("jei.tree.tooltip.produced", prodStr)
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
			tooltip.add(Component.translatable("jei.tree.hint.right_click_recipe_jei").withStyle(ChatFormatting.AQUA));
			tooltip.add(Component.translatable("jei.tree.hint.shift_click_amount").withStyle(ChatFormatting.AQUA));
			tooltip.add(Component.translatable("jei.tree.hint.recipe_keys").withStyle(ChatFormatting.DARK_GRAY));

			g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
		} else if (hoveredStatsSlot != null) {
			TreeStats.Row row = hoveredStatsSlot.getRow();
			List<Component> tooltip = new ArrayList<>();

			tooltip.add(Component.translatable("jei.tree.stats." + row.getKind().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			ITypedIngredient<?> rowIng = row.getTypedIngredient().orElseGet(() -> row.getKey() != null ? row.getKey().typedIngredient() : null);
			if (rowIng != null) {
				tooltip.addAll(getIngredientTooltip(rowIng));
			} else if (row.getKey() != null) {
				tooltip.add(Component.literal(row.getKey().ingredientUid()).withStyle(ChatFormatting.WHITE));
			} else if (row.getKind() == TreeStats.Kind.HANDLER && row.getHandlerUid() != null) {
				mezz.jei.api.recipe.category.IRecipeCategory<?> category = findCategoryByUid(row.getHandlerUid());
				Component title = category != null ? category.getTitle() : Component.literal(row.getHandlerUid().toString());
				tooltip.add(title.copy().withStyle(ChatFormatting.WHITE));
				if (row.getRecipeCount() > 0) {
					tooltip.add(Component.translatable("jei.tree.tooltip.handler_recipes", row.getRecipeCount()).withStyle(ChatFormatting.DARK_AQUA));
				}
			}
			String amtStr = row.getKey() != null ? BookmarkAmountFormatter.formatTypedAmount(row.getAmount(), row.getKey().ingredientTypeUid()) : row.getAmount().toString();
			tooltip.add(Component.translatable("jei.tree.tooltip.amount", amtStr).withStyle(ChatFormatting.GRAY));

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
				// Open JEI Recipes GUI directly to pick recipe with the '+' transfer button!
				openRecipeGuiForNode(hoveredNode);
				return true;
			} else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && Screen.hasShiftDown())) {
				// Open Quantity Adjustment Dialog
				TreeSlot target = hoveredNode;
				Minecraft.getInstance().setScreen(new TreeAdjustQuantityScreen(this, target, result -> {
					if (result.removeNode()) {
						removeNodeAndBranch(target, result.removeAllIdentical());
					} else {
						if (targetIngredient != null && target.equals(graph.getRootSlot())) {
							this.targetAmount = result.amountOverride().max(BigInteger.ONE);
							rebuildTreeMath();
						} else {
							treeMath.setManualAmount(target.getKey(), result.amountOverride());
							treeMath.setForceCraft(target.getKey(), result.forceCraft());
							recalculateTree(target);
						}
					}
				}));
				return true;
			}
		}

		// 3. Stats Bar Click Interactions
		if (hoveredStatsSlot != null) {
			TreeStats.Row row = hoveredStatsSlot.getRow();
			ITypedIngredient<?> rowIng = row.getTypedIngredient().orElseGet(() -> row.getKey() != null ? row.getKey().typedIngredient() : null);
			if (rowIng != null) {
				if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
					openRecipesGui(rowIng, RecipeIngredientRole.OUTPUT, null);
					return true;
				} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
					openRecipesGui(rowIng, RecipeIngredientRole.INPUT, null);
					return true;
				}
			}
		}

		// 4. Canvas Dragging
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
			int maxScroll = Math.max(0, statsLayout.getTotalWidth() - width + 12);
			statsScrollOffset = Math.max(0, Math.min(maxScroll, statsScrollOffset - (int) Math.round(delta * 40)));
			return true;
		}

		if (camera.isInViewport(mouseX, mouseY)) {
			if (Screen.hasShiftDown()) {
				camera.pan((float) (-delta * 60.0 / camera.getZoomLevel()), 0);
				return true;
			}
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

		// Recipe, Uses, and Delete shortcuts on hovered node
		if (hoveredNode != null) {
			if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				removeNodeAndBranch(hoveredNode, Screen.hasShiftDown());
				return true;
			}
			Optional<ITypedIngredient<?>> typedOpt = hoveredNode.getTypedIngredient();
			if (typedOpt.isPresent()) {
				ITypedIngredient<?> typed = typedOpt.get();
				if (keyCode == GLFW.GLFW_KEY_R) {
					openRecipesGui(typed, RecipeIngredientRole.OUTPUT, hoveredNode);
					return true;
				} else if (keyCode == GLFW.GLFW_KEY_U) {
					openRecipesGui(typed, RecipeIngredientRole.INPUT, hoveredNode);
					return true;
				}
			}
		} else if (hoveredStatsSlot != null) {
			TreeStats.Row row = hoveredStatsSlot.getRow();
			ITypedIngredient<?> rowIng = row.getTypedIngredient().orElseGet(() -> row.getKey() != null ? row.getKey().typedIngredient() : null);
			if (rowIng != null) {
				if (keyCode == GLFW.GLFW_KEY_R) {
					openRecipesGui(rowIng, RecipeIngredientRole.OUTPUT, null);
					return true;
				} else if (keyCode == GLFW.GLFW_KEY_U) {
					openRecipesGui(rowIng, RecipeIngredientRole.INPUT, null);
					return true;
				}
			}
		}

		// Keyboard panning via WASD and Arrow keys (Shift for turbo speed)
		// W/Up moves view UP (-scrollY), S/Down moves view DOWN (+scrollY),
		// A/Left moves view LEFT (-scrollX), D/Right moves view RIGHT (+scrollX)
		float panStep = (Screen.hasShiftDown() ? 60.0f : 20.0f) / camera.getZoomLevel();
		if (keyCode == GLFW.GLFW_KEY_W || keyCode == GLFW.GLFW_KEY_UP) {
			camera.pan(0, -panStep);
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_S || keyCode == GLFW.GLFW_KEY_DOWN) {
			camera.pan(0, panStep);
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_A || keyCode == GLFW.GLFW_KEY_LEFT) {
			camera.pan(-panStep, 0);
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_D || keyCode == GLFW.GLFW_KEY_RIGHT) {
			camera.pan(panStep, 0);
			return true;
		} else if (keyCode == GLFW.GLFW_KEY_H || keyCode == GLFW.GLFW_KEY_HOME) {
			centerOnRoot();
			return true;
		}

		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}

		return false;
	}

	private void openRecipesGui(ITypedIngredient<?> ingredient, RecipeIngredientRole role, @Nullable TreeSlot targetSlot) {
		this.pendingRecipeTarget = targetSlot;
		saveSession();
		IFocusFactory focusFactory = Internal.getJeiRuntime().getJeiHelpers().getFocusFactory();
		IFocus<?> focus = focusFactory.createFocus(role, ingredient);
		Internal.getJeiRuntime().getRecipesGui().show(List.of(focus));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
