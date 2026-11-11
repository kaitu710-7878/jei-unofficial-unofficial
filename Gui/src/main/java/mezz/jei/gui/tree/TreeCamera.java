package mezz.jei.gui.tree;

/**
 * Camera controller for the crafting tree canvas.
 * Manages pan, zoom, coordinate mapping, and frustum culling.
 */
public final class TreeCamera {
	private static final float ZOOM_MIN = 0.2f;
	private static final float ZOOM_MAX = 5.0f;
	private static final double ZOOM_STEP_BASE = 1.25;
	private static final float SCROLL_STEP_DIVISOR = 4.0f;
	private static final int CULL_MARGIN = 40;
	private static final float INITIAL_SCROLL = -16.0f;

	private int viewportX;
	private int viewportY;
	private int viewportW;
	private int viewportH;

	private float scrollX = INITIAL_SCROLL;
	private float scrollY = INITIAL_SCROLL;
	private float zoomLevel = 1.0f;

	private float lastDragX;
	private float lastDragY;
	private boolean dragging;

	public void setViewport(int x, int y, int w, int h) {
		this.viewportX = x;
		this.viewportY = y;
		this.viewportW = Math.max(1, w);
		this.viewportH = Math.max(1, h);
	}

	public int getViewportX() {
		return viewportX;
	}

	public int getViewportY() {
		return viewportY;
	}

	public int getViewportW() {
		return viewportW;
	}

	public int getViewportH() {
		return viewportH;
	}

	public float getScrollX() {
		return scrollX;
	}

	public float getScrollY() {
		return scrollY;
	}

	public void pan(float dx, float dy) {
		this.scrollX += dx;
		this.scrollY += dy;
	}

	public void setScroll(float x, float y) {
		this.scrollX = x;
		this.scrollY = y;
	}

	public float getZoomLevel() {
		return zoomLevel;
	}

	public void setZoomLevel(float zoom) {
		this.zoomLevel = clamp(zoom, ZOOM_MIN, ZOOM_MAX);
	}

	public boolean isInViewport(double screenX, double screenY) {
		return screenX >= viewportX && screenX < viewportX + viewportW &&
			screenY >= viewportY && screenY < viewportY + viewportH;
	}

	public void startDrag(double mouseX, double mouseY) {
		this.dragging = true;
		this.lastDragX = (float) mouseX;
		this.lastDragY = (float) mouseY;
	}

	public void drag(double mouseX, double mouseY) {
		if (!dragging) {
			return;
		}
		this.scrollX -= (mouseX - lastDragX) / zoomLevel;
		this.scrollY -= (mouseY - lastDragY) / zoomLevel;
		this.lastDragX = (float) mouseX;
		this.lastDragY = (float) mouseY;
	}

	public void endDrag() {
		this.dragging = false;
	}

	public boolean isDragging() {
		return dragging;
	}

	public void scroll(double mouseX, double mouseY, double scrollDelta, boolean shiftKey, boolean zoomKey) {
		if (shiftKey) {
			this.scrollX -= (float) (scrollDelta * viewportW / (SCROLL_STEP_DIVISOR * zoomLevel));
		} else if (zoomKey) {
			zoomAt(mouseX, mouseY, scrollDelta);
		} else {
			this.scrollY -= (float) (scrollDelta * viewportH / (SCROLL_STEP_DIVISOR * zoomLevel));
		}
	}

	public void zoomAt(double screenX, double screenY, double direction) {
		double localX = screenX - viewportX;
		double localY = screenY - viewportY;
		float oldZoom = this.zoomLevel;
		this.zoomLevel = clamp((float) (this.zoomLevel * Math.pow(ZOOM_STEP_BASE, direction)), ZOOM_MIN, ZOOM_MAX);
		this.scrollX += (float) (localX / oldZoom - localX / this.zoomLevel);
		this.scrollY += (float) (localY / oldZoom - localY / this.zoomLevel);
	}

	public void centerOn(float worldX, float worldY) {
		this.scrollX = worldX - (viewportW / zoomLevel) / 2.0f;
		this.scrollY = worldY - (viewportH / zoomLevel) / 2.0f;
	}

	public void clampToBounds(int treeWidth, int treeHeight) {
		float minX = -viewportW / zoomLevel + 16.0f;
		float maxX = treeWidth + 16.0f;
		float minY = -viewportH / zoomLevel + 16.0f;
		float maxY = treeHeight + 16.0f;
		this.scrollX = clamp(this.scrollX, minX, maxX);
		this.scrollY = clamp(this.scrollY, minY, maxY);
	}

	/**
	 * Fast frustum culling check for a node rectangle in world coordinates.
	 */
	public boolean isWorldRectVisible(int wx, int wy, int w, int h) {
		int screenLeft = toScreenX(wx);
		int screenTop = toScreenY(wy);
		int screenRight = toScreenX(wx + w);
		int screenBottom = toScreenY(wy + h);

		return screenRight >= viewportX - CULL_MARGIN && screenLeft <= viewportX + viewportW + CULL_MARGIN &&
			screenBottom >= viewportY - CULL_MARGIN && screenTop <= viewportY + viewportH + CULL_MARGIN;
	}

	public double[] toWorldPoint(double screenX, double screenY) {
		return new double[]{
			(screenX - viewportX) / zoomLevel + scrollX,
			(screenY - viewportY) / zoomLevel + scrollY
		};
	}

	public int toScreenX(float worldX) {
		return Math.round((worldX - scrollX) * zoomLevel + viewportX);
	}

	public int toScreenY(float worldY) {
		return Math.round((worldY - scrollY) * zoomLevel + viewportY);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
