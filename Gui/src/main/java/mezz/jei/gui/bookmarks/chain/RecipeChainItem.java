package mezz.jei.gui.bookmarks.chain;

import mezz.jei.common.util.BigIntMath;
import mezz.jei.gui.bookmarks.BookmarkItemMetadata;

import java.math.BigInteger;

public final class RecipeChainItem {
	private final int sourceIndex;
	private final BookmarkItemMetadata metadata;
	private final RecipeChainItemType type;
	private final BigInteger bigRealAmount;
	private final BigInteger bigShiftAmount;
	private final BigInteger bigCalculatedAmount;
	private final BigInteger bigRealMultiplier;
	private final BigInteger bigCalculatedMultiplier;

	public RecipeChainItem(
		int sourceIndex,
		BookmarkItemMetadata metadata,
		RecipeChainItemType type,
		long realAmount,
		long shiftAmount,
		long calculatedAmount,
		long realMultiplier,
		long calculatedMultiplier
	) {
		this(
			sourceIndex,
			metadata,
			type,
			BigIntMath.valueOf(realAmount),
			BigIntMath.valueOf(shiftAmount),
			BigIntMath.valueOf(calculatedAmount),
			BigIntMath.valueOf(realMultiplier),
			BigIntMath.valueOf(calculatedMultiplier)
		);
	}

	public RecipeChainItem(
		int sourceIndex,
		BookmarkItemMetadata metadata,
		RecipeChainItemType type,
		BigInteger bigRealAmount,
		BigInteger bigShiftAmount,
		BigInteger bigCalculatedAmount,
		BigInteger bigRealMultiplier,
		BigInteger bigCalculatedMultiplier
	) {
		this.sourceIndex = sourceIndex;
		this.metadata = metadata;
		this.type = type;
		this.bigRealAmount = bigRealAmount == null ? BigIntMath.ZERO : bigRealAmount;
		this.bigShiftAmount = bigShiftAmount == null ? BigIntMath.ZERO : bigShiftAmount;
		this.bigCalculatedAmount = bigCalculatedAmount == null ? BigIntMath.ZERO : bigCalculatedAmount;
		this.bigRealMultiplier = bigRealMultiplier == null ? BigIntMath.ZERO : bigRealMultiplier;
		this.bigCalculatedMultiplier = bigCalculatedMultiplier == null ? BigIntMath.ZERO : bigCalculatedMultiplier;
	}

	public int sourceIndex() {
		return sourceIndex;
	}

	public BookmarkItemMetadata metadata() {
		return metadata;
	}

	public RecipeChainItemType type() {
		return type;
	}

	public long realAmount() {
		return BigIntMath.toLongSaturated(bigRealAmount);
	}

	public long shiftAmount() {
		return BigIntMath.toLongSaturated(bigShiftAmount);
	}

	public long calculatedAmount() {
		return BigIntMath.toLongSaturated(bigCalculatedAmount);
	}

	public long realMultiplier() {
		return BigIntMath.toLongSaturated(bigRealMultiplier);
	}

	public long calculatedMultiplier() {
		return BigIntMath.toLongSaturated(bigCalculatedMultiplier);
	}

	public BigInteger bigRealAmount() {
		return bigRealAmount;
	}

	public BigInteger bigShiftAmount() {
		return bigShiftAmount;
	}

	public BigInteger bigCalculatedAmount() {
		return bigCalculatedAmount;
	}

	public BigInteger bigRealMultiplier() {
		return bigRealMultiplier;
	}

	public BigInteger bigCalculatedMultiplier() {
		return bigCalculatedMultiplier;
	}

	public long requiredAmount() {
		return type == RecipeChainItemType.INGREDIENT ? shiftAmount() : 0;
	}

	public long providedAmount() {
		return type == RecipeChainItemType.INGREDIENT ? 0 : shiftAmount();
	}

	public BigInteger bigRequiredAmount() {
		return type == RecipeChainItemType.INGREDIENT ? bigShiftAmount : BigIntMath.ZERO;
	}

	public BigInteger bigProvidedAmount() {
		return type == RecipeChainItemType.INGREDIENT ? BigIntMath.ZERO : bigShiftAmount;
	}

	public RecipeChainItem withRealProjection(long realAmount, long realMultiplier) {
		return withRealProjection(BigIntMath.valueOf(realAmount), BigIntMath.valueOf(realMultiplier));
	}

	public RecipeChainItem withRealProjection(BigInteger realAmount, BigInteger realMultiplier) {
		return new RecipeChainItem(
			sourceIndex,
			metadata,
			type,
			realAmount,
			bigShiftAmount,
			bigCalculatedAmount,
			realMultiplier,
			bigCalculatedMultiplier
		);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RecipeChainItem that = (RecipeChainItem) o;
		return sourceIndex == that.sourceIndex &&
			metadata.equals(that.metadata) &&
			type == that.type &&
			bigRealAmount.equals(that.bigRealAmount) &&
			bigShiftAmount.equals(that.bigShiftAmount) &&
			bigCalculatedAmount.equals(that.bigCalculatedAmount) &&
			bigRealMultiplier.equals(that.bigRealMultiplier) &&
			bigCalculatedMultiplier.equals(that.bigCalculatedMultiplier);
	}

	@Override
	public int hashCode() {
		int result = sourceIndex;
		result = 31 * result + metadata.hashCode();
		result = 31 * result + type.hashCode();
		result = 31 * result + bigRealAmount.hashCode();
		result = 31 * result + bigShiftAmount.hashCode();
		result = 31 * result + bigCalculatedAmount.hashCode();
		result = 31 * result + bigRealMultiplier.hashCode();
		result = 31 * result + bigCalculatedMultiplier.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "RecipeChainItem[" +
			"sourceIndex=" + sourceIndex + ", " +
			"metadata=" + metadata + ", " +
			"type=" + type + ", " +
			"realAmount=" + bigRealAmount + ", " +
			"shiftAmount=" + bigShiftAmount + ", " +
			"calculatedAmount=" + bigCalculatedAmount + ", " +
			"realMultiplier=" + bigRealMultiplier + ", " +
			"calculatedMultiplier=" + bigCalculatedMultiplier + ']';
	}
}
