package mezz.jei.gui.tree;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import net.minecraft.resources.ResourceLocation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Aggregates statistics of a crafting tree across all nodes.
 * Completely BigInteger-based for unbounded quantities.
 */
public final class TreeStats {
	public enum Kind {
		INGREDIENT_NEEDED,
		INGREDIENT_AVAILABLE,
		CRAFT_NEEDED,
		CRAFT_AVAILABLE,
		RESULT,
		REMAINDER,
		HANDLER
	}

	public static final class Row {
		private final Kind kind;
		private final BookmarkIngredientKey key;
		private final Optional<ITypedIngredient<?>> typedIngredient;
		private final BigInteger amount;
		private final ResourceLocation recipeUid;
		private final BigInteger multiplier;
		private final ResourceLocation handlerUid;
		private final BigInteger iterations;
		private final long recipeCount;

		public Row(
			Kind kind,
			BookmarkIngredientKey key,
			Optional<ITypedIngredient<?>> typedIngredient,
			BigInteger amount,
			ResourceLocation recipeUid,
			BigInteger multiplier,
			ResourceLocation handlerUid,
			BigInteger iterations,
			long recipeCount
		) {
			this.kind = kind;
			this.key = key;
			this.typedIngredient = typedIngredient;
			this.amount = amount;
			this.recipeUid = recipeUid;
			this.multiplier = multiplier;
			this.handlerUid = handlerUid;
			this.iterations = iterations;
			this.recipeCount = recipeCount;
		}

		public Kind getKind() {
			return kind;
		}

		public BookmarkIngredientKey getKey() {
			return key;
		}

		public Optional<ITypedIngredient<?>> getTypedIngredient() {
			return typedIngredient;
		}

		public BigInteger getAmount() {
			return amount;
		}

		public ResourceLocation getRecipeUid() {
			return recipeUid;
		}

		public BigInteger getMultiplier() {
			return multiplier;
		}

		public ResourceLocation getHandlerUid() {
			return handlerUid;
		}

		public BigInteger getIterations() {
			return iterations;
		}

		public long getRecipeCount() {
			return recipeCount;
		}
	}

	private final Map<Kind, List<Row>> sections = new LinkedHashMap<>();

	public static TreeStats compute(TreeGraph graph) {
		TreeStats stats = new TreeStats();

		Map<BookmarkIngredientKey, BigInteger> ingredientNeeded = new LinkedHashMap<>();
		Map<BookmarkIngredientKey, Optional<ITypedIngredient<?>>> ingredientObjects = new HashMap<>();

		Map<BookmarkIngredientKey, BigInteger> ingredientAvailable = new LinkedHashMap<>();
		Map<ResourceLocation, BigInteger> craftNeeded = new LinkedHashMap<>();
		Map<ResourceLocation, BigInteger> craftAvailable = new LinkedHashMap<>();
		Map<ResourceLocation, BookmarkIngredientKey> craftOutputKey = new HashMap<>();

		Map<ResourceLocation, BigInteger> handlerIterations = new LinkedHashMap<>();
		Map<ResourceLocation, Long> handlerRecipeCount = new LinkedHashMap<>();

		for (TreeSlot slot : graph.getItems()) {
			BookmarkIngredientKey key = slot.getKey();
			ingredientObjects.putIfAbsent(key, slot.getTypedIngredient());

			if (!slot.hasRecipe()) {
				// Raw ingredient
				if (slot.getLeftAmount().signum() > 0) {
					ingredientNeeded.merge(key, slot.getLeftAmount(), BigInteger::add);
				}
				BigInteger inStock = slot.getRequestedAmount().subtract(slot.getLeftAmount());
				if (inStock.signum() > 0) {
					ingredientAvailable.merge(key, inStock, BigInteger::add);
				}
			} else {
				// Craft node
				ResourceLocation recipe = slot.getRecipeUid();
				craftOutputKey.putIfAbsent(recipe, key);

				if (slot.getMultiplier().signum() > 0) {
					craftNeeded.merge(recipe, slot.getMultiplier(), BigInteger::add);
				}
				BigInteger inStock = slot.getRequestedAmount().subtract(slot.getLeftAmount());
				if (inStock.signum() > 0) {
					craftAvailable.merge(recipe, inStock, BigInteger::add);
				}

				ResourceLocation category = slot.getRecipeTypeUid();
				if (category != null && slot.getMultiplier().signum() > 0) {
					handlerIterations.merge(category, slot.getMultiplier(), BigInteger::add);
					handlerRecipeCount.merge(category, 1L, Long::sum);
				}
			}
		}

		// Results
		List<Row> resultRows = new ArrayList<>();
		for (TreeSlot root : graph.getRoots()) {
			resultRows.add(new Row(
				Kind.RESULT,
				root.getKey(),
				root.getTypedIngredient(),
				root.getRequestedAmount(),
				root.getRecipeUid(),
				root.getMultiplier(),
				null,
				BigInteger.ZERO,
				0
			));
		}
		stats.sections.put(Kind.RESULT, resultRows);

		// Needed ingredients
		List<Row> neededRows = new ArrayList<>();
		ingredientNeeded.forEach((k, amt) -> neededRows.add(new Row(
			Kind.INGREDIENT_NEEDED,
			k,
			ingredientObjects.getOrDefault(k, Optional.empty()),
			amt,
			null,
			BigInteger.ZERO,
			null,
			BigInteger.ZERO,
			0
		)));
		stats.sections.put(Kind.INGREDIENT_NEEDED, neededRows);

		// Available ingredients
		List<Row> availableRows = new ArrayList<>();
		ingredientAvailable.forEach((k, amt) -> availableRows.add(new Row(
			Kind.INGREDIENT_AVAILABLE,
			k,
			ingredientObjects.getOrDefault(k, Optional.empty()),
			amt,
			null,
			BigInteger.ZERO,
			null,
			BigInteger.ZERO,
			0
		)));
		stats.sections.put(Kind.INGREDIENT_AVAILABLE, availableRows);

		// Crafts needed
		List<Row> craftsRows = new ArrayList<>();
		craftNeeded.forEach((rec, mult) -> craftsRows.add(new Row(
			Kind.CRAFT_NEEDED,
			craftOutputKey.get(rec),
			ingredientObjects.getOrDefault(craftOutputKey.get(rec), Optional.empty()),
			mult,
			rec,
			mult,
			null,
			BigInteger.ZERO,
			0
		)));
		stats.sections.put(Kind.CRAFT_NEEDED, craftsRows);

		// Machine/handler summary
		List<Row> handlerRows = new ArrayList<>();
		handlerIterations.forEach((cat, iters) -> handlerRows.add(new Row(
			Kind.HANDLER,
			null,
			Optional.empty(),
			iters,
			null,
			BigInteger.ZERO,
			cat,
			iters,
			handlerRecipeCount.getOrDefault(cat, 0L)
		)));
		stats.sections.put(Kind.HANDLER, handlerRows);

		return stats;
	}

	public List<Row> getSection(Kind kind) {
		return sections.getOrDefault(kind, Collections.emptyList());
	}

	public Map<Kind, List<Row>> getAllSections() {
		return Collections.unmodifiableMap(sections);
	}
}
