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
	 * Line thickness is clamped between 1px and 2px regardless of zoom.
	 */
	public static void drawConnectionLine(
		GuiGraphics g,
		int parentCenterX,
		int parentBottomY,
		int childCenterX,
		int childTopY,
		int lineColor,
		float zoom
	) {
		int midY = parentBottomY + (childTopY - parentBottomY) / 2;
		int halfNeg = zoom >= 0.75f ? 1 : 0;
		int halfPos = 1; // Always at least 1px thick (total 1px when halfNeg=0, 2px when halfNeg=1)

		// Vertical line down from parent
		g.fill(parentCenterX - halfNeg, parentBottomY, parentCenterX + halfPos, midY, lineColor);
		// Horizontal cross line
		int startX = Math.min(parentCenterX, childCenterX);
		int endX = Math.max(parentCenterX, childCenterX);
		g.fill(startX - halfNeg, midY - halfNeg, endX + halfPos, midY + halfPos, lineColor);
		// Vertical line down to child
		g.fill(childCenterX - halfNeg, midY, childCenterX + halfPos, childTopY, lineColor);
	}

	/**
	 * Draws a small [+] or [-] collapse indicator toggle button at the bottom of a node.
	 */
	public static void drawCollapseButton(
		GuiGraphics g,
		Font font,
		int x,
		int y,
		int size,
		boolean collapsed,
		boolean isHovered
	) {
		if (size < 4) {
			return;
		}
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
		float textScale = Math.min(1.0f, (float) size / 9.0f);
		g.pose().pushPose();
		g.pose().translate(x + (size - textW * textScale) / 2.0f, y + (size - 7.0f * textScale) / 2.0f, 0.0f);
		if (textScale != 1.0f) {
			g.pose().scale(textScale, textScale, 1.0f);
		}
		g.drawString(font, symbol, 0, 0, text, false);
		g.pose().popPose();
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
