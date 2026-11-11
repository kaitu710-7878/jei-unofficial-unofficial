package mezz.jei.gui.tree;

import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.Internal;
import mezz.jei.gui.bookmarks.BookmarkGroup;
import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.bookmarks.RecipeLayoutProjection;
import mezz.jei.gui.bookmarks.chain.RecipeChainInput;
import net.minecraft.resources.ResourceLocation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bidirectional bridge between JEI bookmark recipe chains and the crafting tree.
 * Allows seamless round-trip synchronization between bookmark chains and interactive tree graphs.
 */
public final class TreeChainBridge {
	private TreeChainBridge() {}

	/**
	 * Creates a TreeMath instance from a BookmarkGroup's recipe chain inputs.
	 */
	public static TreeMath createTreeMathFromGroup(
		BookmarkList bookmarks,
		int groupId,
		Map<BookmarkIngredientKey, BigInteger> inventory
	) {
		List<RecipeChainInput> inputs = bookmarks.getGroupRecipeInputs(groupId);
		TreeMath treeMath = new TreeMath(inputs, inventory);

		Optional<BookmarkGroup> groupOpt = bookmarks.getGroup(groupId);
		groupOpt.ifPresent(group -> {
			treeMath.setCollapsedRecipes(group.collapsedRecipeIds());
		});

		return treeMath;
	}

	/**
	 * Exports the state of the crafting tree back into the existing bookmark group.
	 * Updates collapsed recipe ids, activates crafting mode, and synchronizes settings.
	 */
	public static void exportTreeToBookmarkGroup(
		BookmarkList bookmarks,
		int groupId,
		TreeGraph graph,
		Set<ResourceLocation> collapsedRecipes
	) {
		bookmarks.setGroupCraftingMode(groupId, true);
		bookmarks.setGroupCollapsedRecipeIds(groupId, collapsedRecipes);
	}

	/**
	 * Saves a standalone or newly created crafting tree as a brand-new bookmark group in crafting mode.
	 */
	public static Optional<Integer> saveTreeAsNewBookmarkGroup(
		BookmarkList bookmarks,
		TreeGraph graph,
		IIngredientManager ingredientManager
	) {
		IRecipeManager recipeManager = Internal.getJeiRuntime().getRecipeManager();
		IFocusFactory focusFactory = Internal.getJeiRuntime().getJeiHelpers().getFocusFactory();
		List<RecipeLayoutProjection> layouts = new ArrayList<>();
		Set<ResourceLocation> seenRecipes = new HashSet<>();

		for (TreeSlot slot : graph.getItems()) {
			ResourceLocation recipeUid = slot.getRecipeUid();
			if (recipeUid != null && !seenRecipes.contains(recipeUid)) {
				seenRecipes.add(recipeUid);
				TreeRecipeBuilder.RecipeInfo<?> info = TreeRecipeBuilder.findRecipe(recipeUid, slot.getTypedIngredient().orElse(null));
				if (info != null) {
					createRecipeLayoutDrawable(recipeManager, info.category(), info.recipe(), focusFactory.getEmptyFocusGroup())
						.ifPresent(layout -> layouts.add(new RecipeLayoutProjection(layout)));
				}
			}
		}

		if (layouts.isEmpty()) {
			return Optional.empty();
		}

		Optional<Integer> newGroupId = bookmarks.addRecipeLayoutProjectionBookmarkGroupInt(layouts, false);
		newGroupId.ifPresent(gid -> {
			bookmarks.setGroupCraftingMode(gid, true);
		});
		return newGroupId;
	}

	@SuppressWarnings("unchecked")
	private static <T> Optional<IRecipeLayoutDrawable<?>> createRecipeLayoutDrawable(
		IRecipeManager recipeManager,
		IRecipeCategory<?> rawCategory,
		Object rawRecipe,
		IFocusGroup focusGroup
	) {
		IRecipeCategory<T> category = (IRecipeCategory<T>) rawCategory;
		T recipe = (T) rawRecipe;
		return recipeManager.createRecipeLayoutDrawable(category, recipe, focusGroup).map(l -> (IRecipeLayoutDrawable<?>) l);
	}
}
