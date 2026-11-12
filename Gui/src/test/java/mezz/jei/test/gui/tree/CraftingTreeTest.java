package mezz.jei.test.gui.tree;

import mezz.jei.gui.bookmarks.BookmarkIngredientKey;
import mezz.jei.gui.bookmarks.BookmarkItemMetadata;
import mezz.jei.gui.bookmarks.BookmarkItemType;
import mezz.jei.gui.bookmarks.chain.RecipeChainInput;
import mezz.jei.gui.tree.TreeCamera;
import mezz.jei.gui.tree.TreeGraph;
import mezz.jei.gui.tree.TreeLayout;
import mezz.jei.gui.tree.TreeMath;
import mezz.jei.gui.tree.TreeSlot;
import mezz.jei.gui.tree.TreeStats;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CraftingTreeTest {

	@Test
	public void testBigIntegerTreeMathCalculation() {
		BookmarkIngredientKey ironSword = BookmarkIngredientKey.of("minecraft:item", "minecraft:iron_sword");
		BookmarkIngredientKey ironIngot = BookmarkIngredientKey.of("minecraft:item", "minecraft:iron_ingot");
		BookmarkIngredientKey stick = BookmarkIngredientKey.of("minecraft:item", "minecraft:stick");

		ResourceLocation swordRecipe = new ResourceLocation("minecraft", "iron_sword");

		// Target iron sword with huge BigInteger count (10^20)
		BigInteger hugeDemand = new BigInteger("100000000000000000000");

		BookmarkItemMetadata swordMeta = new BookmarkItemMetadata(
			1,
			BookmarkItemType.RESULT,
			1,
			1,
			BookmarkItemMetadata.CHANCE_FULL,
			new ResourceLocation("minecraft", "crafting"),
			swordRecipe,
			Set.of(ironSword)
		);

		BookmarkItemMetadata ironMeta = new BookmarkItemMetadata(
			1,
			BookmarkItemType.INGREDIENT,
			2,
			1,
			BookmarkItemMetadata.CHANCE_FULL,
			new ResourceLocation("minecraft", "crafting"),
			swordRecipe,
			Set.of(ironIngot)
		);

		BookmarkItemMetadata stickMeta = new BookmarkItemMetadata(
			1,
			BookmarkItemType.INGREDIENT,
			1,
			1,
			BookmarkItemMetadata.CHANCE_FULL,
			new ResourceLocation("minecraft", "crafting"),
			swordRecipe,
			Set.of(stick)
		);

		List<RecipeChainInput> inputs = List.of(
			new RecipeChainInput(0, swordMeta, ironSword),
			new RecipeChainInput(1, ironMeta, ironIngot),
			new RecipeChainInput(2, stickMeta, stick)
		);

		TreeMath math = new TreeMath(inputs, Map.of());
		math.setManualAmount(ironSword, hugeDemand);

		TreeGraph graph = math.calculate();
		Assertions.assertEquals(1, graph.getRoots().size());

		TreeSlot root = graph.getRoots().get(0);
		Assertions.assertEquals(hugeDemand, root.getRequestedAmount());
		Assertions.assertEquals(hugeDemand, root.getMultiplier());

		List<TreeSlot> children = graph.getChildren(root);
		Assertions.assertEquals(2, children.size());

		// Iron ingot should require 2 * hugeDemand
		TreeSlot ironSlot = children.stream().filter(c -> c.getKey().equals(ironIngot)).findFirst().orElseThrow();
		Assertions.assertEquals(hugeDemand.multiply(BigInteger.valueOf(2)), ironSlot.getRequestedAmount());

		// Stick should require 1 * hugeDemand
		TreeSlot stickSlot = children.stream().filter(c -> c.getKey().equals(stick)).findFirst().orElseThrow();
		Assertions.assertEquals(hugeDemand, stickSlot.getRequestedAmount());
	}

	@Test
	public void testInventoryAllocationAndForceCraft() {
		BookmarkIngredientKey bread = BookmarkIngredientKey.of("minecraft:item", "minecraft:bread");
		BookmarkIngredientKey wheat = BookmarkIngredientKey.of("minecraft:item", "minecraft:wheat");
		ResourceLocation breadRecipe = new ResourceLocation("minecraft", "bread");

		BookmarkItemMetadata breadMeta = new BookmarkItemMetadata(
			1,
			BookmarkItemType.RESULT,
			1,
			1,
			BookmarkItemMetadata.CHANCE_FULL,
			new ResourceLocation("minecraft", "crafting"),
			breadRecipe,
			Set.of(bread)
		);

		BookmarkItemMetadata wheatMeta = new BookmarkItemMetadata(
			1,
			BookmarkItemType.INGREDIENT,
			3,
			1,
			BookmarkItemMetadata.CHANCE_FULL,
			new ResourceLocation("minecraft", "crafting"),
			breadRecipe,
			Set.of(wheat)
		);

		List<RecipeChainInput> inputs = List.of(
			new RecipeChainInput(0, breadMeta, bread),
			new RecipeChainInput(1, wheatMeta, wheat)
		);

		// In stock: 10 bread, requested 15 bread -> remaining 5 bread to craft
		Map<BookmarkIngredientKey, BigInteger> stock = Map.of(bread, BigInteger.valueOf(10));
		TreeMath math = new TreeMath(inputs, stock);
		math.setManualAmount(bread, BigInteger.valueOf(15));

		TreeGraph graph = math.calculate();
		TreeSlot root = graph.getRoots().get(0);

		Assertions.assertEquals(BigInteger.valueOf(15), root.getRequestedAmount());
		Assertions.assertEquals(BigInteger.valueOf(5), root.getLeftAmount());
		Assertions.assertEquals(BigInteger.valueOf(5), root.getMultiplier());

		// Test Force Craft toggle
		math.setForceCraft(bread, true);
		TreeGraph forceGraph = math.calculate();
		TreeSlot forceRoot = forceGraph.getRoots().get(0);

		// With forceCraft=true, stock is ignored, full 15 must be crafted
		Assertions.assertEquals(BigInteger.valueOf(15), forceRoot.getLeftAmount());
		Assertions.assertEquals(BigInteger.valueOf(15), forceRoot.getMultiplier());
	}

	@Test
	public void testTreeLayoutAndCameraCulling() {
		TreeGraph graph = new TreeGraph();
		BookmarkIngredientKey rootKey = BookmarkIngredientKey.of("minecraft:item", "root");
		BookmarkIngredientKey child1Key = BookmarkIngredientKey.of("minecraft:item", "child1");
		BookmarkIngredientKey child2Key = BookmarkIngredientKey.of("minecraft:item", "child2");

		TreeSlot root = new TreeSlot(rootKey, null, null, null, BigInteger.ONE, BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ONE, false);
		TreeSlot child1 = new TreeSlot(child1Key, null, null, null, BigInteger.ONE, BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ONE, false);
		TreeSlot child2 = new TreeSlot(child2Key, null, null, null, BigInteger.ONE, BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ONE, false);

		graph.addRoot(root);
		graph.addChild(root, child1);
		graph.addChild(root, child2);

		TreeLayout layout = new TreeLayout();
		layout.recalculate(graph, null);

		TreeLayout.Point rootPos = layout.getPosition(root);
		TreeLayout.Point c1Pos = layout.getPosition(child1);
		TreeLayout.Point c2Pos = layout.getPosition(child2);

		Assertions.assertNotNull(rootPos);
		Assertions.assertNotNull(c1Pos);
		Assertions.assertNotNull(c2Pos);

		// Parent center X should be midway between first and last child
		int childrenMidX = (c1Pos.x + c2Pos.x) / 2;
		Assertions.assertEquals(childrenMidX, rootPos.x);

		// Test Camera Frustum Culling
		TreeCamera camera = new TreeCamera();
		camera.setViewport(0, 0, 800, 600);
		camera.centerOn(rootPos.x, rootPos.y);

		Assertions.assertTrue(camera.isWorldRectVisible(rootPos.x, rootPos.y, TreeLayout.SLOT_SIZE, TreeLayout.SLOT_SIZE));
		Assertions.assertFalse(camera.isWorldRectVisible(99999, 99999, 18, 18));
	}

	@Test
	public void testTreeStatsComputation() {
		TreeGraph graph = new TreeGraph();
		BookmarkIngredientKey iron = BookmarkIngredientKey.of("minecraft:item", "minecraft:iron_ingot");
		BookmarkIngredientKey stick = BookmarkIngredientKey.of("minecraft:item", "minecraft:stick");

		TreeSlot root = new TreeSlot(iron, null, new ResourceLocation("minecraft:iron"), new ResourceLocation("minecraft:crafting"), BigInteger.ONE, BigInteger.ZERO, BigInteger.TEN, BigInteger.TEN, BigInteger.ZERO, BigInteger.ZERO, BigInteger.TEN, false);
		TreeSlot raw = new TreeSlot(stick, null, null, null, BigInteger.ONE, BigInteger.ZERO, BigInteger.valueOf(5), BigInteger.ZERO, BigInteger.ZERO, BigInteger.valueOf(5), BigInteger.ZERO, false);

		graph.addRoot(root);
		graph.addChild(root, raw);

		TreeStats stats = TreeStats.compute(graph);
		Assertions.assertFalse(stats.getSection(TreeStats.Kind.RESULT).isEmpty());
		Assertions.assertFalse(stats.getSection(TreeStats.Kind.INGREDIENT_NEEDED).isEmpty());

		TreeStats.Row neededRow = stats.getSection(TreeStats.Kind.INGREDIENT_NEEDED).get(0);
		Assertions.assertEquals(BigInteger.valueOf(5), neededRow.getAmount());
	}
	@Test
	public void testPrunedKeysNodeRemoval() {
		BookmarkIngredientKey sword = BookmarkIngredientKey.of("minecraft:item", "minecraft:iron_sword");
		BookmarkIngredientKey iron = BookmarkIngredientKey.of("minecraft:item", "minecraft:iron_ingot");
		BookmarkIngredientKey stick = BookmarkIngredientKey.of("minecraft:item", "minecraft:stick");

		ResourceLocation swordRecipe = new ResourceLocation("minecraft", "iron_sword");

		BookmarkItemMetadata swordMeta = new BookmarkItemMetadata(
			1, BookmarkItemType.RESULT, 1, 1, BookmarkItemMetadata.CHANCE_FULL,
			new ResourceLocation("minecraft", "crafting"), swordRecipe, Set.of(sword)
		);
		BookmarkItemMetadata ironMeta = new BookmarkItemMetadata(
			1, BookmarkItemType.INGREDIENT, 2, 1, BookmarkItemMetadata.CHANCE_FULL,
			new ResourceLocation("minecraft", "crafting"), swordRecipe, Set.of(iron)
		);
		BookmarkItemMetadata stickMeta = new BookmarkItemMetadata(
			1, BookmarkItemType.INGREDIENT, 1, 1, BookmarkItemMetadata.CHANCE_FULL,
			new ResourceLocation("minecraft", "crafting"), swordRecipe, Set.of(stick)
		);

		List<RecipeChainInput> inputs = List.of(
			new RecipeChainInput(0, swordMeta, sword),
			new RecipeChainInput(1, ironMeta, iron),
			new RecipeChainInput(2, stickMeta, stick)
		);

		// When stick is pruned, the tree must contain only sword and iron
		TreeMath math = new TreeMath(inputs, Map.of(), Set.of(stick));
		TreeGraph graph = math.calculate();

		Assertions.assertEquals(1, graph.getRoots().size());
		TreeSlot root = graph.getRoots().get(0);
		List<TreeSlot> children = graph.getChildren(root);

		Assertions.assertEquals(1, children.size());
		Assertions.assertEquals(iron, children.get(0).getKey());
	}
}
