# CHANGELOG / 変更履歴

[English](#english) | [日本語](#japanese)

---

<a name="japanese"></a>
## 日本語 (Japanese)

### バージョニング方針
本MODでは、上流のJEI / JEIU のバージョンを追跡しやすくするため、以下の命名規則を採用しています：
[上流JEIバージョン]-jeiuu.[JEIUUリビジョン番号] (例: 15.59.0.212-jeiuu.2, 15.59.0.212-jeiuu.4-beta3)
- **上流JEIバージョン** (15.59.0.212): ベースとなっているJEI / JEIUの公式バージョンを表します。
- **JEIUUリビジョン** (jeiuu.4-beta3): 本フォーク（JEIUU）独自で行われた機能追加・バグ修正・最適化の更新回数およびリリース種別（Alpha/Beta/Release）を表します。

---

### [15.59.0.212-jeiuu.4-beta3] - 2026-11-12 (Beta)
#### 修正 (Fixed)
- **クラフトツリー内のアイテム描画が正常に行われない不具合の修正**:
  - `IIngredientRenderer` 描画時に `PoseStack` の座標変換（`translate(x, y, 100)`）およびカメラのズームスケール適用（`scale`）を実装。画面左上端への描画崩れを解消し、各ツリースロットおよび統計バー内に正しいサイズと位置でアイテムが表示されるよう修正。
  - スロット内の数量テキストのZレイヤーを前面（Z=300）に調整し、重なりによる不可視化を防止。
- **レシピチェーンからツリーを開くキー入力（Shift+B / T）が握りつぶされていた問題の修正**:
  - `GroupInputHandler` において、キー入力（`InputType.IMMEDIATE`）に対して `input.isSimulate()` による安全な実行判定を適用。レシピチェーン表示中にも `Shift + B`、`T`、およびマウス中クリックで確実にクラフトツリーが開くよう修正。
- **レシピチェーンからの材料データ（ITypedIngredient）の欠落防止**:
  - `TreeChainBridge` において `BookmarkList.getRecipeChainInputs` を優先参照し、チェーン内の全スロットの `ITypedIngredient` を確実に復元・引当。中間素材の手動レシピ指定時にも JEI レシピから自動で子ノードを動的抽出できるよう強化。

---

### [15.59.0.212-jeiuu.4-beta2] - 2026-11-12 (Beta)
#### 修正・追加 (Fixed & Added)
- **チェーンおよびブックマークからツリーが開けない不具合の修正**:
  - ブックマークスロット上のアイテム、ブックマークチェーン、グループパネルヘッダーのどこからでも `Shift + B`、`T`、および **マウス中クリック** で確実にクラフトツリー画面（`CraftingTreeScreen`）が起動するよう入力ハンドラ階層を再構築。
  - 旧ツリー画面（`RecipeTreeScreen`）へ誘導していた残存コードを完全撤去。
- **チェーン以外（アイテムリスト・レシピ画面・インベントリ）からのツリー直接作成機能**:
  - JEIアイテムリスト、レシピ画面（`RecipesGui`）、インベントリ内の任意のアイテムにカーソルを合わせて `Shift + B` または `T` キーを押すことで、そのアイテムを完成目標（ルート）とするクラフトツリーを即座にゼロから作成・開くことが可能に。
- **目標アイテムとレシピを指定して手動でツリーのルート・階層を探索・構築する機能**:
  - 目標アイテムからツリーを開始し、ツリー内の材料ノードを右クリックして代替レシピを選択することで、その材料の原料ノードがツリーに生えて深掘り展開される手動探索に対応。
  - レシピ解除（原材料に戻す）、ノード削除（枝の剪定）、数量・倍率の手動調整に対応。
  - 手動構築したツリーは画面上部の「チェーン保存」ボタンを押すだけで、ブックマーク欄に新しいクラフトチェーングループとして一発保存可能。

---

### [15.59.0.212-jeiuu.4-beta1] - 2026-11-11 (Beta)
#### 追加・改善 (Added & Improved)
- **クラフトツリー（Crafting Tree）機能の完全移植・統合**:
  - `nei-features-for-jei` のクラフトツリー機能を 1.20.1 Forge 専用に最適化・リファクタリングして統合。
  - **BigInteger（bigint）完全対応**: 要求数、クラフト数、生産数、手動入力数量のすべてを `BigInteger` ベースで高精度演算。10^20規模の巨大数量レシピでもオーバーフローせず正確に算出。
  - **軽量化・フラスタムカリング描画**: ツリーカメラの表示領域判定（`TreeCamera.isWorldRectVisible`）を実装し、画面外のノードや接続線を描画スキップすることで、巨大ツリーでも滑らかな60fpsを維持。
  - **チェーンツリー双方向相互変換**:
    - ブックマーク欄のレシピチェーンから `Shift + B` または `T` キー（またはマウス中クリック）で瞬時にインタラクティブなクラフトツリーを生成。
    - クラフトツリー画面上部の「チェーン保存」ボタンにより、現在のツリー（折りたたみ・手動数量・選択レシピを反映）をブックマークチェーンへエクスポート可能。
  - **代替レシピの選択・指定機能**:
    - ツリー内の材料スロットを右クリックすることで、利用可能なすべての代替レシピ一覧ダイアログ（`TreeRecipeSelectorScreen`）が開き、使用するレシピを直接選択・切り替え可能。
  - **手動調整・倍率指定機能**:
    - 材料ノードを `Shift + 左クリック` または `中クリック` することで調整ダイアログ（`TreeAdjustQuantityScreen`）を開き、数量の直接入力、クイック増減（+1, +10, +64, x2, /2）、強制クラフト切り替え、ノード剪定（削除）が可能。
  - **材料・中間品・副産物・マシンの統計パネル**:
    - 画面下部の統計パネルにより、不足原材料、手持ち原材料、中間作成物、副産物、必要な機械・作業台の一覧をリアルタイム集計して横スクロール一覧表示。
  - **インクリメンタル検索・パン・ズーム**:
    - マウスホイールによる滑らかなズーム（0.2x〜5.0x）、ドラッグによるパン移動、検索バーによるツリー内ノードのインクリメンタル強調表示に対応。

---

### [15.59.0.212-jeiuu.3-beta2] - 2026-11-05 (Beta)
#### 修正・改善 (Fixed & Improved)
- **流体・ガスの巨大数量ホバー時クラッシュおよびフリーズの修正**:
  - `FluidHelper.copyWithAmount` において `Math.toIntExact` による整数オーバーフロー例外（`ArithmeticException: integer overflow`）を `Integer.MAX_VALUE` クランプ処理へ修正。
  - `ProjectedBookmarkElement` でのツールチップ用流体スタック生成時に安全クランプと例外防護を追加し、外部MODのツールチップ生成処理におけるフリーズ・クラッシュを完全に防止。
- **ブックマークツールチップへの BigInteger / long 数量行の追加**:
  - アイテム・液体・ガスのブックマークスロットにホバーした際、ツールチップに安全かつ正確な数量表示（短縮フォーマットおよびカンマ区切りの実数表記）を追加。
- **液体・ガスの単位表記を  md (1,000,000 mB) へ変更**:
  - 1,000 バケツ（1,000,000 mB）を表す流体単位表記を kB から  md に更新。BigInteger の流体量とも一貫して統合。
- **プレビューツールチップの文字重なり防止**:
  - `RecipeChainPreviewTooltipComponent` の数量テキスト描画に動的オートスケーリングと境界クランプを適用。

---

### [15.59.0.212-jeiuu.3-beta1] - 2026-11-05 (Beta)
#### 追加・改善 (Added & Improved)
- **レシピチェーンの BigInteger (bigint) 計算・表示対応**:
  - `Long.MAX_VALUE` 付近の液体やガスを大量に消費・生産するアイテムや複数要求レシピでも、オーバーフローやクラッシュを起こさずに無限精度で計算・描画できる `BigIntMath` 演算エンジンを新設。
  - `BookmarkItemMetadata`、`RecipeChainItem`、`RecipeChainMath` を BigInteger ベースに全面拡張。
- **大きすぎる数字の描画重なり（隣接スロット侵入）の修正**:
  - スロット内のテキスト描画において、テキスト幅がスロット幅を超える場合に自動で縮小スケールを計算して適用するオートスケーリングを導入。
  - 描画開始座標をスロット領域内にクランプし、隣接スロットの数字と重なる視覚的問題を完全に防止。
- **long値範囲の短縮表示の強化**:
  - 単位を K, M, B, T (Tera), P (Peta), E (Exa) まで完全拡張。
  - 100以上では小数を自動省略し、常に3〜4文字以内のスリムな表示を維持。液体・ガスも EB 単位まで網羅。
- **BigInteger における短縮・近似値・指数表記対応**:
  - Exa (E), Zetta (Z), Yotta (Y) 単位に対応。
  - 10^27 以上の超巨大数値に対して、科学的記数法・指数表記（例: 1.2e28）による近似丸め込みを自動適用。
- **CPU使用量・メモリ使用量の軽量化**:
  - 頻出する数値（0〜999）の事前アロケーションキャッシュおよびフォーマット文字列キャッシュを導入し、毎フレームの描画処理に伴うGCプレッシャーとCPU負荷を大幅に削減。
- **マルチスレッド並列計算の導入**:
  - 独立したレシピツリーや折りたたみブロック（Collapsed Blocks）が存在する場合に、安全に並行計算（`parallelStream`）を活用して高速処理。

---

### [15.59.0.212-jeiuu.2] - 2026-11-03
#### 変更 (Changed)
- **バージョニング体系の刷新**:
  - 上流のJEI由来のバージョン番号（15.59.0.212）をベースの印として完全保持し、JEIUU独自の更新を接尾辞 `-jeiuu.<N>` として分離・識別できるように変更。

#### 修正 (Fixed)
- **砥石（Grindstone）レシピ画面のクラッシュバグを修正**:
  - エンチャント除去など下段スロットが空（`ItemStack.EMPTY`）になる砥石レシピにおいて、スロット具材数の不一致により `createFocusLink` で `IllegalArgumentException` がスローされ「このレシピはクラッシュしました」と表示される不具合を修正。
  - `GrindstoneRecipeCategory` にて空スロットの存在を判定し、適切なスロットのみをフォーカスリンクするように改善。
  - `RecipeLayoutBuilder` においてスロット具材数が不一致の場合に例外でレシピ全体をクラッシュさせず、安全にリンクのみをスキップする防護策を追加。

---

### [15.59.0.212-jeiuu.1] - 2026-11-03
#### 追加 (Added)
- **JEI Unofficial Unofficial (JEIUU)** としての初期フォークリリース。
- JEIUU専用の新しいMODアイコンアセット（`jei-icon.png`）を登録。
- 日英バイリンガルのドキュメント（`README.md`, `CHANGELOG.md`, `CONFIG.md`）を追加。
- GPL-3.0 ライセンス定義の統合。

#### 変更 (Changed)
- **Forge 1.20.1 専用化**:
  - Fabric および FabricApi サブプロジェクト、Fabric-Loom等のマルチローダー依存関係を完全に除去。
  - GradleビルドシステムをForge 1.20.1専用にスリム化・最適化。
  - 出力成果物のアーカイブ名を `JEIunofficial-unofficial-1.20.1-forge` に同期。
- **JEI前提要求達成ロジックの強化**:
  - `modId="jei"` として登録し、全JEI依存MODの前提条件を満たす構造を確立。

---

<a name="english"></a>
## English

### Versioning Policy
To clearly trace the upstream JEI / JEIU base version, this mod adopts the following versioning scheme:
[Upstream JEI Version]-jeiuu.[JEIUU Revision] (e.g. 15.59.0.212-jeiuu.2, 15.59.0.212-jeiuu.4-beta3)
- **Upstream JEI Version** (15.59.0.212): Preserves the exact upstream JEI / JEIU release version as a baseline mark.
- **JEIUU Revision** (jeiuu.4-beta3): Distinguishes and tracks fixes, optimizations, and release types (Alpha/Beta/Release) specific to this fork.

---

### [15.59.0.212-jeiuu.4-beta3] - 2026-11-12 (Beta)
#### Fixed
- **Fixed Ingredient Rendering in Crafting Tree GUI**:
  - Added proper `PoseStack` coordinate translation (`translate(x, y, 100)`) and dynamic zoom scaling (`scale`) in `renderTypedIngredient`. Fixed an issue where ingredients were drawn at `(0, 0)` off-screen instead of inside their respective slot boundaries.
  - Adjusted quantity text Z-layer to 300 to strictly prevent clipping behind rendered item stacks.
- **Fixed Recipe Chain Keyboard Trigger (Shift+B / T) Being Consumed**:
  - Replaced the restrictive `InputType.EXECUTE` check in `GroupInputHandler` with `!input.isSimulate()`, allowing immediate keyboard inputs (`InputType.IMMEDIATE`) to correctly launch `CraftingTreeScreen` directly from recipe chains.
- **Prevented Missing Ingredient Instances from Recipe Chains**:
  - Enhanced `TreeChainBridge` to query `BookmarkList.getRecipeChainInputs`, ensuring `ITypedIngredient` instances are fully preserved and populated across all chain nodes. Added dynamic recipe lookup fallback for manually assigned intermediate recipes.

---

### [15.59.0.212-jeiuu.4-beta2] - 2026-11-12 (Beta)
#### Fixed & Added
- **Fixed Crafting Tree Opening from Chains and Bookmarks**:
  - Rebuilt input handling so `Shift + B`, `T`, or **mouse middle-click** reliably opens `CraftingTreeScreen` from any bookmark slot item, recipe chain, or group header.
  - Removed obsolete `RecipeTreeScreen` hooks that blocked event propagation.
- **Direct Tree Creation from Outside Chains (Item List, Recipes GUI, Inventory)**:
  - Hovering over any item in JEI ingredient list, recipe screen (`RecipesGui`), or player inventory and pressing `Shift + B` or `T` now immediately generates and opens a brand-new crafting tree rooted at that item.
- **Manual Root Calculation and Crafting Path Exploration from Scratch**:
  - Start from a target item and right-click intermediate ingredient nodes to choose specific recipes, expanding sub-ingredients dynamically into deeper tree branches.
  - Supports reverting back to raw ingredients, pruning branches via node deletion, and manual quantity overrides.
  - Export custom manually-built trees directly into new bookmark crafting chain groups via the "Save to Chain" button.

---

### [15.59.0.212-jeiuu.4-beta1] - 2026-11-11 (Beta)
#### Added & Improved
- **Ported and Optimized Crafting Tree Feature**:
  - Ported and refactored the interactive Crafting Tree from `nei-features-for-jei` specifically tailored for Minecraft 1.20.1 Forge.
  - **Full BigInteger (bigint) Support**: All requested amounts, craft quantities, outputs, and manual overrides are calculated with infinite-precision `BigInteger`, preventing integer overflows even at massive scales ($10^{20}+$ items/fluids).
  - **Performance Optimization & Frustum Culling**: Implemented viewport boundary checks (`TreeCamera.isWorldRectVisible`) to skip rendering of off-screen nodes and connection lines, maintaining smooth 60fps on large graphs.
  - **Bidirectional Chain & Tree Conversion**:
    - Open an interactive Crafting Tree directly from recipe chains in bookmarks using `Shift + B`, `T`, or mouse middle-click.
    - Export active crafting trees directly into bookmark groups via the "Save to Chain" button.
  - **Alternative Recipe Selection**:
    - Right-click any ingredient node to open `TreeRecipeSelectorScreen` and select custom recipes from available recipe categories.
  - **Manual Quantity & Multiplier Adjustment**:
    - `Shift + Left-click` or `Middle-click` any ingredient node to open `TreeAdjustQuantityScreen` for direct BigInteger quantity entry, quick adjustment (+1, +10, +64, x2, /2), force-craft toggling, and node pruning.
  - **Materials, Intermediates, Byproducts & Machine Statistics**:
    - Bottom analytics panel summarizes missing raw materials, held materials, intermediate crafts, byproducts, and required machines in real-time.
  - **Incremental Search, Pan, and Zoom**:
    - Smooth zooming (0.2x to 5.0x), drag-to-pan camera navigation, and live search bar highlighting matching tree nodes.

---

### [15.59.0.212-jeiuu.3-beta2] - 2026-11-05 (Beta)
#### Fixed & Improved
- **Fixed Crash and Freezes on Tooltip Hover for Huge Fluid/Gas Amounts**:
  - Fixed `ArithmeticException: integer overflow` in `FluidHelper.copyWithAmount` by clamping to `Integer.MAX_VALUE` instead of calling `Math.toIntExact`.
  - Added clamp guards and exception isolation in `ProjectedBookmarkElement.createTooltipIngredient` to completely prevent external mod chemical/fluid renderers from freezing or crashing.
- **Added BigInteger & Long Quantity Display to Bookmark Tooltips**:
  - Added safe, clear bookmark amount info in tooltips (both formatted unit representation and full comma-delimited numbers) on hover for items and fluids.
- **Updated Fluid & Gas Unit to  md (1,000,000 mB)**:
  - Replaced the 1,000-bucket (1,000,000 mB) unit suffix from kB to  md, fully integrated with BigInteger fluid calculations.
- **Prevented Text Overflow in Preview Tooltip Component**:
  - Added dynamic font auto-scaling and slot boundary clamping in `RecipeChainPreviewTooltipComponent`.

---

### [15.59.0.212-jeiuu.3-beta1] - 2026-11-05 (Beta)
#### Added & Improved
- **Recipe Chain BigInteger (bigint) Calculation & Display**:
  - Introduced `BigIntMath` computation engine to calculate and render recipe chains consuming or producing massive amounts of fluids/gases (e.g. near `Long.MAX_VALUE`) without overflow, negative wrapping, or crashes.
  - Fully enhanced `BookmarkItemMetadata`, `RecipeChainItem`, and `RecipeChainMath` with BigInteger precision.
- **Fixed Visual Overlap of Huge Numbers with Adjacent Slots**:
  - Added dynamic text auto-scaling in `IngredientListRenderer` whenever formatted text exceeds the available slot width.
  - Clamped rendering coordinates within slot boundaries to strictly prevent text from overflowing into neighboring slots.
- **Enhanced Compact Rounding for Long Range**:
  - Extended units up to K, M, B, T (Tera), P (Peta), and E (Exa).
  - Automatically omits decimal places when values are >= 100 to strictly maintain a compact 3-4 character footprint. Fluids and chemicals now support up to EB.
- **Aggressive Compact & Exponential Notation for BigInteger**:
  - Added support for Exa (E), Zetta (Z), and Yotta (Y).
  - Massive values >= 10^27 automatically format using compact scientific exponential notation (e.g. 1.2e28).
- **CPU & Memory Allocation Optimization**:
  - Introduced pre-allocated string caches for common small integers (0..999) and a high-frequency format cache to minimize GC pressure and CPU usage during GUI rendering loops.
- **Multithreaded Parallel Computation**:
  - Enabled parallel processing (`parallelStream`) for independent recipe branches and collapsed blocks without shared mutable state.

---

### [15.59.0.212-jeiuu.2] - 2026-11-03
#### Changed
- **New Versioning Scheme**:
  - Preserved the upstream JEI base version (15.59.0.212) intact, separating fork revisions with the `-jeiuu.<N>` suffix.

#### Fixed
- **Fixed Grindstone Recipe Crash**:
  - Fixed an issue where grindstone disenchantment recipes with an empty bottom slot caused `createFocusLink` to throw an `IllegalArgumentException` due to ingredient count mismatch, triggering the "This recipe crashed" error screen.
  - Improved `GrindstoneRecipeCategory` to inspect valid ingredients and only link active slots.
  - Added safety guard in `RecipeLayoutBuilder.createFocusLink` to gracefully skip invalid focus links without crashing the entire recipe layout.

---

### [15.59.0.212-jeiuu.1] - 2026-11-03
#### Added
- Initial fork release of **JEI Unofficial Unofficial (JEIUU)**.
- Integrated custom mod icon asset (`jei-icon.png`).
- Bilingual documentation in English and Japanese (`README.md`, `CHANGELOG.md`, `CONFIG.md`).
- Integrated GPL-3.0 license structure.

#### Changed
- **Forge 1.20.1 Specialization**:
  - Fully removed Fabric and FabricApi subprojects, Fabric-Loom toolchains, and multi-loader overhead.
  - Streamlined and optimized the Gradle build system strictly for Minecraft Forge 1.20.1.
  - Synchronized output archive name to `JEIunofficial-unofficial-1.20.1-forge`.
- **JEI Dependency Satisfaction**:
  - Preserved `modId="jei"` registration ensuring complete compatibility with all JEI-dependent mods and addons.
