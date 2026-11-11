package mezz.jei.common.util;

import java.math.BigInteger;

/**
 * High-performance, overflow-safe BigInteger math utilities and constants for JEI recipe chain calculations.
 */
public final class BigIntMath {
	public static final BigInteger ZERO = BigInteger.ZERO;
	public static final BigInteger ONE = BigInteger.ONE;
	public static final BigInteger TWO = BigInteger.valueOf(2);
	public static final BigInteger TEN = BigInteger.TEN;
	public static final BigInteger CHANCE_FULL = BigInteger.valueOf(10_000L);
	public static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
	public static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);

	// Cache common small integers (0 to 1024) to avoid repeated allocations in recipe loops
	private static final int CACHE_SIZE = 1025;
	private static final BigInteger[] CACHED_INTS = new BigInteger[CACHE_SIZE];

	static {
		for (int i = 0; i < CACHE_SIZE; i++) {
			CACHED_INTS[i] = BigInteger.valueOf(i);
		}
	}

	private BigIntMath() {
	}

	public static BigInteger valueOf(long val) {
		if (val >= 0 && val < CACHE_SIZE) {
			return CACHED_INTS[(int) val];
		}
		if (val == Long.MAX_VALUE) {
			return MAX_LONG;
		}
		if (val == Long.MIN_VALUE) {
			return MIN_LONG;
		}
		return BigInteger.valueOf(val);
	}

	public static BigInteger add(BigInteger a, BigInteger b) {
		if (a == null || a.signum() == 0) {
			return b == null ? ZERO : b;
		}
		if (b == null || b.signum() == 0) {
			return a;
		}
		return a.add(b);
	}

	public static BigInteger subtract(BigInteger a, BigInteger b) {
		if (b == null || b.signum() == 0) {
			return a == null ? ZERO : a;
		}
		if (a == null || a.signum() == 0) {
			return b.negate();
		}
		return a.subtract(b);
	}

	public static BigInteger multiply(BigInteger a, BigInteger b) {
		if (a == null || b == null || a.signum() == 0 || b.signum() == 0) {
			return ZERO;
		}
		if (a.equals(ONE)) {
			return b;
		}
		if (b.equals(ONE)) {
			return a;
		}
		return a.multiply(b);
	}

	public static BigInteger divideRoundUp(BigInteger numerator, BigInteger denominator) {
		if (numerator == null || denominator == null || denominator.signum() <= 0 || numerator.signum() <= 0) {
			return ZERO;
		}
		BigInteger[] divRem = numerator.divideAndRemainder(denominator);
		if (divRem[1].signum() > 0) {
			return divRem[0].add(ONE);
		}
		return divRem[0];
	}

	public static BigInteger min(BigInteger a, BigInteger b) {
		if (a == null)
			return b == null ? ZERO : b;
		if (b == null)
			return a;
		return a.compareTo(b) <= 0 ? a : b;
	}

	public static BigInteger max(BigInteger a, BigInteger b) {
		if (a == null)
			return b == null ? ZERO : b;
		if (b == null)
			return a;
		return a.compareTo(b) >= 0 ? a : b;
	}

	public static long toLongSaturated(BigInteger val) {
		if (val == null) {
			return 0L;
		}
		if (val.compareTo(MAX_LONG) > 0) {
			return Long.MAX_VALUE;
		}
		if (val.compareTo(MIN_LONG) < 0) {
			return Long.MIN_VALUE;
		}
		return val.longValue();
	}

	public static boolean isLongExact(BigInteger val) {
		return val != null && val.compareTo(MIN_LONG) >= 0 && val.compareTo(MAX_LONG) <= 0;
	}
}
