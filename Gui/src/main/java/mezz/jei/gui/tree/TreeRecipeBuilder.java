package mezz.jei.gui.tree;

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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

	public record RecipeInput(
		ITypedIngredient<?> ingredient,
		boolean isNonConsumable
	) {}

	public record RecipeInfo<T>(
		IRecipeCategory<T> category,
		T recipe,
		ResourceLocation recipeUid,
		ResourceLocation categoryUid,
		List<RecipeInput> inputs,
		List<ITypedIngredient<?>> outputs,
		long outputFactor
	) {
		public RecipeInfo<T> withFactor(long factor) {
			return new RecipeInfo<>(category, recipe, recipeUid, categoryUid, inputs, outputs, factor);
		}
	}

	// High-performance concurrency caches
	private static final Map<ResourceLocation, RecipeInfo<?>> RECIPE_UID_CACHE = new ConcurrentHashMap<>();
	private static final Map<BookmarkIngredientKey, List<RecipeInfo<?>>> CANDIDATES_CACHE = new ConcurrentHashMap<>();

	public static void clearCache() {
		RECIPE_UID_CACHE.clear();
		CANDIDATES_CACHE.clear();
	}

	/**
	 * Finds all recipes that produce the given ingredient using parallel search across recipe categories.
	 */
	public static List<RecipeInfo<?>> findRecipesFor(ITypedIngredient<?> targetIngredient) {
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
		RecipeInfo<?> baseInfo = RECIPE_UID_CACHE.get(recipeUid);
		IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();

		if (baseInfo == null) {
			IRecipeManager recipeManager = Internal.getJeiRuntime().getRecipeManager();
			List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup().get().toList();

			// Multi-core parallel category scan for the specific UID
			baseInfo = categories.parallelStream()
				.<RecipeInfo<?>>map(category -> {
					List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType()).get().toList();
					for (Object recipe : recipes) {
						ResourceLocation uid = category.getRegistryName(cast(recipe));
						if (recipeUid.equals(uid)) {
							return extractRecipeInfo(category, recipe, uid, null);
						}
					}
					return null;
				})
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);

			if (baseInfo != null) {
				RECIPE_UID_CACHE.put(recipeUid, baseInfo);
			}
		}

		if (baseInfo == null) {
			return null;
		}

		if (matchTarget == null) {
			return baseInfo;
		}

		// Strictly verify that the recipe produces matchTarget with identical ingredient type & UID
		BookmarkIngredientKey targetKey = BookmarkItemMetadataFactory.createPermutationKey(matchTarget, ingredientManager);
		long factor = calculateMatchingOutputFactor(baseInfo, targetKey, ingredientManager);
		if (factor <= 0) {
			// Recipe does not produce the target ingredient type or UID
			return null;
		}

		return baseInfo.withFactor(factor);
	}

	private static long calculateMatchingOutputFactor(RecipeInfo<?> info, BookmarkIngredientKey targetKey, IIngredientManager ingredientManager) {
		for (ITypedIngredient<?> output : info.outputs()) {
			BookmarkIngredientKey outKey = BookmarkItemMetadataFactory.createPermutationKey(output, ingredientManager);
			if (targetKey.equals(outKey)) {
				return Math.max(1, BookmarkIngredientAmountResolver.getAmount(output, ingredientManager));
			}
		}
		return -1;
	}

	public static boolean isNonConsumable(
		ITypedIngredient<?> input,
		List<ITypedIngredient<?>> outputs,
		IRecipeSlotView slotView,
		IIngredientManager ingredientManager
	) {
		// 1. Explicit Catalyst Role
		if (slotView.getRole() == RecipeIngredientRole.CATALYST) {
			return true;
		}

		BookmarkIngredientKey inKey = BookmarkItemMetadataFactory.createPermutationKey(input, ingredientManager);

		// 2. Output contains the exact same ingredient key (not consumed)
		for (ITypedIngredient<?> out : outputs) {
			BookmarkIngredientKey outKey = BookmarkItemMetadataFactory.createPermutationKey(out, ingredientManager);
			if (inKey.equals(outKey)) {
				return true;
			}
		}

		// 3. Known mold / press / cast / circuit / tool items that are not consumed (e.g. AE2 inscriber press, GregTech circuit)
		String uid = inKey.ingredientUid().toLowerCase(Locale.ROOT);
		if (uid.contains("press") ||
			uid.contains("mold") ||
			uid.contains("cast") ||
			uid.contains("die") ||
			uid.contains("stamp") ||
			uid.contains("programmed_circuit")
		) {
			return true;
		}

		// 4. Vanilla/Forge crafting remainder (e.g. buckets, reusable tools)
		if (input.getItemStack().isPresent()) {
			ItemStack stack = input.getItemStack().get();
			if (stack.getItem().hasCraftingRemainingItem()) {
				return true;
			}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	private static <T> @Nullable RecipeInfo<T> extractRecipeInfo(
		IRecipeCategory<?> rawCategory,
		Object rawRecipe,
		ResourceLocation recipeUid,
		@Nullable ITypedIngredient<?> targetIngredient
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

		List<ITypedIngredient<?>> outputs = new ArrayList<>();
		for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.OUTPUT)) {
			Optional<ITypedIngredient<?>> ingOpt = slot.getDisplayedIngredient().or(() -> slot.getAllIngredients().findFirst());
			ingOpt.ifPresent(outputs::add);
		}

		List<RecipeInput> inputs = new ArrayList<>();
		for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.INPUT)) {
			Optional<ITypedIngredient<?>> ingOpt = slot.getDisplayedIngredient().or(() -> slot.getAllIngredients().findFirst());
			if (ingOpt.isPresent()) {
				ITypedIngredient<?> in = ingOpt.get();
				boolean nonConsumable = isNonConsumable(in, outputs, slot, ingredientManager);
				inputs.add(new RecipeInput(in, nonConsumable));
			}
		}
		for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.CATALYST)) {
			Optional<ITypedIngredient<?>> ingOpt = slot.getDisplayedIngredient().or(() -> slot.getAllIngredients().findFirst());
			if (ingOpt.isPresent()) {
				inputs.add(new RecipeInput(ingOpt.get(), true));
			}
		}

		long factor = 1;
		if (targetIngredient != null) {
			BookmarkIngredientKey targetKey = BookmarkItemMetadataFactory.createPermutationKey(targetIngredient, ingredientManager);
			boolean matched = false;

			for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.OUTPUT)) {
				for (ITypedIngredient<?> slotIng : slot.getAllIngredients().toList()) {
					BookmarkIngredientKey slotKey = BookmarkItemMetadataFactory.createPermutationKey(slotIng, ingredientManager);
					if (targetKey.equals(slotKey)) {
						factor = Math.max(1, BookmarkIngredientAmountResolver.getAmount(slotIng, ingredientManager));
						matched = true;
						break;
					}
				}
				if (matched) {
					break;
				}
			}

			// If recipe does not produce the specified target ingredient, discard it
			if (!matched) {
				return null;
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

		RECIPE_UID_CACHE.putIfAbsent(recipeUid, info);
		return info;
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
		List<RecipeChainInput> inputs = new ArrayList<>();
		Set<BookmarkIngredientKey> visited = new HashSet<>();
		int indexCounter = 0;

		BookmarkIngredientKey rootKey = BookmarkItemMetadataFactory.createPermutationKey(targetIngredient, ingredientManager);
		if (prunedKeys.contains(rootKey)) {
			return inputs;
		}

		// Find recipe for root
		RecipeInfo<?> rootRecipe = null;
		if (targetRecipeOverride != null) {
			rootRecipe = findRecipe(targetRecipeOverride, targetIngredient);
		}
		if (rootRecipe == null) {
			List<RecipeInfo<?>> candidates = findRecipesFor(targetIngredient);
			if (!candidates.isEmpty()) {
				rootRecipe = candidates.get(0);
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

			for (RecipeInput inputWrapper : currentRecipe.inputs()) {
				ITypedIngredient<?> inputIng = inputWrapper.ingredient();
				BookmarkIngredientKey inputKey = BookmarkItemMetadataFactory.createPermutationKey(inputIng, ingredientManager);
				if (prunedKeys.contains(inputKey)) {
					continue;
				}

				ResourceLocation subRecipeUid = manualRecipes.get(inputKey);
				RecipeInfo<?> subRecipe = null;
				if (subRecipeUid != null) {
					subRecipe = findRecipe(subRecipeUid, inputIng);
				}

				BookmarkItemType itemType = inputWrapper.isNonConsumable() ? BookmarkItemType.NONCONSUMABLE : BookmarkItemType.INGREDIENT;
				BookmarkItemMetadata rawMeta = new BookmarkItemMetadata(
					-1,
					itemType,
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
