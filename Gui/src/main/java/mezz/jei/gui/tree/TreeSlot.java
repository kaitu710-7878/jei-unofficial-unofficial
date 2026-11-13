package mezz.jei.gui.tree;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.common.util.BigIntMath;
import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Optional;

/**
 * One node of the crafting tree. Represents an ingredient or result in the hierarchy.
 * Fully supports BigInteger quantities for unbounded and high-precision calculations.
 */
public final class TreeSlot {
	private final BookmarkIngredientKey key;
	private final @Nullable ITypedIngredient<?> typedIngredient;
	private final @Nullable ResourceLocation recipeUid;
	private final @Nullable ResourceLocation recipeTypeUid;
	private final BigInteger recipeOutputAmount;

	private final BigInteger ingeritRequestedAmount;
	private final BigInteger currentRequestedAmount;
	private final BigInteger requestedAmount;
	private final BigInteger producedAmount;
	private final BigInteger containerAmount;
	private final BigInteger leftAmount;
	private final BigInteger multiplier;
	private final boolean isContainerItem;

	// Manual adjustment and customization fields
	private boolean forceCraft;
	private @Nullable BigInteger manualAmountOverride;
	private @Nullable ResourceLocation manualRecipeOverride;
	private boolean collapsed;
	private String nodePath = "";

	public TreeSlot(
		BookmarkIngredientKey key,
		@Nullable ITypedIngredient<?> typedIngredient,
		@Nullable ResourceLocation recipeUid,
		@Nullable ResourceLocation recipeTypeUid,
		BigInteger recipeOutputAmount,
		BigInteger ingeritRequestedAmount,
		BigInteger currentRequestedAmount,
		BigInteger producedAmount,
		BigInteger containerAmount,
		BigInteger leftAmount,
		BigInteger multiplier,
		boolean isContainerItem
	) {
		this.key = key;
		this.typedIngredient = typedIngredient;
		this.recipeUid = recipeUid;
		this.recipeTypeUid = recipeTypeUid;
		this.recipeOutputAmount = recipeOutputAmount.max(BigInteger.ONE);
		this.ingeritRequestedAmount = ingeritRequestedAmount;
		this.currentRequestedAmount = currentRequestedAmount;
		this.requestedAmount = ingeritRequestedAmount.add(currentRequestedAmount);
		this.producedAmount = producedAmount;
		this.containerAmount = containerAmount;
		this.leftAmount = leftAmount;
		this.multiplier = multiplier;
		this.isContainerItem = isContainerItem;
		this.forceCraft = false;
		this.manualAmountOverride = null;
		this.manualRecipeOverride = null;
		this.collapsed = false;
	}

	public BookmarkIngredientKey getKey() {
		return key;
	}

	public Optional<ITypedIngredient<?>> getTypedIngredient() {
		return Optional.ofNullable(typedIngredient);
	}

	public @Nullable ResourceLocation getRecipeUid() {
		return manualRecipeOverride != null ? manualRecipeOverride : recipeUid;
	}

	public @Nullable ResourceLocation getOriginalRecipeUid() {
		return recipeUid;
	}

	public @Nullable ResourceLocation getRecipeTypeUid() {
		return recipeTypeUid;
	}

	public BigInteger getRecipeOutputAmount() {
		return recipeOutputAmount;
	}

	public BigInteger getIngeritRequestedAmount() {
		return ingeritRequestedAmount;
	}

	public BigInteger getCurrentRequestedAmount() {
		return currentRequestedAmount;
	}

	public BigInteger getRequestedAmount() {
		return manualAmountOverride != null ? manualAmountOverride : requestedAmount;
	}

	public BigInteger getOriginalRequestedAmount() {
		return requestedAmount;
	}

	public BigInteger getProducedAmount() {
		return producedAmount;
	}

	public BigInteger getContainerAmount() {
		return containerAmount;
	}

	public BigInteger getLeftAmount() {
		return leftAmount;
	}

	public BigInteger getMultiplier() {
		return multiplier;
	}

	public boolean isContainerItem() {
		return isContainerItem;
	}

	public boolean isForceCraft() {
		return forceCraft;
	}

	public void setForceCraft(boolean forceCraft) {
		this.forceCraft = forceCraft;
	}

	public @Nullable BigInteger getManualAmountOverride() {
		return manualAmountOverride;
	}

	public void setManualAmountOverride(@Nullable BigInteger manualAmountOverride) {
		this.manualAmountOverride = manualAmountOverride;
	}

	public @Nullable ResourceLocation getManualRecipeOverride() {
		return manualRecipeOverride;
	}

	public void setManualRecipeOverride(@Nullable ResourceLocation manualRecipeOverride) {
		this.manualRecipeOverride = manualRecipeOverride;
	}

	public boolean isCollapsed() {
		return collapsed;
	}

	public void setCollapsed(boolean collapsed) {
		this.collapsed = collapsed;
	}

	public void toggleCollapsed() {
		this.collapsed = !this.collapsed;
	}

	public boolean hasRecipe() {
		return getRecipeUid() != null;
	}

	public boolean isResolved() {
		return leftAmount.signum() == 0;
	}

	// Long compatibility accessors
	public long longRequestedAmount() {
		return BigIntMath.toLongSaturated(getRequestedAmount());
	}

	public long longLeftAmount() {
		return BigIntMath.toLongSaturated(getLeftAmount());
	}

	public long longProducedAmount() {
		return BigIntMath.toLongSaturated(getProducedAmount());
	}

	public long longMultiplier() {
		return BigIntMath.toLongSaturated(getMultiplier());
	}

	public String getNodePath() {
		return nodePath;
	}

	public void setNodePath(String nodePath) {
		this.nodePath = nodePath != null ? nodePath : "";
	}
}
