package mezz.jei.gui.tree;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.common.Internal;
import mezz.jei.common.util.BigIntMath;
import mezz.jei.gui.bookmarks.BookmarkIngredientAmountResolver;
import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import mezz.jei.gui.bookmarks.BookmarkItemMetadata;
import mezz.jei.gui.bookmarks.BookmarkItemMetadataFactory;
import mezz.jei.gui.bookmarks.BookmarkItemType;
import mezz.jei.gui.bookmarks.chain.RecipeChainInput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Core calculation engine for the crafting tree.
 * Resolves recipes, inventory deductions, craft multipliers, and recursive dependencies
 * using arbitrary-precision BigInteger arithmetic.
 */
public final class TreeMath {
	private static final int MAX_DEPTH = 48;

	private final List<RecipeChainInput> rootInputs;
	private final Map<BookmarkIngredientKey, BigInteger> inventory;
	private final Map<BookmarkIngredientKey, BigInteger> allocatedInventory = new HashMap<>();

	// Node customization overrides preserved across recalculations
	private final Map<BookmarkIngredientKey, ResourceLocation> manualRecipeOverrides = new HashMap<>();
	private final Map<BookmarkIngredientKey, List<ITypedIngredient<?>>> customRecipeInputs = new HashMap<>();
	private final Map<BookmarkIngredientKey, BigInteger> manualAmountOverrides = new HashMap<>();
	private final Set<BookmarkIngredientKey> forceCraftOverrides = new HashSet<>();
	private final Set<ResourceLocation> collapsedRecipes = new HashSet<>();
	private final Set<BookmarkIngredientKey> uncraftedKeys = new HashSet<>();
	private final Set<String> prunedPaths = new HashSet<>();

	private final Set<BookmarkIngredientKey> prunedKeys;
	private final TreeGraph graph = new TreeGraph();

	public TreeMath(List<RecipeChainInput> rootInputs, Map<BookmarkIngredientKey, BigInteger> inventory) {
		this(rootInputs, inventory, Set.of());
	}

	public TreeMath(List<RecipeChainInput> rootInputs, Map<BookmarkIngredientKey, BigInteger> inventory, Set<BookmarkIngredientKey> prunedKeys) {
		this.rootInputs = new ArrayList<>(rootInputs);
		this.inventory = new HashMap<>(inventory);
		this.prunedKeys = new HashSet<>(prunedKeys);
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

	public void setCustomRecipeInputs(BookmarkIngredientKey key, @Nullable List<ITypedIngredient<?>> inputs) {
		if (inputs != null && !inputs.isEmpty()) {
			customRecipeInputs.put(key, new ArrayList<>(inputs));
		} else {
			customRecipeInputs.remove(key);
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

	public void setUncrafted(BookmarkIngredientKey key, boolean uncrafted) {
		if (uncrafted) {
			uncraftedKeys.add(key);
			manualRecipeOverrides.remove(key);
			customRecipeInputs.remove(key);
		} else {
			uncraftedKeys.remove(key);
		}
	}

	public boolean isUncrafted(BookmarkIngredientKey key) {
		return uncraftedKeys.contains(key);
	}

	public void setPrunedPaths(Set<String> paths) {
		this.prunedPaths.clear();
		this.prunedPaths.addAll(paths);
	}

	public Set<String> getPrunedPaths() {
		return Collections.unmodifiableSet(prunedPaths);
	}

	public boolean isPathPruned(String path) {
		if (prunedPaths.contains(path)) {
			return true;
		}
		for (String pruned : prunedPaths) {
			if (path.startsWith(pruned + "/")) {
				return true;
			}
		}
		return false;
	}

	private static boolean isNonConsumable(BookmarkIngredientKey key) {
		String uid = key.ingredientUid().toLowerCase(Locale.ROOT);
		return uid.contains("press")
			|| uid.contains("mold")
			|| uid.contains("cast")
			|| uid.contains("die")
			|| uid.contains("template")
			|| uid.contains("lens")
			|| uid.contains("catalyst")
			|| uid.contains("programmed_circuit");
	}

	/**
	 * Performs a full tree recalculation and returns the populated TreeGraph.
	 */
	public TreeGraph calculate() {
		graph.clear();
		allocatedInventory.clear();

		// 1. Scan which items are consumed as inputs by ANY recipe in this tree
		Set<BookmarkIngredientKey> consumedKeys = new HashSet<>();
		for (RecipeChainInput input : rootInputs) {
			BookmarkItemMetadata meta = input.metadata();
			if (meta.type().isGraphInput() || meta.type() == BookmarkItemType.INGREDIENT) {
				BookmarkIngredientKey inKey = input.selectedKey();
				if (inKey == null && !meta.permutations().isEmpty()) {
					inKey = meta.permutations().iterator().next();
				}
				if (inKey != null && !prunedKeys.contains(inKey)) {
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
				if (key != null && !consumedKeys.contains(key) && !prunedKeys.contains(key)) {
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

		int rootIndex = 0;
		for (RecipeChainInput rootInput : roots) {
			BookmarkIngredientKey key = rootInput.selectedKey();
			if (key == null && !rootInput.metadata().permutations().isEmpty()) {
				key = rootInput.metadata().permutations().iterator().next();
			}
			if (key == null || prunedKeys.contains(key)) {
				continue;
			}

			String rootPath = String.valueOf(rootIndex++);
			if (isPathPruned(rootPath)) {
				continue;
			}

			BigInteger baseAmount = manualAmountOverrides.getOrDefault(key, rootInput.metadata().bigAmount());
			if (baseAmount.signum() <= 0) {
				baseAmount = BigInteger.ONE;
			}

			long initialOutputFactor = rootInput.metadata().factor() > 0 ? rootInput.metadata().factor() : 1;

			TreeSlot rootSlot = buildNode(
				key,
				rootInput.selectedIngredient() != null ? rootInput.selectedIngredient() : key.typedIngredient(),
				rootInput.metadata().recipeUid(),
				rootInput.metadata().recipeTypeUid(),
				BigInteger.valueOf(initialOutputFactor),
				BigInteger.ZERO,
				baseAmount,
				visitedRecipes,
				0,
				rootPath
			);

			if (rootSlot != null) {
				graph.addRoot(rootSlot);
			}
		}

		graph.updateCaches();
		return graph;
	}

	private @Nullable TreeSlot buildNode(
		BookmarkIngredientKey key,
		@Nullable ITypedIngredient<?> typedIngredient,
		@Nullable ResourceLocation defaultRecipeUid,
		@Nullable ResourceLocation defaultRecipeType,
		BigInteger baseOutputAmount,
		BigInteger inheritAmount,
		BigInteger requestedAmount,
		Set<ResourceLocation> visitedRecipes,
		int depth,
		String nodePath
	) {
		if (isPathPruned(nodePath) || prunedKeys.contains(key)) {
			return null;
		}
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

		ResourceLocation chosenRecipe = null;
		if (!uncraftedKeys.contains(key)) {
			chosenRecipe = manualRecipeOverrides.getOrDefault(key, defaultRecipeUid);
		}
		ResourceLocation chosenType = defaultRecipeType;

		// Dynamically resolve actual output factor from chosen recipe for this node!
		BigInteger effectiveOutputFactor = baseOutputAmount.max(BigInteger.ONE);
		if (chosenRecipe != null) {
			ITypedIngredient<?> matchTarget = typedIngredient != null ? typedIngredient : key.typedIngredient();
			TreeRecipeBuilder.RecipeInfo<?> info = TreeRecipeBuilder.findRecipe(chosenRecipe, matchTarget);
			if (info != null && info.outputFactor() > 0) {
				effectiveOutputFactor = BigInteger.valueOf(info.outputFactor());
				if (chosenType == null) {
					chosenType = info.categoryUid();
				}
			}
		}

		BigInteger multiplier = BigInteger.ZERO;
		BigInteger producedAmount = BigInteger.ZERO;

		if (remainingToCraft.signum() > 0 && chosenRecipe != null) {
			// Calculate craft multiplier based on this node's actual recipe output factor
			multiplier = BigIntMath.divideRoundUp(remainingToCraft, effectiveOutputFactor);
			producedAmount = multiplier.multiply(effectiveOutputFactor);
		}

		TreeSlot node = new TreeSlot(
			key,
			typedIngredient,
			chosenRecipe,
			chosenType,
			effectiveOutputFactor,
			inheritAmount,
			requestedAmount,
			producedAmount,
			usedStock,
			remainingToCraft,
			multiplier,
			false
		);

		node.setForceCraft(force);
		node.setManualAmountOverride(manualAmountOverrides.get(key));
		node.setManualRecipeOverride(manualRecipeOverrides.get(key));
		node.setNodePath(nodePath);
		if (chosenRecipe != null && collapsedRecipes.contains(chosenRecipe)) {
			node.setCollapsed(true);
		}

		// Recursively resolve children ingredients if recipe exists and within depth limit
		if (chosenRecipe != null && depth < MAX_DEPTH && !visitedRecipes.contains(chosenRecipe)) {
			visitedRecipes.add(chosenRecipe);

			List<RecipeChildIngredient> subIngredients = findRecipeInputs(chosenRecipe, key, typedIngredient);
			int childIdx = 0;
			for (RecipeChildIngredient sub : subIngredients) {
				String childPath = nodePath + "/" + sub.key().ingredientUid();
				String legacyIndexedPath = nodePath + "/" + childIdx++ + ":" + sub.key().ingredientUid();
				if (isPathPruned(childPath) || isPathPruned(legacyIndexedPath) || prunedKeys.contains(sub.key())) {
					continue;
				}
				// Non-consumable items (molds, circuits) stay fixed at base amount without multiplier
				BigInteger subRequested = isNonConsumable(sub.key()) ? sub.amount() : sub.amount().multiply(multiplier);

				TreeSlot childSlot = buildNode(
					sub.key(),
					sub.typedIngredient(),
					sub.defaultRecipeUid(),
					sub.recipeTypeUid(),
					BigInteger.ONE,
					BigInteger.ZERO,
					subRequested,
					visitedRecipes,
					depth + 1,
					childPath
				);

				if (childSlot != null) {
					graph.addChild(node, childSlot);
				}
			}

			visitedRecipes.remove(chosenRecipe);
		}

		return node;
	}

	private record RecipeChildIngredient(
		BookmarkIngredientKey key,
		@Nullable ITypedIngredient<?> typedIngredient,
		BigInteger amount,
		@Nullable ResourceLocation defaultRecipeUid,
		@Nullable ResourceLocation recipeTypeUid
	) {}

	private List<RecipeChildIngredient> findRecipeInputs(
		ResourceLocation recipeUid,
		BookmarkIngredientKey parentKey,
		@Nullable ITypedIngredient<?> parentIngredient
	) {
		List<RecipeChildIngredient> results = new ArrayList<>();

		// If manual recipe override or custom inputs were explicitly assigned for this parent node,
		// prioritize live recipes / custom inputs over stale rootInputs from the initial bookmark chain!
		boolean hasManualOverride = manualRecipeOverrides.containsKey(parentKey);
		boolean hasCustomInputs = customRecipeInputs.containsKey(parentKey);

		if (!hasManualOverride && !hasCustomInputs) {
			Map<BookmarkIngredientKey, RecipeChildIngredient> aggregated = new LinkedHashMap<>();
			for (RecipeChainInput input : rootInputs) {
				BookmarkItemMetadata meta = input.metadata();
				if (recipeUid.equals(meta.recipeUid()) && (meta.type().isGraphInput() || meta.type() == BookmarkItemType.NONCONSUMABLE || meta.type() == BookmarkItemType.INGREDIENT)) {
					BookmarkIngredientKey inKey = input.selectedKey();
					if (inKey == null && !meta.permutations().isEmpty()) {
						inKey = meta.permutations().iterator().next();
					}
					if (inKey != null && !prunedKeys.contains(inKey)) {
						BigInteger count = meta.bigAmount().max(BigInteger.ONE);
						RecipeChildIngredient existing = aggregated.get(inKey);
						if (existing != null) {
							aggregated.put(inKey, new RecipeChildIngredient(
								inKey,
								existing.typedIngredient(),
								existing.amount().add(count),
								existing.defaultRecipeUid(),
								existing.recipeTypeUid()
							));
						} else {
							aggregated.put(inKey, new RecipeChildIngredient(
								inKey,
								input.selectedIngredient() != null ? input.selectedIngredient() : inKey.typedIngredient(),
								count,
								manualRecipeOverrides.get(inKey),
								meta.recipeTypeUid()
							));
						}
					}
				}
			}
			results.addAll(aggregated.values());
		}

		if (results.isEmpty()) {
			ITypedIngredient<?> matchTarget = parentIngredient != null ? parentIngredient : parentKey.typedIngredient();
			TreeRecipeBuilder.RecipeInfo<?> info = TreeRecipeBuilder.findRecipe(recipeUid, matchTarget);
			List<ITypedIngredient<?>> effectiveInputs = customRecipeInputs.get(parentKey);
			if (effectiveInputs == null && info != null) {
				effectiveInputs = info.inputs();
			}
			if (effectiveInputs != null && !effectiveInputs.isEmpty()) {
				Map<BookmarkIngredientKey, RecipeChildIngredient> aggregated = new LinkedHashMap<>();
				ResourceLocation catUid = info != null ? info.categoryUid() : null;
				for (ITypedIngredient<?> inIng : effectiveInputs) {
					BookmarkIngredientKey inKey = BookmarkItemMetadataFactory.createPermutationKey(inIng, Internal.getJeiRuntime().getIngredientManager());
					if (prunedKeys.contains(inKey)) {
						continue;
					}
					RecipeChildIngredient existing = aggregated.get(inKey);
					BigInteger count = BigInteger.valueOf(Math.max(1L, BookmarkIngredientAmountResolver.getAmount(inIng, Internal.getJeiRuntime().getIngredientManager())));
					if (existing != null) {
						aggregated.put(inKey, new RecipeChildIngredient(
							inKey,
							existing.typedIngredient(),
							existing.amount().add(count),
							existing.defaultRecipeUid(),
							existing.recipeTypeUid()
						));
					} else {
						aggregated.put(inKey, new RecipeChildIngredient(
							inKey,
							inIng,
							count,
							manualRecipeOverrides.get(inKey),
							catUid
						));
					}
				}
				results.addAll(aggregated.values());
			} else {
				// Last-resort fallback: scan rootInputs regardless of manual override flag
				for (RecipeChainInput input : rootInputs) {
					BookmarkItemMetadata meta = input.metadata();
					if (recipeUid.equals(meta.recipeUid()) && (meta.type().isGraphInput() || meta.type() == BookmarkItemType.NONCONSUMABLE || meta.type() == BookmarkItemType.INGREDIENT)) {
						BookmarkIngredientKey inKey = input.selectedKey();
						if (inKey == null && !meta.permutations().isEmpty()) {
							inKey = meta.permutations().iterator().next();
						}
						if (inKey != null && !prunedKeys.contains(inKey)) {
							results.add(new RecipeChildIngredient(
								inKey,
								input.selectedIngredient() != null ? input.selectedIngredient() : inKey.typedIngredient(),
								meta.bigAmount().max(BigInteger.ONE),
								manualRecipeOverrides.get(inKey),
								meta.recipeTypeUid()
							));
						}
					}
				}
			}
		}

		// Populate default recipes for child ingredients if they appear as outputs in rootInputs
		List<RecipeChildIngredient> resolvedResults = new ArrayList<>(results.size());
		for (RecipeChildIngredient child : results) {
			ResourceLocation defRecipe = child.defaultRecipeUid();
			ResourceLocation defType = child.recipeTypeUid();
			if (defRecipe == null) {
				for (RecipeChainInput input : rootInputs) {
					BookmarkItemMetadata meta = input.metadata();
					if (meta.type().isGraphOutput() && meta.recipeUid() != null) {
						BookmarkIngredientKey outKey = input.selectedKey();
						if (outKey == null && !meta.permutations().isEmpty()) {
							outKey = meta.permutations().iterator().next();
						}
						if (child.key().equals(outKey)) {
							defRecipe = meta.recipeUid();
							defType = meta.recipeTypeUid();
							break;
						}
					}
				}
			}
			resolvedResults.add(new RecipeChildIngredient(
				child.key(),
				child.typedIngredient(),
				child.amount(),
				defRecipe,
				defType
			));
		}

		return resolvedResults;
	}
}
