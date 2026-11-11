package mezz.jei.gui.tree;

import mezz.jei.gui.bookmarks.BookmarkGroup;
import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.bookmarks.chain.RecipeChainInput;
import net.minecraft.resources.ResourceLocation;

import java.math.BigInteger;
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
	 * Exports the state of the crafting tree back into the bookmark group.
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
}
