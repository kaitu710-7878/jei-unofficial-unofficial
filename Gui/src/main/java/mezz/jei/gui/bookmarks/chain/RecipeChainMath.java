/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * Portions of this file are adapted from GTNewHorizons NotEnoughItems:
 * https://github.com/GTNewHorizons/NotEnoughItems
 *
 * GTNH NEI modifications copyright (c) 2019-2024 mitchej123 and the GTNH Team.
 */
package mezz.jei.gui.bookmarks.chain;

import mezz.jei.common.util.BigIntMath;
import mezz.jei.common.util.SaturatedMath;
import mezz.jei.gui.bookmarks.BookmarkGroupManager;
import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import mezz.jei.gui.bookmarks.BookmarkItemMetadata;
import mezz.jei.gui.bookmarks.BookmarkItemType;
import net.minecraft.resources.ResourceLocation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecipeChainMath {
	public static final ResourceLocation ROOT_RECIPE_UID = new ResourceLocation("jei", "recipe-autocrafting");
	private static final ResourceLocation ROOT_RECIPE_TYPE_UID = new ResourceLocation("jei", "autocrafting");
	private static final BookmarkIngredientKey ROOT_KEY = new BookmarkIngredientKey("minecraft:item", "minecraft:fire", null);

	private final List<RecipeChainInput> initialItems = new ArrayList<>();
	private final List<RecipeChainInput> recipeIngredients = new ArrayList<>();
	private final List<RecipeChainInput> recipeResults = new ArrayList<>();
	private final Set<ResourceLocation> collapsedRecipes;
	private RecipeChainGraph graph;
	private RecipeChainPlan plan;

	private final Map<ResourceLocation, BigInteger> outputRecipes = new LinkedHashMap<>();
	private final Map<ResourceLocation, RecipeChainInput> outputTargets = new LinkedHashMap<>();
	private final Map<RecipeChainInput, RecipeChainInput> preferredItems = new LinkedHashMap<>();
	private final Map<RecipeChainInput, BigInteger> requiredAmount = new HashMap<>();
	private final Map<RecipeChainInput, BigInteger> workingMultipliers = new HashMap<>();
	private final Map<BookmarkIngredientKey, BigInteger> containerItems = new LinkedHashMap<>();
	private final Map<BookmarkIngredientKey, List<RecipeChainRemainderStack>> reusableContainerItems = new LinkedHashMap<>();
	private final Set<BookmarkIngredientKey> containerItemsBlacklist = new LinkedHashSet<>();
	private int nextSyntheticIndex = -1;
	private @org.jetbrains.annotations.Nullable IDemandVisitor demandVisitor;

	/** Returns the visitor for this demand's children, or null to skip recording that subtree. */
	@FunctionalInterface
	public interface IDemandVisitor {
		@org.jetbrains.annotations.Nullable
		IDemandVisitor visit(RecipeChainInput input, long requested, long remaining);

		default @org.jetbrains.annotations.Nullable IDemandVisitor visitBig(RecipeChainInput input, BigInteger requested, BigInteger remaining) {
			return visit(input, BigIntMath.toLongSaturated(requested), BigIntMath.toLongSaturated(remaining));
		}
	}

	public static void visitDemands(List<RecipeChainInput> inputs, Set<ResourceLocation> collapsedRecipes, IDemandVisitor visitor) {
		var math = new RecipeChainMath(inputs, collapsedRecipes);
		math.demandVisitor = visitor;
		math.refresh();
	}

	private RecipeChainMath(List<RecipeChainInput> inputs, Set<ResourceLocation> collapsedRecipes) {
		this.collapsedRecipes = Set.copyOf(collapsedRecipes);
		for (RecipeChainInput input : inputs) {
			BookmarkItemMetadata metadata = input.metadata();
			ResourceLocation recipeUid = metadata.recipeUid();
			BookmarkItemType type = metadata.type();
			if (recipeUid == null || !type.isRecipeAssociated()) {
				initialItems.add(input);
			} else if (type.isGraphInput()) {
				recipeIngredients.add(input);
				workingMultipliers.put(input, BigIntMath.ZERO);
			} else if (type.isGraphOutput()) {
				recipeResults.add(input);
				workingMultipliers.put(input, BigIntMath.ZERO);
				outputTargets.putIfAbsent(recipeUid, input);
			}
		}
		rebuildPlan();
		outputTargets.clear();
		outputTargets.putAll(plan.outputTargets());
		for (Map.Entry<ResourceLocation, Long> entry : plan.outputRecipes().entrySet()) {
			outputRecipes.put(entry.getKey(), BigIntMath.valueOf(entry.getValue()));
		}
	}

	public static RecipeChainDetails refresh(List<RecipeChainInput> inputs, Set<ResourceLocation> collapsedRecipes) {
		return new RecipeChainMath(inputs, collapsedRecipes).refresh();
	}

	public static RecipeChainMath of(List<RecipeChainInput> inputs, Set<ResourceLocation> collapsedRecipes) {
		return new RecipeChainMath(inputs, collapsedRecipes);
	}

	public ResourceLocation createMasterRoot() {
		if (hasMasterRoot()) {
			return ROOT_RECIPE_UID;
		}

		List<RecipeChainInput> rootIngredients = new ArrayList<>();
		for (RecipeChainInput result : recipeResults) {
			BookmarkItemMetadata metadata = result.metadata();
			ResourceLocation recipeUid = metadata.recipeUid();
			if (recipeUid != null && outputRecipes.containsKey(recipeUid) && isOutputTarget(result)) {
				BigInteger multiplier = outputRecipes.get(recipeUid);
				rootIngredients.add(new RecipeChainInput(
					nextSyntheticIndex--,
					new BookmarkItemMetadata(
						metadata.groupId(),
						BookmarkItemType.INGREDIENT,
						1,
						metadata.amount(BigIntMath.toLongSaturated(multiplier)),
						BookmarkItemMetadata.CHANCE_FULL,
						metadata.recipeTypeUid(),
						ROOT_RECIPE_UID,
						metadata.permutations()
					)
				));
				containerItemsBlacklist.addAll(metadata.permutations());
			}
		}

		outputRecipes.clear();
		outputRecipes.put(ROOT_RECIPE_UID, BigIntMath.ONE);
		outputTargets.clear();
		recipeResults.removeIf(result -> ROOT_RECIPE_UID.equals(result.metadata().recipeUid()));
		RecipeChainInput rootResult = new RecipeChainInput(
			nextSyntheticIndex--,
			new BookmarkItemMetadata(
				BookmarkGroupManager.DEFAULT_GROUP_ID,
				BookmarkItemType.RESULT,
				1,
				1,
				BookmarkItemMetadata.CHANCE_FULL,
				ROOT_RECIPE_TYPE_UID,
				ROOT_RECIPE_UID,
				Set.of(ROOT_KEY)
			)
		);
		recipeResults.add(rootResult);
		recipeIngredients.addAll(rootIngredients);
		rebuildPlan();
		outputTargets.putAll(plan.outputTargets());
		return ROOT_RECIPE_UID;
	}

	public void expandRootDemandForCraftAll(List<RecipeChainInput> inventory) {
		ResourceLocation rootRecipeUid = createMasterRoot();
		for (int i = 0; i < recipeIngredients.size(); i++) {
			RecipeChainInput ingredient = recipeIngredients.get(i);
			BookmarkItemMetadata metadata = ingredient.metadata();
			BigInteger requiredAmount = metadata.bigAmount();
			if (requiredAmount.signum() <= 0 || !rootRecipeUid.equals(metadata.recipeUid())) {
				continue;
			}

			BigInteger availableAmount = BigIntMath.ZERO;
			for (RecipeChainInput available : inventory) {
				BookmarkItemMetadata availableMetadata = available.metadata();
				if (metadata.isSatisfiedBy(availableMetadata)) {
					availableAmount = BigIntMath.add(availableAmount, availableMetadata.bigAmount());
				}
			}

			if (availableAmount.compareTo(requiredAmount) >= 0) {
				BigInteger remainder = availableAmount.remainder(requiredAmount);
				BigInteger expandedAmount = BigIntMath.add(availableAmount, requiredAmount.subtract(remainder));
				recipeIngredients.set(i, new RecipeChainInput(
					ingredient.index(),
					new BookmarkItemMetadata(
						metadata.groupId(),
						metadata.type(),
						1,
						BigIntMath.toLongSaturated(expandedAmount),
						BookmarkItemMetadata.CHANCE_FULL,
						metadata.recipeTypeUid(),
						metadata.recipeUid(),
						metadata.permutations(),
						metadata.containerItem(),
						metadata.containerItemCraftingUses(),
						metadata.brokenContainerItem()
					)
				));
			}
		}
		rebuildPlan();
	}

	public boolean hasMasterRoot() {
		return outputRecipes.containsKey(ROOT_RECIPE_UID);
	}

	public RecipeChainDetails refreshDetails() {
		return refresh();
	}

	void replaceInitialItems(List<RecipeChainInput> inputs) {
		initialItems.clear();
		initialItems.addAll(inputs);
	}

	Set<ResourceLocation> outputRecipesSnapshot() {
		return Set.copyOf(outputRecipes.keySet());
	}

	Map<RecipeChainInput, RecipeChainInput> preferredItemsSnapshot() {
		return Map.copyOf(preferredItems);
	}

	private RecipeChainDetails refresh() {
		resetCalculation();
		for (Map.Entry<ResourceLocation, BigInteger> outputRecipe : outputRecipes.entrySet()) {
			ResourceLocation recipeUid = outputRecipe.getKey();
			BigInteger multiplier = outputRecipe.getValue();
			for (RecipeChainInput result : graph.resultsFor(recipeUid)) {
				if (!result.metadata().emptyFactor() && isOutputTarget(result) && result.metadata().equalsRecipe(recipeUid, result.metadata().groupId())) {
					BigInteger resultAmount = result.metadata().bigAmount(multiplier);
					preferredItems.put(result, result);
					calculateSuitableRecipe(result, resultAmount, new ArrayList<>());
					preferredItems.remove(result);
				}
			}
		}

		for (Map.Entry<ResourceLocation, BigInteger> outputRecipe : outputRecipes.entrySet()) {
			ResourceLocation recipeUid = outputRecipe.getKey();
			BigInteger multiplier = outputRecipe.getValue();
			for (RecipeChainInput result : graph.resultsFor(recipeUid)) {
				if (!result.metadata().emptyFactor() && isOutputTarget(result) && result.metadata().equalsRecipe(recipeUid, result.metadata().groupId())) {
					requiredAmount.computeIfPresent(result, (ignored, amount) -> amount.subtract(result.metadata().bigAmount(multiplier)));
				}
			}
		}

		return createDetails();
	}

	private void resetCalculation() {
		preferredItems.clear();
		requiredAmount.clear();
		containerItems.clear();
		reusableContainerItems.clear();
		workingMultipliers.clear();

		for (RecipeChainInput ingredient : recipeIngredients) {
			workingMultipliers.put(ingredient, BigIntMath.ZERO);
		}
		for (RecipeChainInput result : recipeResults) {
			workingMultipliers.put(result, BigIntMath.ZERO);
		}

		preferredItems.putAll(plan.preferredResults());
	}

	private void rebuildPlan() {
		List<RecipeChainInput> graphInputs = new ArrayList<>(recipeIngredients.size() + recipeResults.size());
		graphInputs.addAll(recipeIngredients);
		graphInputs.addAll(recipeResults);
		this.plan = RecipeChainPlan.compile(graphInputs, collapsedRecipes);
		this.graph = plan.graph();
	}

	private void calculateSuitableRecipe(RecipeChainInput ingredient, BigInteger amount, List<ResourceLocation> visited) {
		RecipeChainInput preferred = preferredItems.get(ingredient);
		BigInteger requested = amount;

		if (amount.signum() > 0) {
			amount = shiftContainerItems(ingredient.metadata(), amount);
		}

		if (amount.signum() > 0) {
			for (RecipeChainInput initialItem : initialItems) {
				if (ingredient.metadata().isSatisfiedBy(initialItem.metadata())) {
					amount = addRequiredAmount(initialItem, amount, initialItem.metadata().bigAmount());
					if (amount.signum() == 0) {
						break;
					}
				}
			}
		}

		if (preferred == null) {
			if (demandVisitor != null) {
				demandVisitor.visitBig(ingredient, requested, amount);
			}
			addRequiredAmount(ingredient, amount, null);
			return;
		}

		ResourceLocation preferredRecipe = preferred.metadata().recipeUid();
		if (preferredRecipe == null || visited.contains(preferredRecipe)) {
			if (demandVisitor != null) {
				demandVisitor.visitBig(ingredient, requested, amount);
			}
			addRequiredAmount(preferred, amount, null);
			return;
		}

		addRequiredAmount(preferred, amount, null);
		BigInteger targetAmount = requiredAmount.getOrDefault(preferred, BigIntMath.ZERO);
		BigInteger neededMultiplier = preferred.metadata().multiplierFromAmount(targetAmount);
		BigInteger currentMultiplier = workingMultipliers.getOrDefault(preferred, BigIntMath.ZERO);
		BigInteger multiplier = BigIntMath.max(BigIntMath.ZERO, neededMultiplier.subtract(currentMultiplier));

		IDemandVisitor childVisitor = demandVisitor == null ? null : demandVisitor.visitBig(ingredient, requested, amount);
		if (multiplier.signum() > 0) {
			addShift(preferredRecipe, multiplier);
			visited.add(preferredRecipe);
			IDemandVisitor previousVisitor = demandVisitor;
			demandVisitor = childVisitor;
			try {
				prepareIngredients(preferredRecipe, multiplier, visited);
			} finally {
				demandVisitor = previousVisitor;
			}
			visited.remove(preferredRecipe);
		}
	}

	private void prepareIngredients(ResourceLocation recipeUid, BigInteger multiplier, List<ResourceLocation> visited) {
		for (RecipeChainInput ingredient : graph.ingredientsFor(recipeUid)) {
			if (!ingredient.metadata().emptyFactor()) {
				calculateSuitableRecipe(ingredient, ingredient.metadata().bigAmount(multiplier), visited);
			}
		}
	}

	private BigInteger addRequiredAmount(RecipeChainInput input, BigInteger amount, @org.jetbrains.annotations.Nullable BigInteger maxAmount) {
		if (amount == null || amount.signum() <= 0) {
			requiredAmount.putIfAbsent(input, BigIntMath.ZERO);
			return BigIntMath.ZERO;
		}
		if (isReusableContainerItem(input.metadata())) {
			return addReusableContainerRequiredAmount(input, amount, maxAmount);
		}
		BigInteger currentAmount = requiredAmount.getOrDefault(input, BigIntMath.ZERO);
		BigInteger consumed;
		if (maxAmount == null) {
			consumed = amount;
		} else {
			BigInteger available = BigIntMath.max(BigIntMath.ZERO, maxAmount.subtract(currentAmount));
			consumed = BigIntMath.min(amount, available);
		}
		addContainerItems(input.metadata(), consumed);
		if (consumed.signum() > 0 || requiredAmount.containsKey(input)) {
			requiredAmount.put(input, BigIntMath.add(currentAmount, consumed));
		}
		return amount.subtract(consumed);
	}

	private BigInteger addReusableContainerRequiredAmount(RecipeChainInput input, BigInteger amount, @org.jetbrains.annotations.Nullable BigInteger maxAmount) {
		BookmarkItemMetadata metadata = input.metadata();
		BigInteger currentAmount = requiredAmount.getOrDefault(input, BigIntMath.ZERO);
		BigInteger remaining = amount;
		long usesPerItemLong = metadata.containerItemCraftingUses();
		BigInteger usesPerItem = BigIntMath.valueOf(usesPerItemLong);
		while (remaining.signum() > 0 && (maxAmount == null || currentAmount.compareTo(maxAmount) < 0)) {
			currentAmount = currentAmount.add(BigIntMath.ONE);
			BigInteger covered = BigIntMath.min(remaining, usesPerItem);
			remaining = remaining.subtract(covered);
			long remainingUses = usesPerItemLong - covered.longValue();
			addReusableContainerItem(metadata, remainingUses);
			if (remainingUses == 0) {
				addContainerItem(metadata.brokenContainerItem(), BigIntMath.ONE);
			}
		}
		if (currentAmount.signum() > 0 || requiredAmount.containsKey(input)) {
			requiredAmount.put(input, currentAmount);
		}
		return remaining;
	}

	private BigInteger shiftContainerItems(BookmarkItemMetadata metadata, BigInteger amount) {
		BigInteger remaining = shiftReusableContainerItems(metadata, amount);
		if (remaining.signum() <= 0) {
			return BigIntMath.ZERO;
		}
		if (metadata.containerItem() == null) {
			return remaining;
		}
		for (BookmarkIngredientKey permutation : metadata.permutations()) {
			if (remaining.signum() <= 0) {
				break;
			}
			BigInteger available = containerItems.getOrDefault(permutation, BigIntMath.ZERO);
			if (available.signum() <= 0) {
				continue;
			}
			BigInteger consumed = BigIntMath.min(remaining, available);
			remaining = remaining.subtract(consumed);
			BigInteger left = available.subtract(consumed);
			if (left.signum() > 0) {
				containerItems.put(permutation, left);
			} else {
				containerItems.remove(permutation);
			}
		}
		return remaining;
	}

	private void addContainerItems(BookmarkItemMetadata metadata, BigInteger amount) {
		BookmarkIngredientKey containerItem = metadata.containerItem();
		boolean blacklisted = metadata.permutations().stream()
			.anyMatch(containerItemsBlacklist::contains);
		if (containerItem != null && metadata.containerItemCraftingUses() > 0 && amount.signum() > 0 && !blacklisted) {
			addContainerItem(containerItem, amount);
		}
	}

	private void addReusableContainerItem(BookmarkItemMetadata metadata, long remainingUses) {
		BookmarkIngredientKey containerItem = metadata.containerItem();
		boolean blacklisted = metadata.permutations().stream()
			.anyMatch(containerItemsBlacklist::contains);
		if (containerItem != null && remainingUses > 0 && !blacklisted) {
			reusableContainerItems.computeIfAbsent(containerItem, ignored -> new ArrayList<>())
				.add(new RecipeChainRemainderStack(containerItem, remainingUses, metadata.brokenContainerItem()));
			addContainerItem(containerItem, BigIntMath.ONE);
		}
	}

	private BigInteger shiftReusableContainerItems(BookmarkItemMetadata metadata, BigInteger amount) {
		BigInteger remaining = amount;
		for (BookmarkIngredientKey permutation : metadata.permutations()) {
			if (remaining.signum() <= 0) {
				break;
			}
			List<RecipeChainRemainderStack> reusableItems = reusableContainerItems.get(permutation);
			if (reusableItems == null) {
				continue;
			}
			for (int i = 0; i < reusableItems.size() && remaining.signum() > 0;) {
				RecipeChainRemainderStack reusableItem = reusableItems.get(i);
				BigInteger availableUses = BigIntMath.valueOf(reusableItem.remainingUses());
				BigInteger consumed = BigIntMath.min(remaining, availableUses);
				remaining = remaining.subtract(consumed);
				long leftUses = reusableItem.remainingUses() - consumed.longValue();
				if (leftUses > 0) {
					reusableItems.set(i, reusableItem.withRemainingUses(leftUses));
					i++;
				} else {
					reusableItems.remove(i);
					decrementContainerItem(permutation);
					addContainerItem(reusableItem.brokenKey(), BigIntMath.ONE);
				}
			}
			if (reusableItems.isEmpty()) {
				reusableContainerItems.remove(permutation);
			}
		}
		return remaining;
	}

	private void addContainerItem(BookmarkIngredientKey key, BigInteger amount) {
		if (key != null && amount != null && amount.signum() > 0) {
			containerItems.merge(key, amount, BigIntMath::add);
		}
	}

	private void decrementContainerItem(BookmarkIngredientKey key) {
		BigInteger amount = containerItems.getOrDefault(key, BigIntMath.ZERO);
		if (amount.compareTo(BigIntMath.ONE) <= 0) {
			containerItems.remove(key);
		} else {
			containerItems.put(key, amount.subtract(BigIntMath.ONE));
		}
	}

	private static boolean isReusableContainerItem(BookmarkItemMetadata metadata) {
		return metadata.containerItem() != null && metadata.containerItemCraftingUses() > 1;
	}

	private void addShift(ResourceLocation recipeUid, BigInteger shift) {
		for (RecipeChainInput ingredient : graph.ingredientsFor(recipeUid)) {
			workingMultipliers.merge(ingredient, shift, BigIntMath::add);
		}
		for (RecipeChainInput result : graph.resultsFor(recipeUid)) {
			workingMultipliers.merge(result, shift, BigIntMath::add);
		}
	}

	private RecipeChainDetails createDetails() {
		Map<Integer, RecipeChainItem> calculatedItems = new LinkedHashMap<>();
		Set<ResourceLocation> topLevelRecipes = getTopLevelRecipes();
		Map<ResourceLocation, Set<ResourceLocation>> recipeRelations = createRecipeRelations(topLevelRecipes);
		Map<Integer, ResourceLocation> itemToRecipe = createItemToRecipe(recipeRelations);
		Set<ResourceLocation> middleRecipes = getMiddleRecipes();
		Map<BookmarkIngredientKey, Long> missedItems = new LinkedHashMap<>();
		Set<Integer> initialItemIndexes = new LinkedHashSet<>();
		Set<Integer> missingIngredients = new LinkedHashSet<>();
		Set<Integer> remainderItems = new LinkedHashSet<>();

		for (RecipeChainInput initialItem : initialItems) {
			initialItemIndexes.add(initialItem.index());
			BookmarkItemMetadata metadata = initialItem.metadata();
			BigInteger required = requiredAmount.getOrDefault(initialItem, BigIntMath.ZERO);
			BigInteger itemAmount = metadata.bigAmount();
			calculatedItems.put(initialItem.index(), new RecipeChainItem(
				initialItem.index(),
				metadata,
				RecipeChainItemType.INGREDIENT,
				itemAmount,
				required,
				itemAmount,
				BigIntMath.valueOf(metadata.multiplier()),
				BigIntMath.valueOf(metadata.multiplier())
			));
		}

		for (RecipeChainInput result : recipeResults) {
			BookmarkItemMetadata metadata = result.metadata();
			ResourceLocation recipeUid = metadata.recipeUid();
			BigInteger calculatedMultiplier = workingMultipliers.getOrDefault(result, BigIntMath.ZERO);
			BigInteger required = requiredAmount.getOrDefault(result, BigIntMath.ZERO);
			BigInteger calculatedAmount = metadata.bigAmount(calculatedMultiplier);
			BigInteger shiftAmount = BigIntMath.max(BigIntMath.ZERO, calculatedAmount.subtract(required));
			RecipeChainItemType type = recipeUid != null && outputRecipes.containsKey(recipeUid) && isOutputTarget(result) ? RecipeChainItemType.RESULT : RecipeChainItemType.REMAINDER;
			if (shiftAmount.signum() > 0 && type == RecipeChainItemType.REMAINDER) {
				remainderItems.add(result.index());
			}
			long realMultiplierLong = getRealMultiplier(metadata, middleRecipes);
			BigInteger realMultiplier = BigIntMath.valueOf(realMultiplierLong);
			calculatedItems.put(result.index(), new RecipeChainItem(
				result.index(),
				metadata,
				type,
				metadata.bigAmount(realMultiplier),
				shiftAmount,
				calculatedAmount,
				realMultiplier,
				calculatedMultiplier
			));
		}

		for (RecipeChainInput ingredient : recipeIngredients) {
			BookmarkItemMetadata metadata = ingredient.metadata();
			BigInteger calculatedMultiplier = workingMultipliers.getOrDefault(ingredient, BigIntMath.ZERO);
			RecipeChainInput preferred = preferredItems.get(ingredient);
			BigInteger required = requiredAmount.containsKey(preferred) ? BigIntMath.ZERO : requiredAmount.getOrDefault(ingredient, metadata.bigAmount(calculatedMultiplier));
			if (required.signum() > 0) {
				missingIngredients.add(ingredient.index());
				addMissedItem(missedItems, metadata, BigIntMath.toLongSaturated(required));
			}
			long realMultiplierLong = getRealMultiplier(metadata, middleRecipes);
			BigInteger realMultiplier = BigIntMath.valueOf(realMultiplierLong);
			calculatedItems.put(ingredient.index(), new RecipeChainItem(
				ingredient.index(),
				metadata,
				RecipeChainItemType.INGREDIENT,
				metadata.bigAmount(realMultiplier),
				required,
				metadata.bigAmount(calculatedMultiplier),
				realMultiplier,
				calculatedMultiplier
			));
		}

		Map<ResourceLocation, RecipeChainDetails.CollapsedBlock> collapsedBlocks = createCollapsedBlocks(topLevelRecipes, itemToRecipe, recipeRelations);

		Map<Integer, Integer> suppliers = new LinkedHashMap<>();
		preferredItems.forEach((input, result) -> {
			if (input.metadata().type().isGraphInput()) {
				suppliers.put(input.index(), result.index());
			}
		});

		Map<BookmarkIngredientKey, Long> saturatedContainerItems = new LinkedHashMap<>();
		containerItems.forEach((key, val) -> saturatedContainerItems.put(key, BigIntMath.toLongSaturated(val)));

		return new RecipeChainDetails(
			calculatedItems,
			suppliers,
			itemToRecipe,
			outputRecipes.keySet(),
			middleRecipes,
			recipeRelations,
			missedItems,
			saturatedContainerItems,
			initialItemIndexes,
			missingIngredients,
			remainderItems,
			collapsedBlocks
		);
	}

	private static void addMissedItem(Map<BookmarkIngredientKey, Long> missedItems, BookmarkItemMetadata metadata, long amount) {
		metadata.permutations().stream()
			.findFirst()
			.ifPresent(key -> missedItems.merge(key, amount, SaturatedMath::add));
	}

	private Set<ResourceLocation> getMiddleRecipes() {
		Set<ResourceLocation> middleRecipes = new LinkedHashSet<>();
		for (RecipeChainInput result : preferredItems.values()) {
			ResourceLocation recipeUid = result.metadata().recipeUid();
			if (recipeUid != null) {
				middleRecipes.add(recipeUid);
			}
		}
		return middleRecipes;
	}

	private Map<ResourceLocation, Set<ResourceLocation>> createRecipeRelations(Set<ResourceLocation> topLevelRecipes) {
		Map<ResourceLocation, Set<ResourceLocation>> recipeRelations = new LinkedHashMap<>();
		for (ResourceLocation recipeUid : sortedCollapsedRecipes()) {
			recipeRelations.put(recipeUid, getRecipeRelations(recipeUid, topLevelRecipes, new LinkedHashSet<>(Set.of(recipeUid))));
		}
		return recipeRelations;
	}

	private Set<ResourceLocation> getRecipeRelations(ResourceLocation recipeUid, Set<ResourceLocation> topLevelRecipes, Set<ResourceLocation> recipes) {
		for (RecipeChainInput ingredient : graph.ingredientsFor(recipeUid)) {
			RecipeChainInput preferred = preferredItems.get(ingredient);
			ResourceLocation preferredRecipe = preferred == null ? null : preferred.metadata().recipeUid();
			if (preferredRecipe != null && !topLevelRecipes.contains(preferredRecipe) && recipes.add(preferredRecipe)) {
				getRecipeRelations(preferredRecipe, topLevelRecipes, recipes);
			}
		}
		return recipes;
	}

	private Set<ResourceLocation> getTopLevelRecipes() {
		Set<ResourceLocation> topLevelRecipes = new LinkedHashSet<>(outputRecipes.keySet());
		for (RecipeChainInput result : recipeResults) {
			ResourceLocation recipeUid = result.metadata().recipeUid();
			if (recipeUid == null || topLevelRecipes.contains(recipeUid)) {
				continue;
			}
			Set<ResourceLocation> parents = getRecipeParents(recipeUid, new LinkedHashSet<>(), new LinkedHashSet<>(Set.of(recipeUid)));
			if (parents.size() != 1 || parents.stream().noneMatch(collapsedRecipes::contains)) {
				topLevelRecipes.add(recipeUid);
			}
		}
		return topLevelRecipes;
	}

	private Set<ResourceLocation> getRecipeParents(ResourceLocation recipeUid, Set<ResourceLocation> parents, Set<ResourceLocation> visited) {
		Set<ResourceLocation> consumers = new LinkedHashSet<>();
		for (Map.Entry<RecipeChainInput, RecipeChainInput> entry : preferredItems.entrySet()) {
			RecipeChainInput preferred = entry.getValue();
			if (preferred != null && recipeUid.equals(preferred.metadata().recipeUid())) {
				ResourceLocation consumer = entry.getKey().metadata().recipeUid();
				if (consumer != null) {
					consumers.add(consumer);
				}
			}
		}
		if (consumers.isEmpty()) {
			parents.add(recipeUid);
		} else {
			for (ResourceLocation consumer : consumers) {
				if (visited.add(consumer)) {
					if (outputRecipes.containsKey(consumer)) {
						parents.add(consumer);
					} else {
						getRecipeParents(consumer, parents, visited);
					}
				}
			}
		}
		return parents;
	}

	private Map<Integer, ResourceLocation> createItemToRecipe(Map<ResourceLocation, Set<ResourceLocation>> recipeRelations) {
		Map<Integer, ResourceLocation> itemToRecipe = new LinkedHashMap<>();
		if (collapsedRecipes.isEmpty()) {
			return itemToRecipe;
		}
		for (ResourceLocation collapsedRecipe : sortedCollapsedRecipes()) {
			Set<ResourceLocation> relations = recipeRelations.getOrDefault(collapsedRecipe, Set.of(collapsedRecipe));
			for (ResourceLocation recipeUid : relations) {
				for (RecipeChainInput input : graph.itemsFor(recipeUid)) {
					itemToRecipe.put(input.index(), collapsedRecipe);
				}
			}
		}
		return itemToRecipe;
	}

	/**
	 * Parallelized collapsed block compilation when multiple collapsed blocks are present.
	 * Pure calculation without shared mutable state across blocks.
	 */
	private Map<ResourceLocation, RecipeChainDetails.CollapsedBlock> createCollapsedBlocks(
		Set<ResourceLocation> topLevelRecipes,
		Map<Integer, ResourceLocation> itemToRecipe,
		Map<ResourceLocation, Set<ResourceLocation>> recipeRelations
	) {
		Map<ResourceLocation, RecipeChainDetails.CollapsedBlock> collapsedBlocks = new LinkedHashMap<>();
		if (collapsedRecipes.isEmpty()) {
			return collapsedBlocks;
		}
		Set<ResourceLocation> middleRecipes = getMiddleRecipes();
		List<ResourceLocation> sortedRoots = sortedCollapsedRecipes();

		// Use multithreaded calculation if there are multiple collapsed blocks
		if (sortedRoots.size() > 1) {
			List<RecipeChainDetails.CollapsedBlock> blocks = sortedRoots.parallelStream()
				.map(root -> computeCollapsedBlock(root, topLevelRecipes, itemToRecipe, recipeRelations, middleRecipes))
				.toList();
			for (RecipeChainDetails.CollapsedBlock block : blocks) {
				collapsedBlocks.put(block.recipeUid(), block);
			}
		} else {
			for (ResourceLocation root : sortedRoots) {
				collapsedBlocks.put(root, computeCollapsedBlock(root, topLevelRecipes, itemToRecipe, recipeRelations, middleRecipes));
			}
		}
		return collapsedBlocks;
	}

	private RecipeChainDetails.CollapsedBlock computeCollapsedBlock(
		ResourceLocation root,
		Set<ResourceLocation> topLevelRecipes,
		Map<Integer, ResourceLocation> itemToRecipe,
		Map<ResourceLocation, Set<ResourceLocation>> recipeRelations,
		Set<ResourceLocation> middleRecipes
	) {
		Set<ResourceLocation> relations = recipeRelations.getOrDefault(root, Set.of(root));
		List<RecipeChainInput> closureResults = recipeResults.stream()
			.filter(result -> root.equals(itemToRecipe.get(result.index())))
			.filter(result -> {
				ResourceLocation recipeUid = result.metadata().recipeUid();
				return recipeUid != null && relations.contains(recipeUid);
			})
			.toList();
		List<RecipeChainInput> closureIngredients = recipeIngredients.stream()
			.filter(ingredient -> root.equals(itemToRecipe.get(ingredient.index())))
			.filter(ingredient -> {
				ResourceLocation recipeUid = ingredient.metadata().recipeUid();
				return recipeUid != null && relations.contains(recipeUid);
			})
			.toList();
		Map<String, BigInteger> shadowShifts = computeShadowShifts(root, relations);
		List<BlockAccumulator> collected = collectCollapsedItems(
			root,
			relations,
			topLevelRecipes,
			outputRecipes.keySet(),
			closureResults,
			closureIngredients,
			preferredItems,
			requiredAmount,
			workingMultipliers
		);
		for (BlockAccumulator accumulator : collected) {
			BigInteger realMultiplier;
			if (accumulator.anchor()) {
				long mult = middleRecipes.contains(root) ? Math.max(0, accumulator.metadata().multiplier() - 1) : accumulator.metadata().multiplier();
				realMultiplier = BigIntMath.valueOf(mult);
			} else {
				realMultiplier = accumulator.metadata()
					.multiplierFromAmount(shadowShifts.getOrDefault(accumulator.key(), BigIntMath.ZERO));
			}
			accumulator.setRealMultiplier(realMultiplier);
		}

		List<RecipeChainDetails.CollapsedBlockItem> blockItems = new ArrayList<>();
		Comparator<BlockAccumulator> blockComparator = Comparator
			.comparingInt(BlockAccumulator::typeWeight)
			.thenComparingInt(BlockAccumulator::sourceIndex);
		collected.stream()
			.sorted(blockComparator)
			.map(BlockAccumulator::toBlockItem)
			.forEach(blockItems::add);
		return new RecipeChainDetails.CollapsedBlock(root, List.copyOf(blockItems));
	}

	/**
	 * Derived from GTNH NEI RecipeChainDetails.generateShadowItems(): re-runs the closure math
	 * with the bookmark multipliers (the collapsed root keeps its multiplier, every other
	 * closure recipe is zeroed) so the block can show the real, bookmark-driven amounts.
	 */
	private Map<String, BigInteger> computeShadowShifts(ResourceLocation root, Set<ResourceLocation> relations) {
		List<RecipeChainInput> subResults = new ArrayList<>();
		List<RecipeChainInput> subIngredients = new ArrayList<>();
		for (RecipeChainInput result : recipeResults) {
			ResourceLocation recipeUid = result.metadata().recipeUid();
			if (recipeUid != null && relations.contains(recipeUid)) {
				long multiplier = root.equals(recipeUid) ? result.metadata().multiplier() : 0;
				BookmarkItemMetadata metadata = result.metadata().withMultiplier(multiplier);
				subResults.add(new RecipeChainInput(result.index(), metadata, result.selectedKey(), result.selectedIngredient()));
			}
		}
		for (RecipeChainInput ingredient : recipeIngredients) {
			ResourceLocation recipeUid = ingredient.metadata().recipeUid();
			if (recipeUid != null && relations.contains(recipeUid)) {
				long multiplier = root.equals(recipeUid) ? ingredient.metadata().multiplier() : 0;
				BookmarkItemMetadata metadata = ingredient.metadata().withMultiplier(multiplier);
				subIngredients.add(new RecipeChainInput(ingredient.index(), metadata, ingredient.selectedKey(), ingredient.selectedIngredient()));
			}
		}
		List<RecipeChainInput> subInputs = new ArrayList<>(subResults.size() + subIngredients.size());
		subInputs.addAll(subResults);
		subInputs.addAll(subIngredients);
		RecipeChainMath shadowMath = RecipeChainMath.of(subInputs, Set.of());
		shadowMath.refresh();
		List<BlockAccumulator> shadowItems = collectCollapsedItems(
			root,
			relations,
			Set.of(),
			shadowMath.outputRecipes.keySet(),
			subResults,
			subIngredients,
			shadowMath.preferredItems,
			shadowMath.requiredAmount,
			shadowMath.workingMultipliers
		);
		Map<String, BigInteger> shifts = new HashMap<>();
		for (BlockAccumulator item : shadowItems) {
			shifts.put(item.key(), item.shiftAmount());
		}
		return shifts;
	}

	private static List<BlockAccumulator> collectCollapsedItems(
		ResourceLocation root,
		Set<ResourceLocation> relations,
		Set<ResourceLocation> topLevelRecipes,
		Set<ResourceLocation> outputRecipes,
		List<RecipeChainInput> closureResults,
		List<RecipeChainInput> closureIngredients,
		Map<RecipeChainInput, RecipeChainInput> preferredItems,
		Map<RecipeChainInput, BigInteger> requiredAmount,
		Map<RecipeChainInput, BigInteger> workingMultipliers
	) {
		Map<String, BlockAccumulator> results = new LinkedHashMap<>();
		Map<String, BlockAccumulator> ingredients = new LinkedHashMap<>();
		BigInteger rootMultiplier = BigIntMath.ZERO;
		for (RecipeChainInput result : closureResults) {
			BookmarkItemMetadata metadata = result.metadata();
			ResourceLocation recipeUid = metadata.recipeUid();
			if (recipeUid == null || !relations.contains(recipeUid)) {
				continue;
			}
			BigInteger itemAmount = metadata.bigAmount(workingMultipliers.getOrDefault(result, BigIntMath.ZERO));
			BigInteger required = requiredAmount.getOrDefault(result, BigIntMath.ZERO);
			BigInteger amount = itemAmount.subtract(required);
			boolean anchor = root.equals(recipeUid);
			if (anchor) {
				rootMultiplier = workingMultipliers.getOrDefault(result, BigIntMath.ZERO);
			}
			if (anchor || amount.signum() > 0) {
				String key = aggregationKey(metadata, result.index());
				RecipeChainItemType type = outputRecipes.contains(recipeUid) ? RecipeChainItemType.RESULT : RecipeChainItemType.REMAINDER;
				BlockAccumulator accumulator = results.computeIfAbsent(
					key,
					ignored -> new BlockAccumulator(key, result.index(), metadata, anchor ? RecipeChainItemType.RESULT : type, anchor)
				);
				accumulator.append(amount, itemAmount, metadata);
			}
		}
		for (RecipeChainInput ingredient : closureIngredients) {
			BookmarkItemMetadata metadata = ingredient.metadata();
			ResourceLocation recipeUid = metadata.recipeUid();
			if (recipeUid == null || !relations.contains(recipeUid)) {
				continue;
			}
			RecipeChainInput preferred = preferredItems.get(ingredient);
			BigInteger itemAmount = metadata.bigAmount(workingMultipliers.getOrDefault(ingredient, BigIntMath.ZERO));
			BigInteger amount = requiredAmount.containsKey(preferred) ? BigIntMath.ZERO : requiredAmount.getOrDefault(ingredient, itemAmount);
			BigInteger refAmount = preferred != null && !topLevelRecipes.contains(preferred.metadata().recipeUid()) ? requiredAmount.getOrDefault(preferred, BigIntMath.ZERO) : BigIntMath.ZERO;
			boolean include = amount.signum() != 0 ||
				(itemAmount.compareTo(refAmount) > 0 && requiredAmount.containsKey(preferred)) ||
				(root.equals(recipeUid) && rootMultiplier.signum() == 0);
			if (!include) {
				continue;
			}
			String key = aggregationKey(metadata, ingredient.index());
			BlockAccumulator accumulator = ingredients.computeIfAbsent(
				key,
				ignored -> new BlockAccumulator(key, ingredient.index(), metadata, RecipeChainItemType.INGREDIENT, false)
			);
			accumulator.append(amount, itemAmount.subtract(refAmount), metadata);
		}
		List<BlockAccumulator> collected = new ArrayList<>(results.size() + ingredients.size());
		collected.addAll(results.values());
		collected.addAll(ingredients.values());
		return collected;
	}

	private List<ResourceLocation> sortedCollapsedRecipes() {
		return collapsedRecipes.stream().sorted().toList();
	}

	private static String aggregationKey(BookmarkItemMetadata metadata, int sourceIndex) {
		return metadata.permutations().stream()
			.min(Comparator.naturalOrder())
			.map(BookmarkIngredientKey::stableKey)
			.orElse("index:" + sourceIndex);
	}

	private static final class BlockAccumulator {
		private final String key;
		private final int sourceIndex;
		private final BookmarkItemMetadata metadata;
		private final RecipeChainItemType type;
		private final boolean anchor;
		private BigInteger shiftAmount = BigIntMath.ZERO;
		private BigInteger calculatedAmount = BigIntMath.ZERO;
		private BigInteger calculatedMultiplier = BigIntMath.ZERO;
		private BigInteger realMultiplier = BigIntMath.ZERO;

		private BlockAccumulator(
			String key,
			int sourceIndex,
			BookmarkItemMetadata metadata,
			RecipeChainItemType type,
			boolean anchor
		) {
			this.key = key;
			this.sourceIndex = sourceIndex;
			this.metadata = metadata;
			this.type = type;
			this.anchor = anchor;
		}

		private void append(BigInteger amount, BigInteger calculatedAmount, BookmarkItemMetadata metadata) {
			shiftAmount = BigIntMath.add(shiftAmount, amount);
			this.calculatedAmount = BigIntMath.add(this.calculatedAmount, calculatedAmount);
			calculatedMultiplier = BigIntMath.add(calculatedMultiplier, metadata.multiplierFromAmount(calculatedAmount));
		}

		private String key() {
			return key;
		}

		private BigInteger shiftAmount() {
			return shiftAmount;
		}

		private BookmarkItemMetadata metadata() {
			return metadata;
		}

		private boolean anchor() {
			return anchor;
		}

		private void setRealMultiplier(BigInteger realMultiplier) {
			this.realMultiplier = realMultiplier == null ? BigIntMath.ZERO : realMultiplier;
		}

		private int typeWeight() {
			return switch (type) {
				case RESULT -> 0;
				case REMAINDER -> 1;
				case INGREDIENT -> 2;
			};
		}

		private int sourceIndex() {
			return sourceIndex;
		}

		private RecipeChainDetails.CollapsedBlockItem toBlockItem() {
			RecipeChainItem item = new RecipeChainItem(
				sourceIndex,
				metadata,
				type,
				metadata.bigAmount(realMultiplier),
				shiftAmount,
				calculatedAmount,
				realMultiplier,
				calculatedMultiplier
			);
			return new RecipeChainDetails.CollapsedBlockItem(sourceIndex, metadata, item, anchor);
		}
	}

	private static long getRealMultiplier(BookmarkItemMetadata metadata, Set<ResourceLocation> middleRecipes) {
		ResourceLocation recipeUid = metadata.recipeUid();
		if (recipeUid != null && middleRecipes.contains(recipeUid)) {
			return Math.max(0, metadata.multiplier() - 1);
		}
		return metadata.multiplier();
	}

	private boolean isOutputTarget(RecipeChainInput result) {
		ResourceLocation recipeUid = result.metadata().recipeUid();
		return recipeUid != null && outputTargets.get(recipeUid) == result;
	}
}
