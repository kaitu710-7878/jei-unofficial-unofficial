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
import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import mezz.jei.gui.bookmarks.BookmarkItemMetadata;
import mezz.jei.gui.bookmarks.BookmarkItemMetadataFactory;
import mezz.jei.gui.bookmarks.BookmarkItemType;
import mezz.jei.gui.bookmarks.chain.RecipeChainInput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Set;

/**
 * Builds and mutates recipe chain inputs from a target ingredient and dynamically chosen recipes.
 * Enables manual root calculation, step-by-step tree expansion, and crafting path exploration from scratch.
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
	) {}

	/**
	 * Finds all recipes that produce the given ingredient.
	 */
	public static List<RecipeInfo<?>> findRecipesFor(ITypedIngredient<?> targetIngredient) {
		List<RecipeInfo<?>> result = new ArrayList<>();
		IRecipeManager recipeManager = Internal.getJeiRuntime().getRecipeManager();
		IFocusFactory focusFactory = Internal.getJeiRuntime().getJeiHelpers().getFocusFactory();
		IFocus<?> focus = focusFactory.createFocus(RecipeIngredientRole.OUTPUT, targetIngredient);

		List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup()
			.limitFocus(List.of(focus))
			.get()
			.toList();

		for (IRecipeCategory<?> category : categories) {
			List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType())
				.limitFocus(List.of(focus))
				.get()
				.toList();

			for (Object recipe : recipes) {
				ResourceLocation recipeUid = category.getRegistryName(cast(recipe));
				if (recipeUid != null) {
					RecipeInfo<?> info = extractRecipeInfo(category, recipe, recipeUid, targetIngredient);
					if (info != null) {
						result.add(info);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Finds a specific recipe by its category and recipe UID.
	 */
	public static @Nullable RecipeInfo<?> findRecipe(ResourceLocation recipeUid, @Nullable ITypedIngredient<?> matchTarget) {
		IRecipeManager recipeManager = Internal.getJeiRuntime().getRecipeManager();
		for (IRecipeCategory<?> category : recipeManager.createRecipeCategoryLookup().get().toList()) {
			List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType()).get().toList();
			for (Object recipe : recipes) {
				ResourceLocation uid = category.getRegistryName(cast(recipe));
				if (recipeUid.equals(uid)) {
					return extractRecipeInfo(category, recipe, uid, matchTarget);
				}
			}
		}
		return null;
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
		long factor = 1;
		for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.OUTPUT)) {
			Optional<ITypedIngredient<?>> ingOpt = slot.getDisplayedIngredient().or(() -> slot.getAllIngredients().findFirst());
			ingOpt.ifPresent(outputs::add);
		}

		if (targetIngredient != null) {
			for (IRecipeSlotView slot : slotsView.getSlotViews(RecipeIngredientRole.OUTPUT)) {
				Optional<ITypedIngredient<?>> disp = slot.getDisplayedIngredient().or(() -> slot.getAllIngredients().findFirst());
				if (disp.isPresent()) {
					ITypedIngredient<?> typed = disp.get();
					if (typed.getIngredient().equals(targetIngredient.getIngredient()) ||
						(typed.getItemStack().isPresent() && targetIngredient.getItemStack().isPresent() &&
						net.minecraft.world.item.ItemStack.isSameItem(typed.getItemStack().get(), targetIngredient.getItemStack().get()))
					) {
						if (typed.getItemStack().isPresent()) {
							factor = Math.max(1, typed.getItemStack().get().getCount());
						}
						break;
					}
				}
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

			for (ITypedIngredient<?> inputIng : currentRecipe.inputs()) {
				BookmarkIngredientKey inputKey = BookmarkItemMetadataFactory.createPermutationKey(inputIng, ingredientManager);
				if (prunedKeys.contains(inputKey)) {
					continue;
				}

				ResourceLocation subRecipeUid = manualRecipes.get(inputKey);
				RecipeInfo<?> subRecipe = null;
				if (subRecipeUid != null) {
					subRecipe = findRecipe(subRecipeUid, inputIng);
				}

				// Always register inputKey as an INGREDIENT of currentRecipe
				BookmarkItemMetadata rawMeta = new BookmarkItemMetadata(
					-1,
					BookmarkItemType.INGREDIENT,
					1,
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
