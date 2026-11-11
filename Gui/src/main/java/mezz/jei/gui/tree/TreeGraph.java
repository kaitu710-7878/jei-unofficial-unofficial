package mezz.jei.gui.tree;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Graph structure representing the parent-child relationships in the crafting tree.
 * Provides caching of missing resources and subtree pruning operations.
 */
public final class TreeGraph {
	private final List<TreeSlot> roots = new ArrayList<>();
	private final List<TreeSlot> items = new ArrayList<>();
	private final Map<TreeSlot, TreeSlot> parentOf = new HashMap<>();
	private final Map<TreeSlot, List<TreeSlot>> childrenOf = new HashMap<>();

	private final Set<TreeSlot> missingSubtreeCache = new HashSet<>();
	private final Set<TreeSlot> needCraftSubtreeCache = new HashSet<>();

	public void clear() {
		roots.clear();
		items.clear();
		parentOf.clear();
		childrenOf.clear();
		missingSubtreeCache.clear();
		needCraftSubtreeCache.clear();
	}

	public void addRoot(TreeSlot root) {
		roots.add(root);
		items.add(root);
	}

	public void addChild(TreeSlot parent, TreeSlot child) {
		items.add(child);
		parentOf.put(child, parent);
		childrenOf.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
	}

	public List<TreeSlot> getRoots() {
		return Collections.unmodifiableList(roots);
	}

	public List<TreeSlot> getItems() {
		return Collections.unmodifiableList(items);
	}

	public TreeSlot getParent(TreeSlot slot) {
		return parentOf.get(slot);
	}

	public List<TreeSlot> getChildren(TreeSlot slot) {
		return childrenOf.getOrDefault(slot, Collections.emptyList());
	}

	public boolean hasChildren(TreeSlot slot) {
		List<TreeSlot> children = childrenOf.get(slot);
		return children != null && !children.isEmpty();
	}

	public boolean hasMissingSubtree(TreeSlot slot) {
		return missingSubtreeCache.contains(slot);
	}

	public boolean hasNeedCraftSubtree(TreeSlot slot) {
		return needCraftSubtreeCache.contains(slot);
	}

	public void updateCaches() {
		missingSubtreeCache.clear();
		needCraftSubtreeCache.clear();

		for (TreeSlot root : roots) {
			computeCaches(root);
		}
	}

	private boolean computeCaches(TreeSlot node) {
		List<TreeSlot> children = getChildren(node);
		boolean childMissing = false;
		boolean childNeedsCraft = false;

		if (children.isEmpty()) {
			// Leaf node: missing if it needs craft but has no recipe
			if (!node.isResolved() && !node.hasRecipe()) {
				childMissing = true;
			}
		} else {
			for (TreeSlot child : children) {
				boolean cMissing = computeCaches(child);
				childMissing |= cMissing;
				if (!child.isResolved()) {
					childNeedsCraft = true;
				}
			}
		}

		if (childMissing) {
			missingSubtreeCache.add(node);
		}
		if (childNeedsCraft || (!node.isResolved() && node.hasRecipe())) {
			needCraftSubtreeCache.add(node);
		}

		return childMissing;
	}

	/**
	 * Collects unused recipe UIDs that were solely providing for the deleted node.
	 */
	public Set<ResourceLocation> collectUnusedRecipes(TreeSlot target) {
		ResourceLocation targetRecipe = target.getRecipeUid();
		if (targetRecipe == null) {
			return Collections.emptySet();
		}

		Map<ResourceLocation, Set<ResourceLocation>> childrenRecipes = new HashMap<>();
		Map<ResourceLocation, Set<ResourceLocation>> parentRecipes = new HashMap<>();
		int targetRecipeOccurrence = 0;

		for (TreeSlot slot : items) {
			ResourceLocation slotRecipe = slot.getRecipeUid();
			TreeSlot parent = parentOf.get(slot);
			ResourceLocation parentRecipe = parent != null ? parent.getRecipeUid() : null;

			if (slotRecipe != null && parentRecipe != null) {
				childrenRecipes.computeIfAbsent(parentRecipe, k -> new HashSet<>()).add(slotRecipe);
				parentRecipes.computeIfAbsent(slotRecipe, k -> new HashSet<>()).add(parentRecipe);
			}
			if (targetRecipe.equals(slotRecipe)) {
				targetRecipeOccurrence++;
			}
		}

		Set<ResourceLocation> unused = new HashSet<>();
		if (targetRecipeOccurrence <= 1) {
			unused.add(targetRecipe);
			collectSubtreeRecipes(targetRecipe, childrenRecipes, parentRecipes, unused);
		}
		return unused;
	}

	private void collectSubtreeRecipes(
		ResourceLocation recipe,
		Map<ResourceLocation, Set<ResourceLocation>> children,
		Map<ResourceLocation, Set<ResourceLocation>> parents,
		Set<ResourceLocation> unused
	) {
		for (ResourceLocation child : children.getOrDefault(recipe, Collections.emptySet())) {
			Set<ResourceLocation> p = parents.get(child);
			if (p != null && p.size() <= 1 && unused.add(child)) {
				collectSubtreeRecipes(child, children, parents, unused);
			}
		}
	}
}
