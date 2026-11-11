package mezz.jei.gui.tree;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.common.Internal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Dialog for selecting a specific recipe to produce an ingredient in the crafting tree.
 * Allows the user to override which recipe is used for any node in the hierarchy.
 */
public final class TreeRecipeSelectorScreen extends Screen {
	private static final int ITEM_HEIGHT = 28;
	private static final int DIALOG_WIDTH = 260;

	public record RecipeOption(
		ResourceLocation recipeUid,
		ResourceLocation categoryUid,
		Component title,
		@Nullable Object recipe
	) {}

	private final Screen parent;
	private final TreeSlot targetSlot;
	private final Consumer<ResourceLocation> onSelectRecipe;
	private final List<RecipeOption> availableRecipes = new ArrayList<>();
	private int scrollOffset = 0;

	public TreeRecipeSelectorScreen(
		Screen parent,
		TreeSlot targetSlot,
		Consumer<ResourceLocation> onSelectRecipe
	) {
		super(Component.translatable("jei.tree.recipe_selector.title"));
		this.parent = parent;
		this.targetSlot = targetSlot;
		this.onSelectRecipe = onSelectRecipe;
		collectRecipes();
	}

	private void collectRecipes() {
		availableRecipes.clear();
		Optional<ITypedIngredient<?>> typedOpt = targetSlot.getTypedIngredient();
		if (typedOpt.isEmpty()) {
			return;
		}

		ITypedIngredient<?> typed = typedOpt.get();
		IRecipeManager recipeManager = Internal.getJeiRuntime().getRecipeManager();
		IFocusFactory focusFactory = Internal.getJeiRuntime().getJeiHelpers().getFocusFactory();
		IFocus<?> focus = focusFactory.createFocus(RecipeIngredientRole.OUTPUT, typed);
		List<IRecipeCategory<?>> categories = recipeManager.createRecipeCategoryLookup()
			.limitFocus(List.of(focus))
			.get()
			.toList();

		for (IRecipeCategory<?> category : categories) {
			ResourceLocation catUid = category.getRecipeType().getUid();
			Component catTitle = category.getTitle();

			List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType())
				.limitFocus(List.of(focus))
				.get()
				.toList();

			for (Object recipe : recipes) {
				ResourceLocation recipeUid = category.getRegistryName(cast(recipe));
				if (recipeUid != null) {
					availableRecipes.add(new RecipeOption(recipeUid, catUid, catTitle, recipe));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T cast(Object obj) {
		return (T) obj;
	}

	@Override
	protected void init() {
		int dialogX = (width - DIALOG_WIDTH) / 2;
		int dialogY = 40;
		int dialogH = height - 80;

		// Reset to default / auto recipe button
		addRenderableWidget(Button.builder(
				Component.translatable("jei.tree.recipe_selector.auto"),
				button -> {
					onSelectRecipe.accept(null);
					onClose();
				}
			)
			.bounds(dialogX, dialogY + dialogH - 24, (DIALOG_WIDTH / 2) - 2, 20).build());

		// Cancel button
		addRenderableWidget(Button.builder(
				Component.translatable("gui.cancel"),
				button -> onClose()
			)
			.bounds(dialogX + (DIALOG_WIDTH / 2) + 2, dialogY + dialogH - 24, (DIALOG_WIDTH / 2) - 2, 20).build());
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);

		int dialogX = (width - DIALOG_WIDTH) / 2;
		int dialogY = 40;
		int dialogH = height - 80;

		// Dialog container background
		g.fill(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + dialogH, 0xEE1E1E1E);
		g.fill(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + 1, 0xFF666666);
		g.fill(dialogX, dialogY + dialogH - 1, dialogX + DIALOG_WIDTH, dialogY + dialogH, 0xFF666666);
		g.fill(dialogX, dialogY, dialogX + 1, dialogY + dialogH, 0xFF666666);
		g.fill(dialogX + DIALOG_WIDTH - 1, dialogY, dialogX + DIALOG_WIDTH, dialogY + dialogH, 0xFF666666);

		// Title
		g.drawCenteredString(font, title, width / 2, dialogY + 8, 0xFFFFFFFF);

		// Recipe options list
		int listY = dialogY + 24;
		int listH = dialogH - 54;
		int visibleCount = listH / ITEM_HEIGHT;

		if (availableRecipes.isEmpty()) {
			g.drawCenteredString(font, Component.translatable("jei.tree.recipe_selector.none"), width / 2, listY + 20, 0xFFAAAAAA);
		} else {
			for (int i = 0; i < visibleCount && (i + scrollOffset) < availableRecipes.size(); i++) {
				RecipeOption option = availableRecipes.get(i + scrollOffset);
				int itemY = listY + i * ITEM_HEIGHT;
				boolean isHovered = mouseX >= dialogX + 4 && mouseX <= dialogX + DIALOG_WIDTH - 4 &&
					mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT - 2;

				boolean isCurrent = option.recipeUid().equals(targetSlot.getRecipeUid());

				int bg = isCurrent ? 0xFF2E7D32 : (isHovered ? 0xFF3D3D3D : 0xFF252525);
				g.fill(dialogX + 4, itemY, dialogX + DIALOG_WIDTH - 4, itemY + ITEM_HEIGHT - 2, bg);

				// Category title and Recipe UID
				g.drawString(font, option.title(), dialogX + 8, itemY + 3, 0xFFFFFFFF, false);
				String shortUid = option.recipeUid().getPath();
				if (shortUid.length() > 32) {
					shortUid = shortUid.substring(0, 32) + "...";
				}
				g.drawString(font, shortUid, dialogX + 8, itemY + 14, 0xFFAAAAAA, false);
			}
		}

		super.render(g, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int dialogX = (width - DIALOG_WIDTH) / 2;
		int dialogY = 40;
		int dialogH = height - 80;
		int listY = dialogY + 24;
		int listH = dialogH - 54;
		int visibleCount = listH / ITEM_HEIGHT;

		if (mouseX >= dialogX + 4 && mouseX <= dialogX + DIALOG_WIDTH - 4 &&
			mouseY >= listY && mouseY < listY + listH
		) {
			int clickedIdx = (int) ((mouseY - listY) / ITEM_HEIGHT) + scrollOffset;
			if (clickedIdx >= 0 && clickedIdx < availableRecipes.size()) {
				RecipeOption option = availableRecipes.get(clickedIdx);
				onSelectRecipe.accept(option.recipeUid());
				onClose();
				return true;
			}
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int maxScroll = Math.max(0, availableRecipes.size() - ((height - 134) / ITEM_HEIGHT));
		if (delta < 0) {
			scrollOffset = Math.min(scrollOffset + 1, maxScroll);
			return true;
		} else if (delta > 0) {
			scrollOffset = Math.max(scrollOffset - 1, 0);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}
}
