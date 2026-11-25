package mezz.jei.gui.tree;

import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

/**
 * Warning dialog shown when trying to create or duplicate a crafting tree tab while at the maximum tab limit (16).
 * Lets the user select which existing tab to delete before proceeding, or cancel.
 */
public final class TreeTabLimitScreen extends Screen {
	private static final int DIALOG_WIDTH = 360;
	private static final int DIALOG_HEIGHT = 246;
	private static final int COLS = 2;
	private static final int ROWS = 8;
	private static final int CELL_H = 20;
	private static final int CELL_GAP_X = 6;
	private static final int CELL_GAP_Y = 2;

	public record TabOption(
		String tabKey,
		String displayName,
		@Nullable ITypedIngredient<?> ingredient,
		boolean active
	) {}

	private final Screen parent;
	private final List<TabOption> options;
	private final IIngredientManager ingredientManager;
	private final Consumer<String> onConfirmDelete;

	private @Nullable String selectedTabKey;
	private Button confirmButton;

	public TreeTabLimitScreen(
		Screen parent,
		List<TabOption> options,
		IIngredientManager ingredientManager,
		Consumer<String> onConfirmDelete
	) {
		super(Component.translatable("jei.tree.tab_limit.title"));
		this.parent = parent;
		this.options = options;
		this.ingredientManager = ingredientManager;
		this.onConfirmDelete = onConfirmDelete;
	}

	@Override
	protected void init() {
		int dialogW = Math.min(DIALOG_WIDTH, width - 16);
		int dialogH = Math.min(DIALOG_HEIGHT, height - 16);
		int dialogX = (width - dialogW) / 2;
		int dialogY = (height - dialogH) / 2;

		int btnW = (dialogW - 36) / 2;
		int btnY = dialogY + dialogH - 28;

		confirmButton = addRenderableWidget(Button.builder(
				Component.translatable("jei.tree.tab_limit.confirm"),
				b -> {
					if (selectedTabKey != null) {
						onConfirmDelete.accept(selectedTabKey);
					}
				}
			)
			.bounds(dialogX + 12, btnY, btnW, 20)
			.build());
		confirmButton.active = selectedTabKey != null;

		addRenderableWidget(Button.builder(
				Component.translatable("gui.cancel"),
				b -> Minecraft.getInstance().setScreen(parent)
			)
			.bounds(dialogX + dialogW - 12 - btnW, btnY, btnW, 20)
			.build());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);

		int dialogW = Math.min(DIALOG_WIDTH, width - 16);
		int dialogH = Math.min(DIALOG_HEIGHT, height - 16);
		int dialogX = (width - dialogW) / 2;
		int dialogY = (height - dialogH) / 2;

		g.fill(dialogX - 2, dialogY - 2, dialogX + dialogW + 2, dialogY + dialogH + 2, 0xFF705030);
		g.fill(dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, 0xF01C1C1C);

		g.drawString(font, title, dialogX + 12, dialogY + 10, 0xFFFFAA44, false);
		Component desc = Component.translatable("jei.tree.tab_limit.description", options.size());
		g.drawWordWrap(font, desc, dialogX + 12, dialogY + 24, dialogW - 24, 0xFFCCCCCC);

		int listStartX = dialogX + 12;
		int listStartY = dialogY + 46;
		int cellW = (dialogW - 24 - CELL_GAP_X) / COLS;

		for (int i = 0; i < options.size() && i < COLS * ROWS; i++) {
			int col = i % COLS;
			int row = i / COLS;
			int cx = listStartX + col * (cellW + CELL_GAP_X);
			int cy = listStartY + row * (CELL_H + CELL_GAP_Y);

			TabOption opt = options.get(i);
			boolean isSelected = opt.tabKey().equals(selectedTabKey);
			boolean isHover = mouseX >= cx && mouseX < cx + cellW && mouseY >= cy && mouseY < cy + CELL_H;

			int bg = isSelected ? 0xFF4A2A2A : (isHover ? 0xFF2D2D2D : 0xFF232323);
			int border = isSelected ? 0xFFFF5555 : (opt.active() ? 0xFF5599FF : 0xFF3E3E3E);
			g.fill(cx, cy, cx + cellW, cy + CELL_H, bg);
			g.fill(cx, cy, cx + cellW, cy + 1, border);
			g.fill(cx, cy + CELL_H - 1, cx + cellW, cy + CELL_H, border);
			g.fill(cx, cy, cx + 1, cy + CELL_H, border);
			g.fill(cx + cellW - 1, cy, cx + cellW, cy + CELL_H, border);

			int textX = cx + 6;
			if (opt.ingredient() != null) {
				renderIcon(g, opt.ingredient(), cx + 3, cy + 2);
				textX = cx + 22;
			}

			int maxTextW = Math.max(8, (cx + cellW - 6) - textX);
			String label = opt.displayName();
			if (font.width(label) > maxTextW) {
				label = font.plainSubstrByWidth(label, Math.max(4, maxTextW - font.width("..."))) + "...";
			}
			int textColor = isSelected ? 0xFFFFCCCC : (opt.active() ? 0xFFFFFFFF : 0xFFCCCCCC);
			g.drawString(font, label, textX, cy + 6, textColor, false);
		}

		super.render(g, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			int dialogW = Math.min(DIALOG_WIDTH, width - 16);
			int dialogH = Math.min(DIALOG_HEIGHT, height - 16);
			int dialogX = (width - dialogW) / 2;
			int dialogY = (height - dialogH) / 2;
			int listStartX = dialogX + 12;
			int listStartY = dialogY + 46;
			int cellW = (dialogW - 24 - CELL_GAP_X) / COLS;

			for (int i = 0; i < options.size() && i < COLS * ROWS; i++) {
				int col = i % COLS;
				int row = i / COLS;
				int cx = listStartX + col * (cellW + CELL_GAP_X);
				int cy = listStartY + row * (CELL_H + CELL_GAP_Y);
				if (mouseX >= cx && mouseX < cx + cellW && mouseY >= cy && mouseY < cy + CELL_H) {
					selectedTabKey = options.get(i).tabKey();
					if (confirmButton != null) {
						confirmButton.active = true;
					}
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			Minecraft.getInstance().setScreen(parent);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private <T> void renderIcon(GuiGraphics g, ITypedIngredient<T> typed, int x, int y) {
		IIngredientRenderer<T> renderer = ingredientManager.getIngredientRenderer(typed.getType());
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		renderer.render(g, typed.getIngredient());
		g.pose().popPose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
