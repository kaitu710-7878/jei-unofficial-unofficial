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
import mezz.jei.common.util.BigIntMath;
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
import java.util.LinkedHashMap;
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
					RecipeInfo<?> info = extractRecipeInfo(category, recipe, recipeUid, targetIngredient);
					if (info != null) {
						catList.add(info);
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
		boolean syntheticLookup = isSyntheticRecipeUid(recipeUid);

		// Delegate recipe lookup to JEI's IRecipeManager so external recipe-search optimization mods remain compatible
		boolean requireMatch = matchTarget != null;
		RecipeInfo<?> baseInfo = categories.parallelStream()
			.<RecipeInfo<?>>map(category -> {
				if (syntheticLookup && !recipeUid.getPath().startsWith(category.getRecipeType().getUid().getPath() + "_synth_")) {
					return null;
				}
				List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType()).get().toList();
				for (Object recipe : recipes) {
					ResourceLocation uid = category.getRegistryName(cast(recipe));
					if (uid != null) {
						if (recipeUid.equals(uid)) {
							RecipeInfo<?> info = extractRecipeInfo(category, recipe, uid, matchTarget, requireMatch);
							if (info != null) {
								return info;
							}
						}
					} else if (syntheticLookup) {
						RecipeInfo<?> info = extractRecipeInfo(category, recipe, null, matchTarget, requireMatch);
						if (info != null && recipeUid.equals(info.recipeUid())) {
							return info;
						}
					}
				}
				return null;
			})
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);

		if (baseInfo == null && matchTarget != null) {
			// Fallback: search without target filter only if the category does not expose output slots
			baseInfo = categories.parallelStream()
				.<RecipeInfo<?>>map(category -> {
					if (syntheticLookup && !recipeUid.getPath().startsWith(category.getRecipeType().getUid().getPath() + "_synth_")) {
						return null;
					}
					List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType()).get().toList();
					for (Object recipe : recipes) {
						ResourceLocation uid = category.getRegistryName(cast(recipe));
						if (uid != null) {
							if (recipeUid.equals(uid)) {
								RecipeInfo<?> info = extractRecipeInfo(category, recipe, uid, null, false);
								if (info != null) {
									return info;
								}
							}
						} else if (syntheticLookup) {
							RecipeInfo<?> info = extractRecipeInfo(category, recipe, null, null, false);
							if (info != null && recipeUid.equals(info.recipeUid())) {
								return info;
							}
						}
					}
					return null;
				})
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);

			if (baseInfo != null) {
				if (!baseInfo.outputs().isEmpty() && !hasMatchingOutput(baseInfo, matchTarget, ingredientManager)) {
					// Reject mismatched recipe (e.g., gas output vs fluid output sharing the same recipe UID)
					baseInfo = null;
				} else {
					long factor = calculateOutputFactor(baseInfo, matchTarget, ingredientManager);
					baseInfo = baseInfo.withFactor(Math.max(1, factor));
				}
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
		IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();
		IRecipeSlotsView slotsView = layout.getRecipeSlotsView();

		ResourceLocation recipeUid = category.getRegistryName(recipe);
		if (recipeUid == null) {
			recipeUid = buildSyntheticRecipeUid(category.getRecipeType().getUid(), slotsView, ingredientManager);
		}

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
		} else if (!outputs.isEmpty()) {
			factor = Math.max(1, BookmarkIngredientAmountResolver.getAmount(outputs.get(0), ingredientManager));
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

	private static boolean hasMatchingOutput(
		RecipeInfo<?> info,
		ITypedIngredient<?> matchTarget,
		IIngredientManager ingredientManager
	) {
		BookmarkIngredientKey matchKey = BookmarkItemMetadataFactory.createPermutationKey(matchTarget, ingredientManager);
		for (ITypedIngredient<?> out : info.outputs()) {
			BookmarkIngredientKey outKey = BookmarkItemMetadataFactory.createPermutationKey(out, ingredientManager);
			if (matchKey.equals(outKey) || matchKey.matchesCraftingAvailable(outKey)) {
				return true;
			}
		}
		return false;
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
		try {
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
				pickDeterministicIngredient(slot, ingredientManager).ifPresent(inputs::add);
			}

			List<ITypedIngredient<?>> outputs = new ArrayList<>();
			for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.OUTPUT)) {
				pickDeterministicIngredient(slot, ingredientManager).ifPresent(outputs::add);
			}

			ResourceLocation effectiveUid = recipeUid != null
				? recipeUid
				: buildSyntheticRecipeUid(category.getRecipeType().getUid(), slotsView, ingredientManager);

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
				effectiveUid,
				category.getRecipeType().getUid(),
				inputs,
				outputs,
				factor
			);
		} catch (RuntimeException | LinkageError ignored) {
			return null;
		}
	}

	private static Optional<ITypedIngredient<?>> pickDeterministicIngredient(IRecipeSlotView slot, IIngredientManager ingredientManager) {
		List<ITypedIngredient<?>> all = slot.getAllIngredients().toList();
		if (all.isEmpty()) {
			return slot.getDisplayedIngredient();
		}
		if (all.size() == 1) {
			return Optional.of(all.get(0));
		}
		ITypedIngredient<?> best = all.get(0);
		String bestKey = BookmarkItemMetadataFactory.createPermutationKey(best, ingredientManager).stableKey();
		for (int i = 1; i < all.size(); i++) {
			ITypedIngredient<?> cand = all.get(i);
			String candKey = BookmarkItemMetadataFactory.createPermutationKey(cand, ingredientManager).stableKey();
			if (candKey.compareTo(bestKey) < 0) {
				best = cand;
				bestKey = candKey;
			}
		}
		return Optional.of(best);
	}

	public static boolean isSyntheticRecipeUid(@Nullable ResourceLocation uid) {
		return uid != null && uid.getPath().contains("_synth_");
	}

	public static <R> ResourceLocation getOrBuildRecipeUid(
		IRecipeCategory<R> category,
		R recipe,
		IRecipeSlotsView slotsView,
		IIngredientManager ingredientManager
	) {
		ResourceLocation uid = category.getRegistryName(recipe);
		if (uid != null) {
			return uid;
		}
		return buildSyntheticRecipeUid(category.getRecipeType().getUid(), slotsView, ingredientManager);
	}

	public static ResourceLocation buildSyntheticRecipeUid(
		ResourceLocation categoryUid,
		IRecipeSlotsView slotsView,
		IIngredientManager ingredientManager
	) {
		long hash = 0xcbf29ce484222325L;
		hash = fnvUpdate(hash, categoryUid.toString());
		for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.OUTPUT)) {
			hash = fnvUpdate(hash, "O");
			List<String> keys = slot.getAllIngredients()
				.map(ing -> BookmarkItemMetadataFactory.createPermutationKey(ing, ingredientManager).stableKey() + "#" + BookmarkIngredientAmountResolver.getAmount(ing, ingredientManager))
				.sorted()
				.toList();
			for (String k : keys) {
				hash = fnvUpdate(hash, k);
			}
		}
		for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.INPUT)) {
			hash = fnvUpdate(hash, "I");
			List<String> keys = slot.getAllIngredients()
				.map(ing -> BookmarkItemMetadataFactory.createPermutationKey(ing, ingredientManager).stableKey() + "#" + BookmarkIngredientAmountResolver.getAmount(ing, ingredientManager))
				.sorted()
				.toList();
			for (String k : keys) {
				hash = fnvUpdate(hash, k);
			}
		}
		return new ResourceLocation(categoryUid.getNamespace(), categoryUid.getPath() + "_synth_" + Long.toUnsignedString(hash, 16));
	}

	private static long fnvUpdate(long hash, String s) {
		for (int i = 0; i < s.length(); i++) {
			hash ^= s.charAt(i);
			hash *= 0x100000001b3L;
		}
		hash ^= 0xff;
		hash *= 0x100000001b3L;
		return hash;
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
			if (customList != null && !customList.isEmpty()) {
				rootRecipe = rootRecipe.withInputs(customList);
			}
		}

		// Create root entry (RESULT)
		long rootMultiplier = Math.max(1L, BigIntMath.toLongSaturated(targetAmount));
		long rootFactor = rootRecipe != null ? rootRecipe.outputFactor() : 1L;
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

			// Aggregate identical ingredient keys per recipe so slot counts are summed consistently
			Map<BookmarkIngredientKey, Long> aggregatedAmounts = new LinkedHashMap<>();
			Map<BookmarkIngredientKey, ITypedIngredient<?>> representativeIngredients = new LinkedHashMap<>();

			for (ITypedIngredient<?> inputIng : currentRecipe.inputs()) {
				BookmarkIngredientKey inputKey = BookmarkItemMetadataFactory.createPermutationKey(inputIng, ingredientManager);
				if (prunedKeys.contains(inputKey)) {
					continue;
				}
				long amt = Math.max(1L, BookmarkIngredientAmountResolver.getAmount(inputIng, ingredientManager));
				aggregatedAmounts.merge(inputKey, amt, Long::sum);
				representativeIngredients.putIfAbsent(inputKey, inputIng);
			}

			for (Map.Entry<BookmarkIngredientKey, Long> entry : aggregatedAmounts.entrySet()) {
				BookmarkIngredientKey inputKey = entry.getKey();
				long totalInputAmt = entry.getValue();
				ITypedIngredient<?> inputIng = representativeIngredients.get(inputKey);

				RecipeInfo<?> subRecipe = null;
				if (!uncraftedKeys.contains(inputKey)) {
					ResourceLocation subRecipeUid = manualRecipes.get(inputKey);
					if (subRecipeUid != null) {
						subRecipe = findRecipe(subRecipeUid, inputIng);
						if (subRecipe != null && customInputs.containsKey(inputKey)) {
							List<ITypedIngredient<?>> customList = customInputs.get(inputKey);
							if (customList != null && !customList.isEmpty()) {
								subRecipe = subRecipe.withInputs(customList);
							}
						}
					}
				}

				BookmarkItemMetadata rawMeta = new BookmarkItemMetadata(
					-1,
					BookmarkItemType.INGREDIENT,
					totalInputAmt,
					1,
					BookmarkItemMetadata.CHANCE_FULL,
					currentRecipe.categoryUid(),
					currentRecipe.recipeUid(),
					Set.of(inputKey)
				);
				inputs.add(new RecipeChainInput(indexCounter++, rawMeta, inputKey, inputIng));

				if (subRecipe != null && !visited.contains(inputKey)) {
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
