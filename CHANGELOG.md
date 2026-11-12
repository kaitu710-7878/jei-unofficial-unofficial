# CHANGELOG / 変更履歴

[English](#english) | [日本語](#japanese)

---

<a name="japanese"></a>
## 日本語 (Japanese)

### バージョニング方針
本MODでは、上流のJEI / JEIU のバージョンを追跡しやすくするため、以下の命名規則を採用しています：
`[上流JEIバージョン]-jeiuu.[JEIUUリビジョン番号]` (例: `15.59.0.212-jeiuu.2`, `15.59.0.212-jeiuu.4-beta9`)
- **上流JEIバージョン** (`15.59.0.212`): ベースとなっているJEI / JEIUの公式バージョンを表します。
- **JEIUUリビジョン** (`jeiuu.4-beta9`): 本フォーク（JEIUU）独自で行われた機能追加・バグ修正・最適化の更新回数およびリリース種別（Alpha/Beta/Release）を表します。

---

### [15.59.0.212-jeiuu.4-beta9] - 2026-11-12 (Beta)
#### 追加・改善 (Added & Improved)
- **核廃棄物・吹込み素材・全ケミカルのmb表記統一**:
  - 1,000mB未満の流体・ケミカル（Mekanism気体、核廃棄物、吹込み素材、顔料、スラリー等）が `.16` などの単位なし小数で表示されていた不具合を解消し、`160mb` や `80mb` と明確に「mb」表記されるよう統一。
  - ツリー画面の全ツールチップ（ノードの要求総数、生産総数、統計バーの各数量）でも流体・ケミカルを単位付き（mb, B, bmb 等）で統一表示。
- **レシピ選択時のカメラ位置・ズーム倍率の完全維持**:
  - レシピ画面（`RecipesGui`）で「＋」ボタンを押してレシピを決定した際、拡大率（ズーム倍率）やカメラ位置が全体表示へリセットされていた問題を修正。作業していたノードの位置をアンカーとして追従し、元の場所・倍率をそのまま維持。
- **非消費アイテム（AE2の金型、触媒、回路等）の要求数量固定**:
  - レシピで消費されない金型（Applied Energistics 2の各種インスクライバー金型 `_press`、鋳型、ダイ、スタンプ、GregTech回路、クラフト残留アイテム、触媒スロット）を自動判定。
  - クラフト実行回数が膨大になっても、非消費アイテムの要求数量を乗算せず「1個（または基本要求数）」に固定。

#### 修正 (Fixed)
- **吹込み素材変換レシピ等の必要素材数量計算バグの修正**:
  - 親レシピの生産量（`outputFactor`、例: 80mb）が親ノードのクラフト実行回数計算（`multiplier`）に正しく反映されず、さらに親の生産量が子ノード自身の生産量へ誤って代入されていたバグを根本解決。
  - 160mbの吹込み素材に対して80mb/個の変換レシピを割り当てた場合、消費素材が正しく「2個」と計算されるよう正常化。

---

### [15.59.0.212-jeiuu.4-beta8] - 2026-11-12 (Beta)
#### 追加・改善 (Added & Improved)
- **大規模ツリーのマルチコア並列処理とキャッシュ最適化**:
  - レシピ候補検索およびUID探索をマルチスレッド（parallel stream）で並列処理化。CPUの全コアを活用し、膨大なレシピが存在する環境でも大規模ツリーを高速に展開・再計算可能に。
  - レシピ情報（入力・出力・生産ファクター）および候補探索結果のインメモリキャッシュ（`ConcurrentHashMap`）を導入し、重複検索オーバーヘッドを劇的に低減。
- **スロット数量テキストの視認性向上と背景ボックス**:
  - スロット内および統計バーの数量テキスト背後に半透明黒背景ボックス（`0xC0000000`）を描画。回路アイテムの番号（Programmed Circuitの「8」など）や複雑なテクスチャとテキストが重なっても、視認性を損なわず鮮明に読めるよう改善。
  - 原材料ノード（葉ノード）でも要求数量が確実に右下に表示されるようフォールバックを適用。
- **Shift + マウスホイールのスクロール速度向上**:
  - 水平パン移動の感度を大幅に引き上げ、大規模ツリーにおける左右の高速スクロール・移動を快適化。
- **「ノードとその枝を削除」の強化とキーボードショートカット対応**:
  - ノードを削除した際、その配下に広がるすべての子孫ノードを再帰的に収集して確実に枝刈り（Prune）するよう修正。手動ツリー・チェーンツリーの双方に対応。
  - ホバー中に `Delete` キーまたは `Backspace` キーを押すだけで、該当ノードとその枝を瞬時に削除可能に。

#### 修正 (Fixed)
- **言語キー未翻訳露出の修正**:
  - `jei.tree.tooltip.amount` および `jei.tree.tooltip.produced` の言語定義（英語、日本語、簡体中文）を追加し、未翻訳キーが表示される問題を解消。
- **液体と気体（リチウム等）の厳格な型分離**:
  - `TreeRecipeBuilder` で `BookmarkIngredientKey` による厳格な型検証（`fluid_stack` と `mekanism.api.chemical.gas.GasStack` の区別）を行い、一致する成分を出力しないレシピを確実に除外。液体リチウムと気体リチウムが内部的に混同される問題を修正。
- **ItemStackのスタックサイズ重複描画バグの修正**:
  - `renderTypedIngredient` でアイテム単体描画用のコピー（count=1）が正しくレンダラーに渡されていなかったバグを修正。バニラのスロット数字「8」とツリーの数量表示「2.1M」が重なる不具合を完全解消。
- **液体・ガス単位のタイポ修正 (`1bmb`)**:
  - 1,000 Buckets (1,000,000 mB) の単位表記を仕様通り `1bmb` に修正（`bmd` タイポの解消）。

---

### [15.59.0.212-jeiuu.4-beta7] - 2026-11-12 (Beta)
#### 追加・改善 (Added & Improved)
- **ツリー専用ホットキーの新設とセッション保持（空中でのTキー再開）**:
  - 画面内の何もない場所（またはアイテム非ホバー時）で `T` キーを押すだけで、直前まで作業していたツリーをそのまま確実に再開できる機能を追加。
  - アイテムをホバーして開く必要がなくなったため、意図せず他のアイテムでツリーの保存済み作業内容が上書き消失する事故を完全に防止。
  - 操作設定（Controls）に「クラフトツリーを開く/再開 (`key.jei.openCraftingTree`)」キーバインドを新設し、好みのキーに割り当て可能に。
  - ブックマークグループから開いたツリーもセッションとして永続保持されるように改善。
- **WASD/矢印キーによるツリーパン操作の改善**:
  - WASDや矢印キーが画面上のボタン選択フォーカスに吸われていた問題を解消。ツリー移動キー入力を最優先で消費し、快適なキーボード操作を実現。
- **Shift + マウスホイールによる水平スクロール有効化**:
  - Shiftキーを押しながらホイールを回すことで、ツリー画面を左右にスムーズにパン移動できるよう修正。

#### 修正 (Fixed)
- **統計パネルのホバー表示が内部名称だった不具合の修正**:
  - 統計パネル（Summary Bar）のアイテムや流体にマウスホバーした際、内部ID（`minecraft:iron_ingot` 等）ではなく正式なローカライズ表示名および詳細ツールチップが表示されるよう修正。
- **統計パネルの数量表示の重なり解消**:
  - 統計パネルのスロットにおいて、桁数の多い数量テキストが隣接スロットやアイコンに食い込んで重なる問題を修正。スロット幅に応じた自動スケール縮小とスロット間隔の拡大を適用。
- **統計パネルの初期生成時に液体やガスの数量が表示されない問題の修正**:
  - ツリー初期生成時にも `BookmarkIngredientAmountResolver` を通じて流体・ガスの要求量（mB）を正確に取得・集計できるよう改修。

---

### [15.59.0.212-jeiuu.4-beta6] - 2026-11-12 (Beta)
#### 追加・改善 (Added & Improved)
- **JEI レシピビューワー画面との連携による直感的なレシピ選択**:
  - ノード右クリック時に専用ダイアログではなく、JEI の標準レシピ画面（`RecipesGui`）を開くよう変更。
  - レシピ画面内の「＋（プラス）」ボタンを押すことで、そのレシピをツリーのノードに即座に選択・採用してツリー画面に自動復帰するシームレスなUIを実現。
- **作業中ツリーのセッション保持（ツリーを閉じても消えない機能）**:
  - 手動探索やカスタマイズ作業中のツリー状態（目標アイテム、選択レシピ、数量、折りたたみ状態、カメラ位置）をセッションとして永続保持。
  - 画面を閉じてインベントリ作業やレシピ確認を行った後でも、作業途中のツリーがそのまま完全に復元されます。
  - トップバーに「リセット（`New`）」ボタンを追加し、いつでもワンクリックで初期状態に戻せるよう改善。
- **大規模ツリーの左右移動・ナビゲーションの劇的快適化**:
  - `Shift + マウスホイール` によるスムーズな水平パン（左右スクロール）を実装。
  - キーボードの矢印キー（←/→/↑/↓）および WASD による自由なパン操作に対応。
  - トップバーに「親項目（目標アイテム）にカメラ移動（`H`）」ボタンを追加（`Home` キーまたは `H` キーでも瞬時にセンタリング可能）。

#### 修正 (Fixed)
- **大規模ツリーにおけるノード重なり・衝突の解消**:
  - `TreeLayout` におけるサブツリー輪郭計算の正規化ロジックを修正。親ノードを中心とした完璧な相対座標系で輪郭を合成・照合するように改修し、階層が深くなったり分岐が増えてもノードが一切重ならないよう是正。
  - ノード間隔（X/Y間隔）を最適化し、密集時の視認性を向上。
- **折りたたみ・展開時の親項目（目標アイテム）の座標ズレ防止**:
  - レイアウト再計算時に目標アイテム（ルートノード）の変位をカメラ位置へ即座にフィードバック追従（アンカー補正）させるよう改善。
  - ノードの開閉や拡張を行っても、画面上での目標アイテムの位置が一切動かずピタッと静止するよう修正。

---

<a name="english"></a>
## English

### Versioning Policy
To clearly trace the upstream JEI / JEIU base version, this mod adopts the following versioning scheme:
`[Upstream JEI Version]-jeiuu.[JEIUU Revision]` (e.g. `15.59.0.212-jeiuu.2`, `15.59.0.212-jeiuu.4-beta9`)
- **Upstream JEI Version** (`15.59.0.212`): Preserves the exact upstream JEI / JEIU release version as a baseline mark.
- **JEIUU Revision** (`jeiuu.4-beta9`): Distinguishes and tracks fixes, optimizations, and release types (Alpha/Beta/Release) specific to this fork.

---

### [15.59.0.212-jeiuu.4-beta9] - 2026-11-12 (Beta)
#### Added & Improved
- **Unified "mb" Formatting for Nuclear Waste, Infusions & All Chemicals**:
  - Fixed sub-bucket amounts (< 1,000 mB) displaying as bare decimals (e.g., `.16`) by standardizing on `mb` units (e.g., `160mb`, `80mb`).
  - Standardized tooltips across the tree screen (required total, produced total, summary bar amounts) to always display typed units (mb, B, bmb, etc.).
- **Camera Pan & Zoom Preservation on Recipe Selection**:
  - Selecting a recipe in JEI via the `+` button now retains the active zoom level and anchors camera tracking to the modified node, eliminating jarring camera resets to fit-view.
- **Non-Consumable Catalysts & Molds Handling (AE2 Presses, Molds, GT Circuits)**:
  - Automatically identifies non-consumable items (AE2 inscriber presses, molds, casts, dies, stamps, GregTech circuits, crafting remainders, and catalyst slots).
  - Keeps requirements fixed at 1 (or base requirement) without multiplying by recipe execution multiplier.

#### Fixed
- **Sub-Ingredient Multiplier Calculation for Partial Yield Recipes**:
  - Fixed a math bug where parent recipe output factors (e.g. 80mb yield) were erroneously mapped onto child output amounts rather than parent craft multipliers.
  - Correctly calculates input item demand (e.g. producing 160mb from an 80mb/item recipe accurately demands 2 items instead of 160).

---

### [15.59.0.212-jeiuu.4-beta8] - 2026-11-12 (Beta)
#### Added & Improved
- **Multi-Core Parallel Processing & Cache Optimization for Massive Trees**:
  - Multi-threaded recipe candidate lookups and UID resolution using parallel streams. Utilizes all CPU cores to calculate and expand massive recipe trees instantly in heavy modpacks.
  - Introduced in-memory concurrent caches (`ConcurrentHashMap`) for recipe metadata, slots, outputs, and candidate lookups, eliminating redundant drawable creation overhead.
- **Improved Slot Amount Visibility & Dark Background Box**:
  - Added semi-transparent dark background boxes (`0xC0000000`) behind quantity text in slots and the stats bar. Solves visual overlapping with complex textures and item labels (such as GregTech Programmed Circuit configuration numbers).
  - Ensured leaf/raw material nodes display their total required amounts correctly.
- **Enhanced Horizontal Pan Speed with Shift + Scroll**:
  - Significantly increased Shift + Mouse Wheel scroll speed for swift horizontal navigation across large crafting trees.
- **Pruning & Branch Deletion Fixes with Keyboard Shortcut**:
  - Deleting a node now recursively gathers and removes all descendant child nodes across both manual and bookmark chain trees.
  - Added `Delete` and `Backspace` hotkeys to directly remove the hovered node and its entire branch.

#### Fixed
- **Missing Tooltip Translation Keys**:
  - Added translations for `jei.tree.tooltip.amount` and `jei.tree.tooltip.produced` in English, Japanese, and Simplified Chinese.
- **Strict Separation of Fluids and Gases (e.g. Lithium)**:
  - Enforced strict ingredient type and UID matching via `BookmarkIngredientKey`, properly distinguishing between `fluid_stack` and `mekanism.api.chemical.gas.GasStack`.
- **Duplicate ItemStack Count Rendering**:
  - Fixed `renderTypedIngredient` passing the original multi-count item instead of a single item copy, preventing vanilla item stack numbers from overlapping with tree totals.
- **Fluid & Gas Unit Typo (`1bmb`)**:
  - Corrected 1,000 Buckets (1,000,000 mB) abbreviation from `bmd` to `bmb`.

---

### [15.59.0.212-jeiuu.4-beta7] - 2026-11-12 (Beta)
#### Added & Improved
- **Dedicated Crafting Tree Hotkey & Session Resume (`key.jei.openCraftingTree`)**:
  - Added dedicated keybind to resume working crafting tree anytime from anywhere without needing to hover over an item.
  - Prevents accidental overwriting of in-progress trees when opening the tree.
- **Optimized Keyboard WASD & Arrow Key Panning**:
  - Ensured tree panning consumes WASD / Arrow keys with priority over widget focus.
- **Shift + Mouse Wheel Horizontal Scroll Support**:
  - Smooth lateral camera panning across the tree canvas.

#### Fixed
- **Stats Panel Tooltip Displaying Internal IDs**:
  - Resolved localized item/fluid display names and full tooltips in the summary bar.
- **Overlapping Quantity Text in Stats Bar**:
  - Automatic scaling and enlarged slot spacing prevent text collision.
- **Initial Fluid and Gas Demand Counting**:
  - Accurate calculation of fluid and gas units (mB/B) upon initial tree generation.

---

### [15.59.0.212-jeiuu.4-beta6] - 2026-11-12 (Beta)
#### Added & Improved
- **JEI Recipes GUI Integration for Recipe Selection**:
  - Right-clicking a node now opens JEI's standard `RecipesGui`. Clicking the `+` button directly binds the recipe to the crafting tree node and returns seamlessly.
- **Crafting Tree Session Persistence**:
  - Working tree state (target item, custom recipe choices, manual quantities, collapsed branches, camera pan/zoom) is persistently retained even across GUI closes.
  - Added `New` reset button to restore default layout anytime.
- **Smooth Navigation Across Massive Trees**:
  - Horizontal camera panning with `Shift + Mouse Wheel`.
  - Directional panning with Arrow keys and WASD.
  - `H` / `Home` button to instantly center the view on the root target item.

#### Fixed
- **Resolved Node Overlapping in Massive Trees**:
  - Overhauled subtree contour calculation in `TreeLayout` to prevent overlap regardless of tree depth or branch width.
- **Eliminated Camera Jumping on Collapse/Expand**:
  - Re-anchored camera tracking to target item root node during layout updates.

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
