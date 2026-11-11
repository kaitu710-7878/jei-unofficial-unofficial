package mezz.jei.gui.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Calculates the horizontal slot layout for the stats bar at the bottom of the tree screen.
 */
public final class TreeStatsLayout {
	public static final int SLOT_SIZE = 18;
	public static final int SLOT_SPACING = 2;
	public static final int GROUP_SPACING = 8;
	public static final int PADDING = 4;

	public static final class PlacedSlot {
		private final TreeStats.Row row;
		private final int x;
		private final int y;

		public PlacedSlot(TreeStats.Row row, int x, int y) {
			this.row = row;
			this.x = x;
			this.y = y;
		}

		public TreeStats.Row getRow() {
			return row;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}
	}

	private final List<PlacedSlot> placedSlots = new ArrayList<>();
	private int totalWidth = 0;
	private int totalHeight = SLOT_SIZE + PADDING * 2;

	public List<PlacedSlot> getPlacedSlots() {
		return Collections.unmodifiableList(placedSlots);
	}

	public int getTotalWidth() {
		return totalWidth;
	}

	public int getTotalHeight() {
		return totalHeight;
	}

	public void recalculate(TreeStats stats) {
		placedSlots.clear();
		int currentX = PADDING;
		int slotY = PADDING;

		for (Map.Entry<TreeStats.Kind, List<TreeStats.Row>> entry : stats.getAllSections().entrySet()) {
			List<TreeStats.Row> rows = entry.getValue();
			if (rows.isEmpty()) {
				continue;
			}

			for (TreeStats.Row row : rows) {
				placedSlots.add(new PlacedSlot(row, currentX, slotY));
				currentX += SLOT_SIZE + SLOT_SPACING;
			}
			currentX += GROUP_SPACING;
		}

		totalWidth = currentX + PADDING;
	}
}
