package mezz.jei.gui.overlay.bookmarks;

import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import mezz.jei.common.platform.Services;
import mezz.jei.common.util.BigIntMath;
import mezz.jei.common.util.FluidAmountFormatter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BookmarkAmountFormatter {
	private static final Set<String> FLUID_AMOUNT_TYPE_UIDS = Set.of(
		"fluid_stack",
		"mekanism.api.chemical.gas.GasStack",
		"mekanism.api.chemical.infuse.InfusionStack",
		"mekanism.api.chemical.pigment.PigmentStack",
		"mekanism.api.chemical.slurry.SlurryStack"
	);

	// Cache small numbers 0..999 as strings to eliminate allocations during common render loops
	private static final int SMALL_CACHE_LIMIT = 1_000;
	private static final String[] SMALL_ITEM_CACHE = new String[SMALL_CACHE_LIMIT];

	static {
		for (int i = 0; i < SMALL_CACHE_LIMIT; i++) {
			SMALL_ITEM_CACHE[i] = Integer.toString(i);
		}
	}

	// LRU-like bounded cache for high-frequency formatted strings to reduce GC pressure
	private static final int MAX_CACHE_ENTRIES = 2048;
	private static final Map<Long, String> ITEM_AMOUNT_CACHE = new ConcurrentHashMap<>(256);
	private static final Map<String, String> BIGINT_ITEM_CACHE = new ConcurrentHashMap<>(128);

	private static final BigInteger THOUSAND = BigInteger.valueOf(1_000L);
	private static final BigInteger MILLION = BigInteger.valueOf(1_000_000L);
	private static final BigInteger BILLION = BigInteger.valueOf(1_000_000_000L);
	private static final BigInteger TRILLION = BigInteger.valueOf(1_000_000_000_000L);
	private static final BigInteger QUADRILLION = BigInteger.valueOf(1_000_000_000_000_000L);
	private static final BigInteger QUINTILLION = BigInteger.valueOf(1_000_000_000_000_000_000L); // 10^18 (Exa)
	private static final BigInteger SEXTILLION = new BigInteger("1000000000000000000000"); // 10^21 (Zetta)
	private static final BigInteger SEPTILLION = new BigInteger("1000000000000000000000000"); // 10^24 (Yotta)

	private BookmarkAmountFormatter() {
	}

	public static String formatItemAmount(long amount) {
		if (amount >= 0 && amount < SMALL_CACHE_LIMIT) {
			return SMALL_ITEM_CACHE[(int) amount];
		}
		if (amount < 0) {
			return Long.toString(amount);
		}
		String cached = ITEM_AMOUNT_CACHE.get(amount);
		if (cached != null) {
			return cached;
		}

		String formatted;
		if (amount < 1_000) {
			formatted = Long.toString(amount);
		} else if (amount < 1_000_000) {
			formatted = formatCompact(amount, 1_000, "K");
		} else if (amount < 1_000_000_000) {
			formatted = formatCompact(amount, 1_000_000, "M");
		} else if (amount < 1_000_000_000_000L) {
			formatted = formatCompact(amount, 1_000_000_000L, "B");
		} else if (amount < 1_000_000_000_000_000L) {
			formatted = formatCompact(amount, 1_000_000_000_000L, "T");
		} else if (amount < 1_000_000_000_000_000_000L) {
			formatted = formatCompact(amount, 1_000_000_000_000_000L, "P");
		} else {
			formatted = formatCompact(amount, 1_000_000_000_000_000_000L, "E");
		}

		if (ITEM_AMOUNT_CACHE.size() < MAX_CACHE_ENTRIES) {
			ITEM_AMOUNT_CACHE.put(amount, formatted);
		}
		return formatted;
	}

	public static String formatItemAmount(BigInteger amount) {
		if (amount == null) {
			return "0";
		}
		if (BigIntMath.isLongExact(amount)) {
			return formatItemAmount(amount.longValue());
		}
		String key = amount.toString();
		String cached = BIGINT_ITEM_CACHE.get(key);
		if (cached != null) {
			return cached;
		}

		String formatted;
		if (amount.compareTo(SEXTILLION) < 0) {
			formatted = formatCompactBigInt(amount, QUINTILLION, "E");
		} else if (amount.compareTo(SEPTILLION) < 0) {
			formatted = formatCompactBigInt(amount, SEXTILLION, "Z");
		} else if (amount.compareTo(SEPTILLION.multiply(THOUSAND)) < 0) {
			formatted = formatCompactBigInt(amount, SEPTILLION, "Y");
		} else {
			// Super massive number: use scientific exponential notation e.g. "1.2e28"
			BigDecimal bd = new BigDecimal(amount);
			int exponent = bd.precision() - bd.scale() - 1;
			BigDecimal mantissa = bd.movePointLeft(exponent).setScale(1, RoundingMode.HALF_UP);
			formatted = mantissa.stripTrailingZeros().toPlainString() + "e" + exponent;
		}

		if (BIGINT_ITEM_CACHE.size() < MAX_CACHE_ENTRIES) {
			BIGINT_ITEM_CACHE.put(key, formatted);
		}
		return formatted;
	}

	private static long getBucketVolume() {
		try {
			if (Services.PLATFORM != null && Services.PLATFORM.getFluidHelper() != null) {
				return Services.PLATFORM.getFluidHelper().bucketVolume();
			}
		} catch (Throwable ignored) {
			// Fallback when running outside Minecraft / in test harness
		}
		return 1_000L;
	}

	public static String formatFluidAmount(long amount) {
		return formatFluidAmount(amount, getBucketVolume());
	}

	public static String formatFluidAmount(long amount, long bucketVolume) {
		if (bucketVolume <= 0) {
			bucketVolume = 1_000;
		}
		if (amount == 0) {
			return "0";
		}
		if (amount < bucketVolume) {
			return amount + "mb";
		}
		if (amount < bucketVolume * 100) {
			return FluidAmountFormatter.format(amount, bucketVolume) + "B";
		}
		if (amount < bucketVolume * 1_000_000L) {
			return formatCompact(amount, bucketVolume * 1_000L, "bmb");
		}
		if (amount < bucketVolume * 1_000_000_000L) {
			return formatCompact(amount, bucketVolume * 1_000_000L, "MB");
		}
		if (amount < bucketVolume * 1_000_000_000_000L) {
			return formatCompact(amount, bucketVolume * 1_000_000_000L, "GB");
		}
		if (amount < bucketVolume * 1_000_000_000_000_000L) {
			return formatCompact(amount, bucketVolume * 1_000_000_000_000L, "TB");
		}
		if (amount < bucketVolume * 1_000_000_000_000_000_000L) {
			return formatCompact(amount, bucketVolume * 1_000_000_000_000_000L, "PB");
		}
		return formatCompact(amount, bucketVolume * 1_000_000_000_000_000_000L, "EB");
	}

	public static String formatFluidAmount(BigInteger amount) {
		return formatFluidAmount(amount, getBucketVolume());
	}

	public static String formatFluidAmount(BigInteger amount, long bucketVolume) {
		if (amount == null) {
			return "0";
		}
		if (BigIntMath.isLongExact(amount)) {
			return formatFluidAmount(amount.longValue(), bucketVolume);
		}
		if (bucketVolume <= 0) {
			bucketVolume = 1_000;
		}
		BigInteger bigBv = BigInteger.valueOf(bucketVolume);
		if (amount.signum() == 0) {
			return "0";
		}
		if (amount.compareTo(bigBv) < 0) {
			return amount.toString() + "mb";
		}
		BigInteger oneBmb = bigBv.multiply(THOUSAND);
		if (amount.compareTo(oneBmb.multiply(THOUSAND)) < 0) {
			return formatCompactBigInt(amount, oneBmb, "bmb");
		}
		BigInteger buckets = amount.divide(bigBv);
		return formatItemAmount(buckets) + "B";
	}

	public static String formatTypedAmount(long amount, Set<BookmarkIngredientKey> permutations) {
		return permutations.stream().map(BookmarkIngredientKey::ingredientTypeUid)
			.filter(BookmarkAmountFormatter::usesFluidAmountUnits)
			.findFirst()
			.map(type -> formatTypedAmount(amount, type))
			.orElseGet(() -> formatItemAmount(amount));
	}

	public static String formatTypedAmount(BigInteger amount, Set<BookmarkIngredientKey> permutations) {
		if (amount == null) {
			return "0";
		}
		if (BigIntMath.isLongExact(amount)) {
			return formatTypedAmount(amount.longValue(), permutations);
		}
		return permutations.stream().map(BookmarkIngredientKey::ingredientTypeUid)
			.filter(BookmarkAmountFormatter::usesFluidAmountUnits)
			.findFirst()
			.map(type -> formatTypedAmount(amount, type))
			.orElseGet(() -> formatItemAmount(amount));
	}

	public static String formatTypedAmount(long amount, String ingredientTypeUid) {
		if (usesFluidAmountUnits(ingredientTypeUid)) {
			return ingredientTypeUid.equals("fluid_stack") ? formatFluidAmount(amount) : formatFluidAmount(amount, 1_000);
		}
		return formatItemAmount(amount);
	}

	public static String formatTypedAmount(BigInteger amount, String ingredientTypeUid) {
		if (amount == null) {
			return "0";
		}
		if (BigIntMath.isLongExact(amount)) {
			return formatTypedAmount(amount.longValue(), ingredientTypeUid);
		}
		if (usesFluidAmountUnits(ingredientTypeUid)) {
			return ingredientTypeUid.equals("fluid_stack") ? formatFluidAmount(amount) : formatFluidAmount(amount, 1_000);
		}
		return formatItemAmount(amount);
	}

	public static boolean usesFluidAmountUnits(String ingredientTypeUid) {
		if (ingredientTypeUid == null) {
			return false;
		}
		if (FLUID_AMOUNT_TYPE_UIDS.contains(ingredientTypeUid)) {
			return true;
		}
		String lower = ingredientTypeUid.toLowerCase(java.util.Locale.ROOT);
		return lower.contains("fluid") ||
			lower.contains("chemical") ||
			lower.contains("gas") ||
			lower.contains("infus") ||
			lower.contains("pigment") ||
			lower.contains("slurry") ||
			lower.contains("waste");
	}

	/**
	 * Formats number compactly to fit inside a 16x16 / 18x18 item slot (typically 3-4 chars).
	 * If the number before the unit is >= 10, omits the decimal part to prevent overflow (e.g. 12K instead of 12.3K).
	 */
	private static String formatCompact(long amount, long divisor, String suffix) {
		long whole = amount / divisor;
		if (whole >= 100) {
			// 100 or more: omit decimals to strictly keep <= 4 characters (e.g. "100K", "999K")
			long rounded = Math.round((double) amount / divisor);
			return rounded + suffix;
		}
		// Below 100: display up to 1 decimal place (e.g. "1K", "1.2K", "9.9K", "99.9K")
		long tenths = Math.round((amount * 10.0) / divisor);
		if (tenths % 10 == 0) {
			return (tenths / 10) + suffix;
		}
		return (tenths / 10) + "." + Math.abs(tenths % 10) + suffix;
	}

	private static String formatCompactBigInt(BigInteger amount, BigInteger divisor, String suffix) {
		BigInteger[] divRem = amount.divideAndRemainder(divisor);
		BigInteger whole = divRem[0];
		if (whole.compareTo(BigInteger.valueOf(100)) >= 0) {
			if (divRem[1].multiply(BigIntMath.TWO).compareTo(divisor) >= 0) {
				whole = whole.add(BigIntMath.ONE);
			}
			return whole.toString() + suffix;
		}
		// 1 digit before decimal (up to 99.9)
		BigInteger tenthsTimes = amount.multiply(BigIntMath.TEN).divide(divisor);
		long tenths = tenthsTimes.longValue();
		if (tenths % 10 == 0) {
			return (tenths / 10) + suffix;
		}
		return (tenths / 10) + "." + Math.abs(tenths % 10) + suffix;
	}
}
