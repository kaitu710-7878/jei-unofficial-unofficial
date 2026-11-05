package mezz.jei.test.gui.bookmarks;

import mezz.jei.common.util.BigIntMath;
import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import mezz.jei.gui.bookmarks.BookmarkItemMetadata;
import mezz.jei.gui.bookmarks.BookmarkItemType;
import mezz.jei.gui.bookmarks.chain.RecipeChainInput;
import mezz.jei.gui.bookmarks.chain.RecipeChainMath;
import mezz.jei.gui.overlay.bookmarks.BookmarkAmountFormatter;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

public class BookmarkAmountFormatterTest {

	@Test
	public void testSmallNumberFormatting() {
		Assertions.assertEquals("0", BookmarkAmountFormatter.formatItemAmount(0));
		Assertions.assertEquals("42", BookmarkAmountFormatter.formatItemAmount(42));
		Assertions.assertEquals("999", BookmarkAmountFormatter.formatItemAmount(999));
	}

	@Test
	public void testCompactItemAmountsLong() {
		// K (1,000)
		Assertions.assertEquals("1.2K", BookmarkAmountFormatter.formatItemAmount(1_200));
		Assertions.assertEquals("12.3K", BookmarkAmountFormatter.formatItemAmount(12_345));
		Assertions.assertEquals("999K", BookmarkAmountFormatter.formatItemAmount(999_499));

		// M (1,000,000)
		Assertions.assertEquals("1.5M", BookmarkAmountFormatter.formatItemAmount(1_500_000));
		Assertions.assertEquals("25M", BookmarkAmountFormatter.formatItemAmount(25_000_000));

		// B (1,000,000,000)
		Assertions.assertEquals("2B", BookmarkAmountFormatter.formatItemAmount(2_000_000_000L));
		Assertions.assertEquals("100B", BookmarkAmountFormatter.formatItemAmount(100_000_000_000L));

		// T (1,000,000,000,000)
		Assertions.assertEquals("5T", BookmarkAmountFormatter.formatItemAmount(5_000_000_000_000L));

		// P (10^15)
		Assertions.assertEquals("3P", BookmarkAmountFormatter.formatItemAmount(3_000_000_000_000_000L));

		// E (10^18)
		Assertions.assertEquals("9.2E", BookmarkAmountFormatter.formatItemAmount(Long.MAX_VALUE));
	}

	@Test
	public void testBigIntegerFormatting() {
		// Exa
		BigInteger bigExa = new BigInteger("2500000000000000000"); // 2.5 * 10^18
		Assertions.assertEquals("2.5E", BookmarkAmountFormatter.formatItemAmount(bigExa));

		// Zetta (10^21)
		BigInteger bigZetta = new BigInteger("4200000000000000000000"); // 4.2 * 10^21
		Assertions.assertEquals("4.2Z", BookmarkAmountFormatter.formatItemAmount(bigZetta));

		// Yotta (10^24)
		BigInteger bigYotta = new BigInteger("7800000000000000000000000"); // 7.8 * 10^24
		Assertions.assertEquals("7.8Y", BookmarkAmountFormatter.formatItemAmount(bigYotta));

		// Super massive number (exponential scientific notation)
		BigInteger massive = new BigInteger("123456789012345678901234567890"); // ~1.2e29
		String formatted = BookmarkAmountFormatter.formatItemAmount(massive);
		Assertions.assertTrue(formatted.contains("e29"), "Expected exponential notation with exponent 29, got: " + formatted);
	}

	@Test
	public void testRecipeChainHugeFluidBigIntCalculationDoesNotCrash() {
		// Scenario: A recipe requiring 2 items of a fluid where each takes Long.MAX_VALUE
		// In old logic, Long.MAX_VALUE * 2 would overflow long, clamp to MAX_VALUE or wrap negatively
		BookmarkIngredientKey fluidKey = new BookmarkIngredientKey("fluid_stack", "minecraft:lava", null);
		ResourceLocation recipeUid = new ResourceLocation("test", "huge_lava_recipe");
		ResourceLocation rootRecipeUid = new ResourceLocation("test", "huge_consumer");

		// Ingredient that requires Long.MAX_VALUE of fluid per batch
		RecipeChainInput fluidIngredient = new RecipeChainInput(
			1,
			new BookmarkItemMetadata(
				1,
				BookmarkItemType.INGREDIENT,
				1,
				Long.MAX_VALUE,
				BookmarkItemMetadata.CHANCE_FULL,
				recipeUid,
				recipeUid,
				Set.of(fluidKey)
			)
		);

		// Result produced with multiplier of 5
		RecipeChainInput recipeResult = new RecipeChainInput(
			2,
			new BookmarkItemMetadata(
				1,
				BookmarkItemType.RESULT,
				5,
				1,
				BookmarkItemMetadata.CHANCE_FULL,
				recipeUid,
				recipeUid,
				Set.of(new BookmarkIngredientKey("minecraft:item", "minecraft:obsidian", null))
			)
		);

		var details = RecipeChainMath.refresh(List.of(fluidIngredient, recipeResult), Set.of());
		var item = details.calculatedItems().get(1);

		Assertions.assertNotNull(item);
		// With BigInteger support, bigCalculatedAmount should be Long.MAX_VALUE * 5
		BigInteger expectedAmount = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(5));
		Assertions.assertEquals(expectedAmount, item.bigCalculatedAmount());

		// And formatting it should not throw and output nicely formatted Exa units
		String formattedAmount = BookmarkAmountFormatter.formatTypedAmount(item.bigCalculatedAmount(), Set.of(fluidKey));
		Assertions.assertNotNull(formattedAmount);
		Assertions.assertTrue(formattedAmount.endsWith("B") || formattedAmount.endsWith("EB"), "Formatted: " + formattedAmount);
	}
	@Test
	public void testFluidBmdUnit() {
		// 1,000 buckets = 1,000,000 mB = 1 bmd
		Assertions.assertEquals("1bmd", BookmarkAmountFormatter.formatFluidAmount(1_000_000, 1_000));
		Assertions.assertEquals("1.5bmd", BookmarkAmountFormatter.formatFluidAmount(1_500_000, 1_000));
		Assertions.assertEquals("12bmd", BookmarkAmountFormatter.formatFluidAmount(12_000_000, 1_000));
		Assertions.assertEquals("999bmd", BookmarkAmountFormatter.formatFluidAmount(999_000_000, 1_000));
	}
}
