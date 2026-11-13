package mezz.jei.gui.tree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.math.BigInteger;
import java.util.function.Consumer;

/**
 * Dialog for manually adjusting the required amount or craft multiplier for a tree node.
 * Completely BigInteger-powered, supports force-craft toggle, single-node pruning, and identical-items pruning.
 */
public final class TreeAdjustQuantityScreen extends Screen {
	private static final int DIALOG_WIDTH = 236;
	private static final int DIALOG_HEIGHT = 204;

	public enum RemoveMode {
		NONE,
		SINGLE_NODE,
		ALL_IDENTICAL
	}

	public record AdjustmentResult(
		@Nullable BigInteger amountOverride,
		boolean forceCraft,
		RemoveMode removeMode
	) {
		public AdjustmentResult(@Nullable BigInteger amountOverride, boolean forceCraft, boolean removeNode) {
			this(amountOverride, forceCraft, removeNode ? RemoveMode.SINGLE_NODE : RemoveMode.NONE);
		}

		public boolean removeNode() {
			return removeMode != RemoveMode.NONE;
		}

		public boolean removeAllIdentical() {
			return removeMode == RemoveMode.ALL_IDENTICAL;
		}
	}

	private final Screen parent;
	private final TreeSlot targetSlot;
	private final Consumer<AdjustmentResult> onApply;

	private EditBox amountInput;
	private boolean forceCraft;

	public TreeAdjustQuantityScreen(
		Screen parent,
		TreeSlot targetSlot,
		Consumer<AdjustmentResult> onApply
	) {
		super(Component.translatable("jei.tree.adjust.title"));
		this.parent = parent;
		this.targetSlot = targetSlot;
		this.onApply = onApply;
		this.forceCraft = targetSlot.isForceCraft();
	}

	@Override
	protected void init() {
		int dialogX = (width - DIALOG_WIDTH) / 2;
		int dialogY = (height - DIALOG_HEIGHT) / 2;
		int innerWidth = DIALOG_WIDTH - 32;

		amountInput = new EditBox(font, dialogX + 16, dialogY + 28, innerWidth, 18, Component.literal("Amount"));
		amountInput.setMaxLength(64);
		amountInput.setValue(targetSlot.getRequestedAmount().toString());
		addRenderableWidget(amountInput);
		setInitialFocus(amountInput);

		// Quick adjustment buttons row 1
		int btnY1 = dialogY + 52;
		int colW1 = (innerWidth - 16) / 5;
		addRenderableWidget(Button.builder(Component.literal("-1"), b -> modifyAmount(-1))
			.bounds(dialogX + 16, btnY1, colW1, 18).build());
		addRenderableWidget(Button.builder(Component.literal("+1"), b -> modifyAmount(1))
			.bounds(dialogX + 16 + (colW1 + 4), btnY1, colW1, 18).build());
		addRenderableWidget(Button.builder(Component.literal("-10"), b -> modifyAmount(-10))
			.bounds(dialogX + 16 + (colW1 + 4) * 2, btnY1, colW1, 18).build());
		addRenderableWidget(Button.builder(Component.literal("+10"), b -> modifyAmount(10))
			.bounds(dialogX + 16 + (colW1 + 4) * 3, btnY1, colW1, 18).build());
		addRenderableWidget(Button.builder(Component.literal("+64"), b -> modifyAmount(64))
			.bounds(dialogX + 16 + (colW1 + 4) * 4, btnY1, innerWidth - (colW1 + 4) * 4, 18).build());

		// Quick adjustment buttons row 2 (x2, /2, x10)
		int btnY2 = dialogY + 74;
		int colW2 = (innerWidth - 12) / 4;
		addRenderableWidget(Button.builder(Component.literal("/2"), b -> divideAmount(2))
			.bounds(dialogX + 16, btnY2, colW2, 18).build());
		addRenderableWidget(Button.builder(Component.literal("x2"), b -> multiplyAmount(2))
			.bounds(dialogX + 16 + (colW2 + 4), btnY2, colW2, 18).build());
		addRenderableWidget(Button.builder(Component.literal("/10"), b -> divideAmount(10))
			.bounds(dialogX + 16 + (colW2 + 4) * 2, btnY2, colW2, 18).build());
		addRenderableWidget(Button.builder(Component.literal("x10"), b -> multiplyAmount(10))
			.bounds(dialogX + 16 + (colW2 + 4) * 3, btnY2, innerWidth - (colW2 + 4) * 3, 18).build());

		// Force Craft Toggle
		int btnY3 = dialogY + 98;
		addRenderableWidget(Button.builder(
				getForceCraftLabel(),
				b -> {
					forceCraft = !forceCraft;
					b.setMessage(getForceCraftLabel());
				}
			)
			.bounds(dialogX + 16, btnY3, innerWidth, 18).build());

		// Remove Single Node & Branch Button
		int btnY4 = dialogY + 120;
		addRenderableWidget(Button.builder(
				Component.translatable("jei.tree.adjust.remove"),
				b -> {
					onApply.accept(new AdjustmentResult(null, false, RemoveMode.SINGLE_NODE));
					onClose();
				}
			)
			.bounds(dialogX + 16, btnY4, innerWidth, 18).build());

		// Remove All Identical Items & Branches Button
		int btnY5 = dialogY + 142;
		addRenderableWidget(Button.builder(
				Component.translatable("jei.tree.adjust.remove_all"),
				b -> {
					onApply.accept(new AdjustmentResult(null, false, RemoveMode.ALL_IDENTICAL));
					onClose();
				}
			)
			.bounds(dialogX + 16, btnY5, innerWidth, 18).build());

		// Apply and Cancel
		int btnY6 = dialogY + 168;
		int halfW = (innerWidth - 8) / 2;
		addRenderableWidget(Button.builder(
				Component.translatable("gui.done"),
				b -> applyChanges()
			)
			.bounds(dialogX + 16, btnY6, halfW, 20).build());

		addRenderableWidget(Button.builder(
				Component.translatable("gui.cancel"),
				b -> onClose()
			)
			.bounds(dialogX + 16 + halfW + 8, btnY6, innerWidth - halfW - 8, 20).build());
	}

	private Component getForceCraftLabel() {
		return Component.translatable(forceCraft ? "jei.tree.adjust.force_craft.on" : "jei.tree.adjust.force_craft.off");
	}

	private void modifyAmount(long delta) {
		try {
			BigInteger current = new BigInteger(amountInput.getValue().trim());
			BigInteger next = current.add(BigInteger.valueOf(delta)).max(BigInteger.ONE);
			amountInput.setValue(next.toString());
		} catch (NumberFormatException ignored) {}
	}

	private void multiplyAmount(long factor) {
		try {
			BigInteger current = new BigInteger(amountInput.getValue().trim());
			BigInteger next = current.multiply(BigInteger.valueOf(factor)).max(BigInteger.ONE);
			amountInput.setValue(next.toString());
		} catch (NumberFormatException ignored) {}
	}

	private void divideAmount(long divisor) {
		try {
			BigInteger current = new BigInteger(amountInput.getValue().trim());
			BigInteger next = current.divide(BigInteger.valueOf(divisor)).max(BigInteger.ONE);
			amountInput.setValue(next.toString());
		} catch (NumberFormatException ignored) {}
	}

	private void applyChanges() {
		try {
			BigInteger amt = new BigInteger(amountInput.getValue().trim());
			if (amt.signum() <= 0) {
				amt = BigInteger.ONE;
			}
			onApply.accept(new AdjustmentResult(amt, forceCraft, RemoveMode.NONE));
			onClose();
		} catch (NumberFormatException ignored) {
			onClose();
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			applyChanges();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);

		int dialogX = (width - DIALOG_WIDTH) / 2;
		int dialogY = (height - DIALOG_HEIGHT) / 2;

		// Dialog container background
		g.fill(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + DIALOG_HEIGHT, 0xEE1E1E1E);
		g.fill(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + 1, 0xFF666666);
		g.fill(dialogX, dialogY + DIALOG_HEIGHT - 1, dialogX + DIALOG_WIDTH, dialogY + DIALOG_HEIGHT, 0xFF666666);
		g.fill(dialogX, dialogY, dialogX + 1, dialogY + DIALOG_HEIGHT, 0xFF666666);
		g.fill(dialogX + DIALOG_WIDTH - 1, dialogY, dialogX + DIALOG_WIDTH, dialogY + DIALOG_HEIGHT, 0xFF666666);

		// Title
		g.drawCenteredString(font, title, width / 2, dialogY + 10, 0xFFFFFFFF);

		super.render(g, mouseX, mouseY, partialTick);
	}
}
