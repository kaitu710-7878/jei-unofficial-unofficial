package mezz.jei.gui.tree;

import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Calculates the 2D layout for crafting tree nodes.
 * Parents are centered over children, with subtree collision separation.
 */
public final class TreeLayout {
	public static final int SLOT_SIZE = 18;
	public static final int X_SPACING = 30;
	public static final int Y_SPACING = 24;
	public static final int ROOT_SPACING = 42;

	public static final class Point {
		public int x;
		public int y;

		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	private final Map<TreeSlot, Point> positions = new HashMap<>();
	private final TreeMap<Integer, List<TreeSlot>> rowsByY = new TreeMap<>();
	private int treeWidth = 0;
	private int treeHeight = 0;

	public Map<TreeSlot, Point> getPositions() {
		return Collections.unmodifiableMap(positions);
	}

	public @Nullable Point getPosition(TreeSlot slot) {
		return positions.get(slot);
	}

	public TreeMap<Integer, List<TreeSlot>> getRowsByY() {
		return rowsByY;
	}

	public int getTreeWidth() {
		return treeWidth;
	}

	public int getTreeHeight() {
		return treeHeight;
	}

	/**
	 * Recalculates layout for the given graph.
	 * Returns displacement [dx, dy] of the anchor if provided, or null.
	 * Matches anchor across graph rebuilds using its deterministic nodePath or ingredient key.
	 */
	public int @Nullable [] recalculate(TreeGraph graph, @Nullable TreeSlot anchor) {
		Point oldAnchorPos = findPositionForAnchor(anchor);

		positions.clear();
		rowsByY.clear();
		treeWidth = 0;
		treeHeight = 0;

		Map<TreeSlot, Integer> offsetFromParent = new HashMap<>();
		List<int[]> combinedContour = new ArrayList<>();

		for (TreeSlot root : graph.getRoots()) {
			List<int[]> layout = computeSubtreeLayout(root, graph, offsetFromParent);
			int shift = combinedContour.isEmpty() ? 0 : calculateRootShift(combinedContour, layout);

			mergeContour(combinedContour, layout, shift);
			assignPositions(root, shift, 0, graph, offsetFromParent);
		}

		// Normalize coordinate origin to (0, 0)
		int minX = 0;
		for (Point p : positions.values()) {
			minX = Math.min(minX, p.x);
		}
		if (minX < 0) {
			for (Point p : positions.values()) {
				p.x -= minX;
			}
		}

		for (Map.Entry<TreeSlot, Point> entry : positions.entrySet()) {
			TreeSlot slot = entry.getKey();
			Point p = entry.getValue();
			rowsByY.computeIfAbsent(p.y, k -> new ArrayList<>()).add(slot);
			treeWidth = Math.max(treeWidth, p.x + SLOT_SIZE);
			treeHeight = Math.max(treeHeight, p.y + SLOT_SIZE);
		}

		if (oldAnchorPos != null) {
			Point newAnchorPos = findPositionForAnchor(anchor);
			if (newAnchorPos != null) {
				return new int[]{newAnchorPos.x - oldAnchorPos.x, newAnchorPos.y - oldAnchorPos.y};
			}
		}
		return null;
	}

	private @Nullable Point findPositionForAnchor(@Nullable TreeSlot anchor) {
		if (anchor == null || positions.isEmpty()) {
			return null;
		}
		Point direct = positions.get(anchor);
		if (direct != null) {
			return new Point(direct.x, direct.y);
		}
		String targetPath = anchor.getNodePath();
		if (targetPath != null && !targetPath.isEmpty()) {
			for (Map.Entry<TreeSlot, Point> entry : positions.entrySet()) {
				if (targetPath.equals(entry.getKey().getNodePath())) {
					Point p = entry.getValue();
					return new Point(p.x, p.y);
				}
			}
		}
		BookmarkIngredientKey targetKey = anchor.getKey();
		if (targetKey != null) {
			for (Map.Entry<TreeSlot, Point> entry : positions.entrySet()) {
				if (targetKey.equals(entry.getKey().getKey())) {
					Point p = entry.getValue();
					return new Point(p.x, p.y);
				}
			}
		}
		return null;
	}

	private List<int[]> computeSubtreeLayout(
		TreeSlot node,
		TreeGraph graph,
		Map<TreeSlot, Integer> offsetFromParent
	) {
		List<TreeSlot> children = node.isCollapsed() ? Collections.emptyList() : graph.getChildren(node);
		if (children.isEmpty()) {
			List<int[]> leaf = new ArrayList<>();
			leaf.add(new int[]{0, SLOT_SIZE});
			return leaf;
		}

		List<int[]> combined = new ArrayList<>();
		List<Integer> childOffsets = new ArrayList<>();

		for (TreeSlot child : children) {
			List<int[]> childLayout = computeSubtreeLayout(child, graph, offsetFromParent);
			int shift = combined.isEmpty() ? 0 : calculateShift(combined, childLayout);
			childOffsets.add(shift);
			mergeContour(combined, childLayout, shift);
		}

		int firstChildX = childOffsets.get(0);
		int lastChildX = childOffsets.get(childOffsets.size() - 1);
		int parentX = (firstChildX + lastChildX) / 2;

		for (int i = 0; i < children.size(); i++) {
			TreeSlot child = children.get(i);
			offsetFromParent.put(child, childOffsets.get(i) - parentX);
		}

		List<int[]> result = new ArrayList<>();
		result.add(new int[]{0, SLOT_SIZE});

		for (int[] interval : combined) {
			result.add(new int[]{interval[0] - parentX, interval[1] - parentX});
		}
		return result;
	}

	private int calculateShift(List<int[]> left, List<int[]> right) {
		int minShift = 0;
		int depth = Math.min(left.size(), right.size());
		for (int i = 0; i < depth; i++) {
			int leftMax = left.get(i)[1];
			int rightMin = right.get(i)[0];
			int required = leftMax + X_SPACING - rightMin;
			minShift = Math.max(minShift, required);
		}
		return minShift;
	}

	private int calculateRootShift(List<int[]> left, List<int[]> right) {
		int shift = calculateShift(left, right);
		return Math.max(shift, left.get(0)[1] + ROOT_SPACING - right.get(0)[0]);
	}

	private void mergeContour(List<int[]> contour, List<int[]> layout, int shift) {
		for (int i = 0; i < layout.size(); i++) {
			int[] interval = layout.get(i);
			int start = interval[0] + shift;
			int end = interval[1] + shift;

			if (i < contour.size()) {
				contour.get(i)[0] = Math.min(contour.get(i)[0], start);
				contour.get(i)[1] = Math.max(contour.get(i)[1], end);
			} else {
				contour.add(new int[]{start, end});
			}
		}
	}

	private void assignPositions(
		TreeSlot node,
		int worldX,
		int depth,
		TreeGraph graph,
		Map<TreeSlot, Integer> offsetFromParent
	) {
		int worldY = depth * (SLOT_SIZE + Y_SPACING);
		positions.put(node, new Point(worldX, worldY));

		if (node.isCollapsed()) {
			return;
		}

		for (TreeSlot child : graph.getChildren(node)) {
			int childOffset = offsetFromParent.getOrDefault(child, 0);
			assignPositions(child, worldX + childOffset, depth + 1, graph, offsetFromParent);
		}
	}
}
