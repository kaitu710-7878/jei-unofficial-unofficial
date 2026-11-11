package mezz.jei.gui.tree;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.common.util.BigIntMath;
import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import mezz.jei.gui.bookmarks.BookmarkItemMetadata;
import mezz.jei.gui.bookmarks.BookmarkItemType;
import mezz.jei.gui.bookmarks.chain.RecipeChainInput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core crafting tree calculation engine with BigInteger precision.
 * Resolves recipe trees, handles stock allocation, manual quantity/recipe overrides,
 * and builds the TreeGraph.
 */
public final class TreeMath {
	private static final int MAX_DEPTH = 32;

	private final List<RecipeChainInput> rootInputs;
	private final Map<BookmarkIngredientKey, BigInteger> inventory;
	private final Map<BookmarkIngredientKey, BigInteger> allocatedInventory = new HashMap<>();

	// Node customization overrides preserved across recalculations
	private final Map<BookmarkIngredientKey, ResourceLocation> manualRecipeOverrides = new HashMap<>();
	private final Map<BookmarkIngredientKey, BigInteger> manualAmountOverrides = new HashMap<>();
	private final Set<BookmarkIngredientKey> forceCraftOverrides = new HashSet<>();
	private final Set<ResourceLocation> collapsedRecipes = new HashSet<>();

	private final TreeGraph graph = new TreeGraph();

	public TreeMath(List<RecipeChainInput> rootInputs, Map<BookmarkIngredientKey, BigInteger> inventory) {
		this.rootInputs = new ArrayList<>(rootInputs);
		this.inventory = new HashMap<>(inventory);
	}

	public TreeGraph getGraph() {
		return graph;
	}

	public void setManualRecipe(BookmarkIngredientKey key, @Nullable ResourceLocation recipe) {
		if (recipe != null) {
			manualRecipeOverrides.put(key, recipe);
		} else {
			manualRecipeOverrides.remove(key);
		}
	}

	public void setManualAmount(BookmarkIngredientKey key, @Nullable BigInteger amount) {
		if (amount != null && amount.signum() > 0) {
			manualAmountOverrides.put(key, amount);
		} else {
			manualAmountOverrides.remove(key);
		}
	}

	public void setForceCraft(BookmarkIngredientKey key, boolean force) {
		if (force) {
			forceCraftOverrides.add(key);
		} else {
			forceCraftOverrides.remove(key);
		}
	}

	public void toggleRecipeCollapsed(ResourceLocation recipeUid) {
		if (collapsedRecipes.contains(recipeUid)) {
			collapsedRecipes.remove(recipeUid);
		} else {
			collapsedRecipes.add(recipeUid);
		}
	}

	public Set<ResourceLocation> getCollapsedRecipes() {
		return Collections.unmodifiableSet(collapsedRecipes);
	}

	public void setCollapsedRecipes(Set<ResourceLocation> collapsed) {
		this.collapsedRecipes.clear();
		this.collapsedRecipes.addAll(collapsed);
	}

	/**
	 * Executes full tree calculation and builds the TreeGraph.
	 */
	public TreeGraph calculate() {
		graph.clear();
		allocatedInventory.clear();

		// 1. Identify all ingredient keys consumed as inputs by any recipe in rootInputs
		Set<BookmarkIngredientKey> consumedKeys = new HashSet<>();
		for (RecipeChainInput input : rootInputs) {
			BookmarkItemMetadata meta = input.metadata();
			if (meta.type().isGraphInput() || meta.type() == BookmarkItemType.INGREDIENT) {
				BookmarkIngredientKey inKey = input.selectedKey();
				if (inKey == null && !meta.permutations().isEmpty()) {
					inKey = meta.permutations().iterator().next();
				}
				if (inKey != null) {
					consumedKeys.add(inKey);
				}
			}
		}

		// 2. Filter output items as roots (only those that are NOT consumed by any parent recipe)
		List<RecipeChainInput> roots = new ArrayList<>();
		for (RecipeChainInput input : rootInputs) {
			BookmarkItemMetadata meta = input.metadata();
			if (meta.type().isGraphOutput() || meta.type() == BookmarkItemType.ITEM) {
				BookmarkIngredientKey key = input.selectedKey();
				if (key == null && !meta.permutations().isEmpty()) {
					key = meta.permutations().iterator().next();
				}
				if (key != null && !consumedKeys.contains(key)) {
					roots.add(input);
				}
			}
		}

		// Fallback: if all outputs are consumed (cyclic) or none found, pick the first output or first input
		if (roots.isEmpty() && !rootInputs.isEmpty()) {
			for (RecipeChainInput input : rootInputs) {
				if (input.metadata().type().isGraphOutput() || input.metadata().type() == BookmarkItemType.ITEM) {
					roots.add(input);
					break;
				}
			}
			if (roots.isEmpty()) {
				roots.add(rootInputs.get(0));
			}
		}

		Set<ResourceLocation> visitedRecipes = new HashSet<>();

		for (RecipeChainInput rootInput : roots) {
			BookmarkIngredientKey key = rootInput.selectedKey();
			if (key == null && !rootInput.metadata().permutations().isEmpty()) {
				key = rootInput.metadata().permutations().iterator().next();
			}
			if (key == null) {
				continue;
			}

			BigInteger baseAmount = manualAmountOverrides.getOrDefault(key, rootInput.metadata().bigAmount());
			if (baseAmount.signum() <= 0) {
				baseAmount = BigInteger.ONE;
			}

			TreeSlot rootSlot = buildNode(
				key,
				rootInput.selectedIngredient() != null ? rootInput.selectedIngredient() : key.typedIngredient(),
				rootInput.metadata().recipeUid(),
				rootInput.metadata().recipeTypeUid(),
				BigInteger.valueOf(rootInput.metadata().multiplier()).max(BigInteger.ONE),
				BigInteger.ZERO,
				baseAmount,
				visitedRecipes,
				0
			);

			graph.addRoot(rootSlot);
		}

		graph.updateCaches();
		return graph;
	}

	private TreeSlot buildNode(
		BookmarkIngredientKey key,
		@Nullable ITypedIngredient<?> typedIngredient,
		@Nullable ResourceLocation defaultRecipeUid,
		@Nullable ResourceLocation defaultRecipeType,
		BigInteger baseOutputAmount,
		BigInteger inheritAmount,
		BigInteger requestedAmount,
		Set<ResourceLocation> visitedRecipes,
		int depth
	) {
		boolean force = forceCraftOverrides.contains(key);
		BigInteger totalDemand = inheritAmount.add(requestedAmount);

		// Determine stock allocation
		BigInteger inStock = inventory.getOrDefault(key, BigInteger.ZERO);
		BigInteger alreadyAllocated = allocatedInventory.getOrDefault(key, BigInteger.ZERO);
		BigInteger availableStock = inStock.subtract(alreadyAllocated).max(BigInteger.ZERO);

		BigInteger usedStock = BigInteger.ZERO;
		BigInteger remainingToCraft = totalDemand;

		if (!force && availableStock.signum() > 0) {
			usedStock = availableStock.min(totalDemand);
			allocatedInventory.put(key, alreadyAllocated.add(usedStock));
			remainingToCraft = totalDemand.subtract(usedStock);
		}

		ResourceLocation chosenRecipe = manualRecipeOverrides.getOrDefault(key, defaultRecipeUid);
		ResourceLocation chosenType = defaultRecipeType;

		BigInteger multiplier = BigInteger.ZERO;
		BigInteger producedAmount = BigInteger.ZERO;

		if (remainingToCraft.signum() > 0 && chosenRecipe != null) {
			// Calculate craft multiplier
			multiplier = BigIntMath.divideRoundUp(remainingToCraft, baseOutputAmount);
			producedAmount = multiplier.multiply(baseOutputAmount);
		}

		TreeSlot node = new TreeSlot(
			key,
			typedIngredient,
			chosenRecipe,
			chosenType,
			baseOutputAmount,
			inheritAmount,
			requestedAmount,
			producedAmount,
			BigInteger.ZERO,
			remainingToCraft,
			multiplier,
			false
		);

		node.setForceCraft(force);
		node.setManualAmountOverride(manualAmountOverrides.get(key));
		node.setManualRecipeOverride(manualRecipeOverrides.get(key));
		if (chosenRecipe != null && collapsedRecipes.contains(chosenRecipe)) {
			node.setCollapsed(true);
		}

		// Recursively resolve children ingredients if recipe exists and within depth limit
		if (chosenRecipe != null && depth < MAX_DEPTH && !visitedRecipes.contains(chosenRecipe)) {
			visitedRecipes.add(chosenRecipe);

			List<RecipeChildIngredient> subIngredients = findRecipeInputs(chosenRecipe, key, typedIngredient);
			for (RecipeChildIngredient sub : subIngredients) {
				BigInteger subRequested = sub.amount().multiply(multiplier);

				TreeSlot childSlot = buildNode(
					sub.key(),
					sub.typedIngredient(),
					sub.defaultRecipeUid(),
					sub.recipeTypeUid(),
					sub.outputAmount(),
					BigInteger.ZERO,
					subRequested,
					visitedRecipes,
					depth + 1
				);

				graph.addChild(node, childSlot);
			}

			visitedRecipes.remove(chosenRecipe);
		}

		return node;
	}

	private record RecipeChildIngredient(
		BookmarkIngredientKey key,
		@Nullable ITypedIngredient<?> typedIngredient,
		BigInteger amount,
		BigInteger outputAmount,
		@Nullable ResourceLocation defaultRecipeUid,
		@Nullable ResourceLocation recipeTypeUid
	) {}

	private List<RecipeChildIngredient> findRecipeInputs(
		ResourceLocation recipeUid,
		BookmarkIngredientKey parentKey,
		@Nullable ITypedIngredient<?> parentIngredient
	) {
		List<RecipeChildIngredient> results = new ArrayList<>();

		// Check if inputs are available from rootInputs metadata
		for (RecipeChainInput input : rootInputs) {
			BookmarkItemMetadata meta = input.metadata();
			if (recipeUid.equals(meta.recipeUid()) && meta.type().isGraphInput()) {
				BookmarkIngredientKey inKey = input.selectedKey();
				if (inKey == null && !meta.permutations().isEmpty()) {
					inKey = meta.permutations().iterator().next();
				}
				if (inKey != null) {
					results.add(new RecipeChildIngredient(
						inKey,
						input.selectedIngredient() != null ? input.selectedIngredient() : inKey.typedIngredient(),
						meta.bigAmount().max(BigInteger.ONE),
						BigInteger.ONE,
						manualRecipeOverrides.get(inKey),
						meta.recipeTypeUid()
					));
				}
			}
		}

		if (results.isEmpty()) {
			ITypedIngredient<?> matchTarget = parentIngredient != null ? parentIngredient : parentKey.typedIngredient();
			TreeRecipeBuilder.RecipeInfo<?> info = TreeRecipeBuilder.findRecipe(recipeUid, matchTarget);
			if (info != null) {
				Map<BookmarkIngredientKey, RecipeChildIngredient> aggregated = new java.util.LinkedHashMap<>();
				for (mezz.jei.api.ingredients.ITypedIngredient<?> inIng : info.inputs()) {
					BookmarkIngredientKey inKey = mezz.jei.gui.bookmarks.BookmarkItemMetadataFactory.createPermutationKey(inIng, mezz.jei.common.Internal.getJeiRuntime().getIngredientManager());
					RecipeChildIngredient existing = aggregated.get(inKey);
					BigInteger count = BigInteger.ONE;
					if (inIng.getItemStack().isPresent()) {
						count = BigInteger.valueOf(Math.max(1, inIng.getItemStack().get().getCount()));
					}
					if (existing != null) {
						aggregated.put(inKey, new RecipeChildIngredient(
							inKey,
							existing.typedIngredient(),
							existing.amount().add(count),
							existing.outputAmount(),
							existing.defaultRecipeUid(),
							existing.recipeTypeUid()
						));
					} else {
						aggregated.put(inKey, new RecipeChildIngredient(
							inKey,
							inIng,
							count,
							BigInteger.valueOf(info.outputFactor()),
							manualRecipeOverrides.get(inKey),
							info.categoryUid()
						));
					}
				}
				results.addAll(aggregated.values());
			}
		}

		return results;
	}
}
