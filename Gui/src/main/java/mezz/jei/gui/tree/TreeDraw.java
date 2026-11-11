package mezz.jei.gui.tree;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * High-performance drawing utilities for the Crafting Tree GUI using GuiGraphics.
 * Provides clean, modern styled slots, connection lines, badges, and collapse indicators.
 */
public final class TreeDraw {
	// Slot appearance colors
	public static final int COLOR_SLOT_BG = 0xAA1E1E1E;
	public static final int COLOR_SLOT_BORDER = 0xFF555555;
	public static final int COLOR_SLOT_RESOLVED = 0xFF2E7D32; // Green
	public static final int COLOR_SLOT_CRAFTING = 0xFF1565C0; // Blue
	public static final int COLOR_SLOT_MISSING = 0xFFC62828; // Red
	public static final int COLOR_SLOT_HOVER = 0x66FFFFFF;
	public static final int COLOR_SLOT_SEARCH_HIGHLIGHT = 0xFFFBC02D; // Yellow

	// Line colors
	public static final int COLOR_LINE_DEFAULT = 0xFF888888;
	public static final int COLOR_LINE_MISSING = 0xFFE53935;
	public static final int COLOR_LINE_CRAFTING = 0xFF42A5F5;

	private TreeDraw() {}

	/**
	 * Draws a modern styled slot with status borders.
	 */
	public static void drawSlot(
		GuiGraphics g,
		int x,
		int y,
		int size,
		TreeSlot node,
		boolean isSearchMatch,
		boolean isHovered
	) {
		// Background fill
		g.fill(x, y, x + size, y + size, COLOR_SLOT_BG);

		// Determine border color based on node state
		int borderColor = COLOR_SLOT_BORDER;
		if (isSearchMatch) {
			borderColor = COLOR_SLOT_SEARCH_HIGHLIGHT;
		} else if (node.isResolved()) {
			borderColor = COLOR_SLOT_RESOLVED;
		} else if (!node.hasRecipe()) {
			borderColor = COLOR_SLOT_MISSING;
		} else if (node.getMultiplier().signum() > 0) {
			borderColor = COLOR_SLOT_CRAFTING;
		}

		// Outer 1px border
		g.fill(x, y, x + size, y + 1, borderColor);
		g.fill(x, y + size - 1, x + size, y + size, borderColor);
		g.fill(x, y, x + 1, y + size, borderColor);
		g.fill(x + size - 1, y, x + size, y + size, borderColor);

		// Hover highlight overlay
		if (isHovered) {
			g.fill(x + 1, y + 1, x + size - 1, y + size - 1, COLOR_SLOT_HOVER);
		}
	}

	/**
	 * Draws an orthogonal stepped connection line between parent and child slots.
	 */
	public static void drawConnectionLine(
		GuiGraphics g,
		int parentCenterX,
		int parentBottomY,
		int childCenterX,
		int childTopY,
		int lineColor
	) {
		int midY = parentBottomY + (childTopY - parentBottomY) / 2;

		// Vertical line down from parent
		g.fill(parentCenterX - 1, parentBottomY, parentCenterX + 1, midY, lineColor);
		// Horizontal cross line
		int startX = Math.min(parentCenterX, childCenterX);
		int endX = Math.max(parentCenterX, childCenterX);
		g.fill(startX - 1, midY - 1, endX + 1, midY + 1, lineColor);
		// Vertical line down to child
		g.fill(childCenterX - 1, midY, childCenterX + 1, childTopY, lineColor);
	}

	/**
	 * Draws a small [+] or [-] collapse indicator toggle button at the bottom of a node.
	 */
	public static void drawCollapseButton(
		GuiGraphics g,
		Font font,
		int x,
		int y,
		boolean collapsed,
		boolean isHovered
	) {
		int size = 9;
		int bg = isHovered ? 0xFF555555 : 0xFF2A2A2A;
		int border = isHovered ? 0xFFFFFFFF : 0xFF888888;
		int text = isHovered ? 0xFFFFFFFF : 0xFFCCCCCC;

		g.fill(x, y, x + size, y + size, bg);
		g.fill(x, y, x + size, y + 1, border);
		g.fill(x, y + size - 1, x + size, y + size, border);
		g.fill(x, y, x + 1, y + size, border);
		g.fill(x + size - 1, y, x + size, y + size, border);

		String symbol = collapsed ? "+" : "-";
		int textW = font.width(symbol);
		g.drawString(font, symbol, x + (size - textW) / 2, y + 1, text, false);
	}

	/**
	 * Draws a mini badge in the upper right or left corner of the slot.
	 */
	public static void drawBadge(GuiGraphics g, int x, int y, int color, String label, Font font) {
		int w = font.width(label) + 4;
		int h = font.lineHeight + 1;
		g.fill(x, y, x + w, y + h, color);
		g.drawString(font, label, x + 2, y + 1, 0xFFFFFFFF, false);
	}
}
