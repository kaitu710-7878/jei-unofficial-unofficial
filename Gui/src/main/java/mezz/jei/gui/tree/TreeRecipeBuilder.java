package mezz.jei.gui.tree;

import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.Internal;
import mezz.jei.gui.bookmarks.BookmarkIngredientAmountResolver;
import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import mezz.jei.gui.bookmarks.BookmarkItemMetadata;
import mezz.jei.gui.bookmarks.BookmarkItemMetadataFactory;
import mezz.jei.gui.bookmarks.BookmarkItemType;
import mezz.jei.gui.bookmarks.chain.RecipeChainInput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds and mutates recipe chain inputs from a target ingredient and dynamically chosen recipes.
 * Enables manual root calculation, step-by-step tree expansion, and crafting path exploration from scratch.
 * Fully multi-core parallelized and cached for high performance on massive recipe trees.
 */
public final class TreeRecipeBuilder {
	private TreeRecipeBuilder() {}

	public record RecipeInfo<T>(
		IRecipeCategory<T> category,
		T recipe,
		ResourceLocation recipeUid,
		ResourceLocation categoryUid,
		List<ITypedIngredient<?>> inputs,
		List<ITypedIngredient<?>> outputs,
		long outputFactor
	) {
		public RecipeInfo<T> withFactor(long factor) {
			return new RecipeInfo<>(category, recipe, recipeUid, categoryUid, inputs, outputs, factor);
		}

		public RecipeInfo<T> withInputs(List<ITypedIngredient<?>> customInputs) {
			return new RecipeInfo<>(category, recipe, recipeUid, categoryUid, customInputs, outputs, outputFactor);
		}
	}

	public record RecipeTargetKey(ResourceLocation recipeUid, @Nullable BookmarkIngredientKey targetKey) {}

	// High-performance concurrency caches
	private static final Map<RecipeTargetKey, RecipeInfo<?>> RECIPE_UID_CACHE = new ConcurrentHashMap<>();
	private static final Map<BookmarkIngredientKey, List<RecipeInfo<?>>> CANDIDATES_CACHE = new ConcurrentHashMap<>();

	public static void clearCache() {
		RECIPE_UID_CACHE.clear();
		CANDIDATES_CACHE.clear();
	}

	/**
	 * Finds all recipes that produce the given ingredient using parallel search across recipe categories.
	 */
	public static List<RecipeInfo<?>> findRecipesFor(ITypedIngredient<?> targetIngredient) {
		try {
			Internal.getJeiRuntime();
		} catch (Throwable ignored) {
			return List.of();
		}
		IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();
		BookmarkIngredientKey targetKey = BookmarkItemMetadataFactory.createPermutationKey(targetIngredient, ingredientManager);

		List<RecipeInfo<?>> cached = CANDIDATES_CACHE.get(targetKey);
		if (cached != null) {
			return cached;
		}

		IRecipeManager recipeManager = Internal.getJeiRuntime().getRecipeManager();
		IFocusFactory focusFactory = Internal.getJeiRuntime().getJeiHelpers().getFocusFactory();
		IFocus<?> focus = focusFactory.createFocus(RecipeIngredientRole.OUTPUT, targetIngredient);

		List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup()
			.limitFocus(List.of(focus))
			.get()
			.toList();

		// Parallel multi-core evaluation across recipe categories
		List<RecipeInfo<?>> result = categories.parallelStream()
			.<RecipeInfo<?>>flatMap(category -> {
				List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType())
					.limitFocus(List.of(focus))
					.get()
					.toList();

				List<RecipeInfo<?>> catList = new ArrayList<>();
				for (Object recipe : recipes) {
					ResourceLocation recipeUid = category.getRegistryName(cast(recipe));
					if (recipeUid != null) {
						RecipeInfo<?> info = extractRecipeInfo(category, recipe, recipeUid, targetIngredient);
						if (info != null) {
							catList.add(info);
						}
					}
				}
				return catList.stream();
			})
			.toList();

		CANDIDATES_CACHE.put(targetKey, result);
		return result;
	}

	/**
	 * Finds a specific recipe by its recipe UID, matching against matchTarget if supplied.
	 */
	public static @Nullable RecipeInfo<?> findRecipe(ResourceLocation recipeUid, @Nullable ITypedIngredient<?> matchTarget) {
		try {
			Internal.getJeiRuntime();
		} catch (Throwable ignored) {
			return null;
		}
		IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();
		BookmarkIngredientKey targetKey = matchTarget != null ? BookmarkItemMetadataFactory.createPermutationKey(matchTarget, ingredientManager) : null;
		RecipeTargetKey cacheKey = new RecipeTargetKey(recipeUid, targetKey);

		RecipeInfo<?> cached = RECIPE_UID_CACHE.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		IRecipeManager recipeManager = Internal.getJeiRuntime().getRecipeManager();
		List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup().get().toList();

		// Multi-core parallel category scan for the specific UID matching matchTarget
		RecipeInfo<?> baseInfo = categories.parallelStream()
			.<RecipeInfo<?>>map(category -> {
				List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType()).get().toList();
				for (Object recipe : recipes) {
					ResourceLocation uid = category.getRegistryName(cast(recipe));
					if (recipeUid.equals(uid)) {
						return extractRecipeInfo(category, recipe, uid, matchTarget, false);
					}
				}
				return null;
			})
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);

		if (baseInfo == null && matchTarget != null) {
			// Fallback: search without target filter if category does not expose exact output match
			baseInfo = categories.parallelStream()
				.<RecipeInfo<?>>map(category -> {
					List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType()).get().toList();
					for (Object recipe : recipes) {
						ResourceLocation uid = category.getRegistryName(cast(recipe));
						if (recipeUid.equals(uid)) {
							return extractRecipeInfo(category, recipe, uid, null, false);
						}
					}
					return null;
				})
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);

			if (baseInfo != null) {
				long factor = calculateOutputFactor(baseInfo, matchTarget, ingredientManager);
				baseInfo = baseInfo.withFactor(Math.max(1, factor));
			}
		}

		if (baseInfo != null) {
			RECIPE_UID_CACHE.put(cacheKey, baseInfo);
		}
		return baseInfo;
	}

	public static <T> @Nullable RecipeInfo<T> createFromLayout(IRecipeLayoutDrawable<T> layout, @Nullable ITypedIngredient<?> targetIngredient) {
		IRecipeCategory<T> category = layout.getRecipeCategory();
		T recipe = layout.getRecipe();
		ResourceLocation recipeUid = category.getRegistryName(recipe);
		if (recipeUid == null) {
			recipeUid = new ResourceLocation(category.getRecipeType().getUid().getNamespace(), category.getRecipeType().getUid().getPath() + "_" + Math.abs(System.identityHashCode(recipe)));
		}
		IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();
		IRecipeSlotsView slotsView = layout.getRecipeSlotsView();

		List<ITypedIngredient<?>> inputs = new ArrayList<>();
		for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.INPUT)) {
			// Extract the EXACT displayed ingredient chosen by the user (scrolled items like colored glass)!
			Optional<ITypedIngredient<?>> ingOpt = slot.getDisplayedIngredient().or(() -> slot.getAllIngredients().findFirst());
			ingOpt.ifPresent(inputs::add);
		}

		List<ITypedIngredient<?>> outputs = new ArrayList<>();
		for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.OUTPUT)) {
			Optional<ITypedIngredient<?>> ingOpt = slot.getDisplayedIngredient().or(() -> slot.getAllIngredients().findFirst());
			ingOpt.ifPresent(outputs::add);
		}

		long factor = 1;
		if (targetIngredient != null) {
			BookmarkIngredientKey targetKey = BookmarkItemMetadataFactory.createPermutationKey(targetIngredient, ingredientManager);
			for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.OUTPUT)) {
				for (ITypedIngredient<?> slotIng : slot.getAllIngredients().toList()) {
					BookmarkIngredientKey slotKey = BookmarkItemMetadataFactory.createPermutationKey(slotIng, ingredientManager);
					if (targetKey.equals(slotKey) || targetKey.matchesCraftingAvailable(slotKey)) {
						factor = Math.max(1, BookmarkIngredientAmountResolver.getAmount(slotIng, ingredientManager));
						break;
					}
				}
			}
		}

		RecipeInfo<T> info = new RecipeInfo<>(
			category,
			recipe,
			recipeUid,
			category.getRecipeType().getUid(),
			inputs,
			outputs,
			factor
		);

		BookmarkIngredientKey targetKey = targetIngredient != null ? BookmarkItemMetadataFactory.createPermutationKey(targetIngredient, ingredientManager) : null;
		RECIPE_UID_CACHE.put(new RecipeTargetKey(recipeUid, targetKey), info);
		return info;
	}

	private static long calculateOutputFactor(
		RecipeInfo<?> info,
		ITypedIngredient<?> matchTarget,
		IIngredientManager ingredientManager
	) {
		BookmarkIngredientKey matchKey = BookmarkItemMetadataFactory.createPermutationKey(matchTarget, ingredientManager);
		for (ITypedIngredient<?> out : info.outputs()) {
			BookmarkIngredientKey outKey = BookmarkItemMetadataFactory.createPermutationKey(out, ingredientManager);
			if (matchKey.equals(outKey) || matchKey.matchesCraftingAvailable(outKey)) {
				return Math.max(1, BookmarkIngredientAmountResolver.getAmount(out, ingredientManager));
			}
		}
		return 1;
	}

	@SuppressWarnings("unchecked")
	private static <T> @Nullable RecipeInfo<T> extractRecipeInfo(
		IRecipeCategory<?> rawCategory,
		Object rawRecipe,
		ResourceLocation recipeUid,
		@Nullable ITypedIngredient<?> targetIngredient
	) {
		return extractRecipeInfo(rawCategory, rawRecipe, recipeUid, targetIngredient, true);
	}

	@SuppressWarnings("unchecked")
	private static <T> @Nullable RecipeInfo<T> extractRecipeInfo(
		IRecipeCategory<?> rawCategory,
		Object rawRecipe,
		ResourceLocation recipeUid,
		@Nullable ITypedIngredient<?> targetIngredient,
		boolean requireTargetMatch
	) {
		IRecipeCategory<T> category = (IRecipeCategory<T>) rawCategory;
		T recipe = (T) rawRecipe;
		IRecipeManager recipeManager = Internal.getJeiRuntime().getRecipeManager();
		IFocusFactory focusFactory = Internal.getJeiRuntime().getJeiHelpers().getFocusFactory();
		IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();

		IRecipeSlotsView slotsView = recipeManager.createRecipeLayoutDrawable(category, recipe, focusFactory.getEmptyFocusGroup())
			.map(layout -> layout.getRecipeSlotsView())
			.orElse(null);

		if (slotsView == null) {
			return null;
		}

		List<ITypedIngredient<?>> inputs = new ArrayList<>();
		for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.INPUT)) {
			Optional<ITypedIngredient<?>> ingOpt = slot.getDisplayedIngredient().or(() -> slot.getAllIngredients().findFirst());
			ingOpt.ifPresent(inputs::add);
		}

		List<ITypedIngredient<?>> outputs = new ArrayList<>();
		for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.OUTPUT)) {
			Optional<ITypedIngredient<?>> ingOpt = slot.getDisplayedIngredient().or(() -> slot.getAllIngredients().findFirst());
			ingOpt.ifPresent(outputs::add);
		}

		long factor = 1;
		if (targetIngredient != null) {
			BookmarkIngredientKey targetKey = BookmarkItemMetadataFactory.createPermutationKey(targetIngredient, ingredientManager);
			boolean matched = false;

			for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.OUTPUT)) {
				for (ITypedIngredient<?> slotIng : slot.getAllIngredients().toList()) {
					BookmarkIngredientKey slotKey = BookmarkItemMetadataFactory.createPermutationKey(slotIng, ingredientManager);
					if (targetKey.equals(slotKey) || targetKey.matchesCraftingAvailable(slotKey)) {
						factor = Math.max(1, BookmarkIngredientAmountResolver.getAmount(slotIng, ingredientManager));
						matched = true;
						break;
					}
				}
				if (matched) {
					break;
				}
			}

			// If recipe does not produce the specified target ingredient and match is strictly required, discard it
			if (!matched && requireTargetMatch) {
				return null;
			}
		}

		return new RecipeInfo<>(
			category,
			recipe,
			recipeUid,
			category.getRecipeType().getUid(),
			inputs,
			outputs,
			factor
		);
	}

	/**
	 * Builds a chain of RecipeChainInput objects starting from targetIngredient.
	 * Allows manual assignment of recipes for any ingredient node via manualRecipes,
	 * as well as pruning unwanted branches via prunedKeys.
	 */
	public static List<RecipeChainInput> buildInputsForTarget(
		ITypedIngredient<?> targetIngredient,
		BigInteger targetAmount,
		@Nullable ResourceLocation targetRecipeOverride,
		Map<BookmarkIngredientKey, ResourceLocation> manualRecipes,
		Set<BookmarkIngredientKey> prunedKeys,
		IIngredientManager ingredientManager
	) {
		return buildInputsForTarget(targetIngredient, targetAmount, targetRecipeOverride, manualRecipes, Map.of(), prunedKeys, ingredientManager);
	}

	public static List<RecipeChainInput> buildInputsForTarget(
		ITypedIngredient<?> targetIngredient,
		BigInteger targetAmount,
		@Nullable ResourceLocation targetRecipeOverride,
		Map<BookmarkIngredientKey, ResourceLocation> manualRecipes,
		Map<BookmarkIngredientKey, List<ITypedIngredient<?>>> customInputs,
		Set<BookmarkIngredientKey> prunedKeys,
		IIngredientManager ingredientManager
	) {
		return buildInputsForTarget(targetIngredient, targetAmount, targetRecipeOverride, manualRecipes, customInputs, Set.of(), prunedKeys, ingredientManager);
	}

	public static List<RecipeChainInput> buildInputsForTarget(
		ITypedIngredient<?> targetIngredient,
		BigInteger targetAmount,
		@Nullable ResourceLocation targetRecipeOverride,
		Map<BookmarkIngredientKey, ResourceLocation> manualRecipes,
		Map<BookmarkIngredientKey, List<ITypedIngredient<?>>> customInputs,
		Set<BookmarkIngredientKey> uncraftedKeys,
		Set<BookmarkIngredientKey> prunedKeys,
		IIngredientManager ingredientManager
	) {
		try {
			Internal.getJeiRuntime();
		} catch (Throwable ignored) {
			return List.of();
		}
		List<RecipeChainInput> inputs = new ArrayList<>();
		Set<BookmarkIngredientKey> visited = new HashSet<>();
		int indexCounter = 0;

		BookmarkIngredientKey rootKey = BookmarkItemMetadataFactory.createPermutationKey(targetIngredient, ingredientManager);
		if (prunedKeys.contains(rootKey)) {
			return inputs;
		}

		// Find recipe for root
		RecipeInfo<?> rootRecipe = null;
		if (!uncraftedKeys.contains(rootKey)) {
			if (targetRecipeOverride != null) {
				rootRecipe = findRecipe(targetRecipeOverride, targetIngredient);
			}
			if (rootRecipe == null) {
				List<RecipeInfo<?>> candidates = findRecipesFor(targetIngredient);
				if (!candidates.isEmpty()) {
					rootRecipe = candidates.get(0);
				}
			}
		}

		if (rootRecipe != null && customInputs.containsKey(rootKey)) {
			List<ITypedIngredient<?>> customList = customInputs.get(rootKey);
			if (customList != null && customList.size() >= rootRecipe.inputs().size()) {
				rootRecipe = rootRecipe.withInputs(customList);
			}
		}

		// Create root entry (RESULT)
		long rootMultiplier = Math.max(1, targetAmount.longValue());
		long rootFactor = rootRecipe != null ? rootRecipe.outputFactor() : 1;
		ResourceLocation rootCatUid = rootRecipe != null ? rootRecipe.categoryUid() : null;
		ResourceLocation rootRecUid = rootRecipe != null ? rootRecipe.recipeUid() : null;

		BookmarkItemMetadata rootMeta = new BookmarkItemMetadata(
			-1,
			BookmarkItemType.RESULT,
			rootMultiplier,
			rootFactor,
			BookmarkItemMetadata.CHANCE_FULL,
			rootCatUid,
			rootRecUid,
			Set.of(rootKey)
		);
		inputs.add(new RecipeChainInput(indexCounter++, rootMeta, rootKey, targetIngredient));
		visited.add(rootKey);

		if (rootRecipe == null) {
			return inputs;
		}

		// Queue for processing sub-ingredients
		Queue<RecipeInfo<?>> recipeQueue = new ArrayDeque<>();
		recipeQueue.add(rootRecipe);

		while (!recipeQueue.isEmpty()) {
			RecipeInfo<?> currentRecipe = recipeQueue.poll();

			for (ITypedIngredient<?> inputIng : currentRecipe.inputs()) {
				BookmarkIngredientKey inputKey = BookmarkItemMetadataFactory.createPermutationKey(inputIng, ingredientManager);
				if (prunedKeys.contains(inputKey)) {
					continue;
				}

				RecipeInfo<?> subRecipe = null;
				if (!uncraftedKeys.contains(inputKey)) {
					ResourceLocation subRecipeUid = manualRecipes.get(inputKey);
					if (subRecipeUid != null) {
						subRecipe = findRecipe(subRecipeUid, inputIng);
						if (subRecipe != null && customInputs.containsKey(inputKey)) {
							List<ITypedIngredient<?>> customList = customInputs.get(inputKey);
							if (customList != null && customList.size() >= subRecipe.inputs().size()) {
								subRecipe = subRecipe.withInputs(customList);
							}
						}
					}
				}

				// Always register inputKey as an INGREDIENT of currentRecipe
				BookmarkItemMetadata rawMeta = new BookmarkItemMetadata(
					-1,
					BookmarkItemType.INGREDIENT,
					Math.max(1, BookmarkIngredientAmountResolver.getAmount(inputIng, ingredientManager)),
					1,
					BookmarkItemMetadata.CHANCE_FULL,
					currentRecipe.categoryUid(),
					currentRecipe.recipeUid(),
					Set.of(inputKey)
				);
				inputs.add(new RecipeChainInput(indexCounter++, rawMeta, inputKey, inputIng));

				if (subRecipe != null && !visited.contains(inputKey)) {
					// Sub-ingredient has a manually selected recipe: expand it
					visited.add(inputKey);
					recipeQueue.add(subRecipe);
				}
			}
		}

		return inputs;
	}

	@SuppressWarnings("unchecked")
	private static <T> T cast(Object obj) {
		return (T) obj;
	}
}
