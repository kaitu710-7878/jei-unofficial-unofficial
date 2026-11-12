# JEI Unofficial Unofficial (JEIUU)

[English](#english) | [日本語](#japanese)

---

<a name="japanese"></a>
## 日本語 (Japanese)

### 概要
**JEI Unofficial Unofficial (JEIUU)** は、StardustMINUS氏による「JEI unofficial (JEIU)」をベースに、**Minecraft 1.20.1 Minecraft Forge 専用** に特化・最適化・更新した非公式フォークMODです。

互換性が高く最低限の機能のJEIをベースに、NEI-GTNH等の便利機能を追加した設計思想を堅持し、基礎となるJEIの仕様やAPIを破壊することなく、1.20.1 Forge環境に最適化されたアップデート・バグ修正・BigInteger拡張・クラフトツリー機能を提供します。

### 特徴
- **Minecraft 1.20.1 Forge 専用設計**: 不要なマルチローダー依存性を排除し、ビルド構成および実行環境を軽量化。
- **NEI Features for JEI 由来の高度なインタラクティブ・クラフトツリー**:
  - `nei-features-for-jei` から本格的なクラフトツリー画面（Crafting Tree Screen）を移植・リファクタリング。
  - **最適化・軽量化**: 画面外ノードや接続線のフラスタムカリング（描画スキップ）、幾何レイアウトキャッシュにより、大規模ツリーでも高速・軽快に動作。
  - **チェーンツリー間の双方向相互変換**: ブックマークグループ内のレシピチェーンから直接ツリーを展開でき、ツリー画面で編集したレシピや数量をそのままブックマークレシピチェーンへ保存・エクスポート可能。
  - **レシピ指定機能**: ツリー内の各ノードを右クリックすることで、代替レシピ一覧から好みのレシピを個別に指定・切り替え可能。
  - **手動調整機能**: Shift+左クリックまたは中クリックで、要求数量やクラフト倍率を直接入力・編集可能。クイックボタン（+1, -1, +10, -10, +64, x2, /2 等）や在庫引当を無視する「強制クラフト」トグル、不要ノードの自動剪定・削除に対応。
  - **下部統計パネル**: 不足原材料、手持ち原材料、中間クラフト、完成目標物、機械・ハンドラ別のクラフト実行回数をリアルタイム集計。
- **BigInteger (bigint) 完全対応**:
  - クラフトツリーおよびレシピチェーンのすべての計算エンジン・数量フィールドを `BigInteger` 任意精度演算に拡張。
  - `Long.MAX_VALUE` を超える超膨大な流体・ガス・アイテム要求や倍率計算でもオーバーフローやクラッシュを起こさず、高精度に計算・描画。
  - スロット内の数値描画オートスケーリング、SI接頭辞+mb表記（Kmb, Mmb, Gmb, Tmb, Pmb, Emb, Zmb, Ymb 等）、科学表記（指数表記）に対応。
- **JEI前提要求達成ロジックの内蔵**:
  - `modId="jei"` として登録されるため、JEIを前提MODとして要求するアドオンや外部MOD（Create, Mekanism, AE2, Jade, EMI等）の依存関係チェックをそのまま完全にパスします。
  - 公式JEI API（`mezz.jei.api.*`）を完全内包しており、互換性を損ないません。
- **JEIUの強力な追加機能**:
  - `F` キーによるレシピお気に入り登録、`Shift + F` による多層クラフトツリー自動生成。
  - レシピブックマークの結合・倍率計算機能（GTNH NEI着想）。
  - アイテムリストの折りたたみ機能。
  - AE2などの大規模技術MODとの高度な連携。
- **専用アセット**: JEIUU専用のアイコンを同梱。

### クラフトツリー（レシピツリー）機能の使い方と詳細
NEI-GTNH および `nei-features-for-jei` の優れた設計思想を取り入れた、高度な対話型クラフト計算ツリーです。

#### 1. ツリーの起動
- **キー操作**: アイテムやJEIのレシピ上で `Shift + F` を押すか、専用キーバインド（設定可能: `key.jei.openCraftingTree`）を押すことで、対象アイテムのクラフトツリー画面を開きます。
- **ブックマークグループ連携**: ブックマーク一覧内のグループにあるクラフトモード（ツリーアイコン）ボタンを押すことで、ブックマークしたレシピチェーンから直接ツリーを展開できます。

#### 2. キャンバスのナビゲーション
- **パン（画面移動）**: 画面上のドラッグ（左ドラッグ / 右ドラッグ）、またはキーボードの `W`, `A`, `S`, `D` / 矢印キーで自由にカメラを移動できます。
- **ズーム（拡大・縮小）**: マウスホイールで直感的に拡大・縮小できます。
- **高速水平移動**: `Shift + マウスホイール` でツリー全体を素早く横方向にスクロールできます。
- **自動調整（Fit）**: 左上の「Fit」ボタンを押すことで、ツリー全体が画面内に綺麗に収まるよう自動調整されます。

#### 3. レシピの選択と自由なカスタマイズ
- **レシピの個別指定・変更**:
  - 各ノードを **右クリック** すると、JEI標準のレシピプレビュー画面（Recipes GUI）が開きます。
  - 目的のレシピを選択し、右下のプラスボタン（`+`）を押すだけで、該当ノードのレシピを即座に変更・上書きできます。
- **サイクリング・タグ素材の選択保持**:
  - 色付きガラスや木材タグ、鉱石辞書など、複数の代替素材が存在するレシピスロットにおいて、レシピ画面上でスロットをスクロールして表示させた特定素材（例: 赤色ガラス）を選択した状態で `+` ボタンを押すと、その素材がツリーに固定・反映されます。
- **数量・倍率の手動調整**:
  - ノードを **Shift + 左クリック** または **マウス中クリック** すると調整ダイアログが開きます。
  - 必要数量やクラフト回数（倍率）を任意精度（BigInteger）で直接数値入力できるほか、クイックボタン（`+1`, `-1`, `+10`, `-10`, `+64`, `-64`, `x2`, `/2` 等）で素早く増減できます。
  - **強制クラフト（Force Craft）**: プレイヤーの手持ちインベントリ在庫を無視して全量を中間素材から製造計算するトグルスイッチを搭載しています。
- **枝の剪定（削除）と再選択**:
  - 間違った中間工程や不要な枝は、ノードを選択して `Delete` または `Backspace` キーを押す（または調整ダイアログの「枝を削除」）ことで簡単に剪定できます。
  - 削除したノードは「未加工の葉ノード」として画面上に保持されるため、いつでも右クリックから別のレシピを再割り当てしてツリーを修正・再構築できます。
- **枝の折りたたみ**: ノード左上の `+/-` トグルで、巨大なサブツリーを折りたたんで画面を整理できます。

#### 4. 下部統計パネル（統計サマリー）
画面下部のパネルでは、ツリー全体の所要資源と製造工程がリアルタイムに集計されます：
- **不足原材料（Missing）**: 手持ちインベントリで不足している最終的な基本素材一覧。
- **在庫原材料（Inventory）**: プレイヤーの所持品から引き当てられた素材一覧。
- **中間生産物（Intermediates）**: 途中の加工段階で製造される中間アイテム・部品一覧。
- **完成目標物（Output）**: 最終的に完成する目標生産物の一覧。
- **作業機械・工程（Operations）**: 各種機械や作業台ごとの総クラフト実行回数。
- **単位表記**: アイテム個数だけでなく、流体・ガス・化学物質（Mekanism等）もミリバケツ基準（`mb`, `Kmb`, `Mmb`, `Gmb`, `Tmb`, `Pmb`, `Emb`, `Zmb`, `Ymb`）および超巨大数量の指数表記（例: `1.2e28mb`）で整然と表示されます。

#### 5. セッション永続化とレシピチェーン保存
- **自動保存**: 画面を閉じたりゲームを再起動しても、作業中ツリーの状態（選択したレシピ、素材バリエーション、数量、カメラ位置）は自動的にセッションファイルに保存され、次回も中断した状態からそのまま再開できます。
- **チェーンエクスポート（保存）**: 画面上の「保存」ボタンを押すことで、完成したツリーをJEIのブックマークレシピチェーン（Bookmark Group）として出力できます。下位工程（原材料）から最終製品へと正しい工程順序でエクスポートされるため、そのままゲーム内での製造ガイドとして活用できます。

### バージョニング・更新方針
- **バージョニング規則**:
  - **表記形式**: `<上流JEIバージョン>-jeiuu.<JEIUUリビジョン>` (例: `15.59.0.212-jeiuu.4-beta1`)
  - **上流JEIバージョン**: ベース元のJEI/JEIUバージョン（例: `15.59.0.212`）を識別子として保持。
  - **JEIUUリビジョン**: 本フォーク独自で行われたバグ修正や最適化の更新番号。
- **上流JEIのバージョン更新**:
  - JEIUおよびJEIUUの機能・互換性を維持したまま、大元となる公式JEIのバージョンを追従・更新する場合があります。

### リクエスト・フィードバックについて
- 改善要望・新機能のリクエストや不具合報告は積極的に歓迎・検討いたします。
- ※本プロジェクトの開発にはAIを活用しているため、**AIの使用量・利用枠の制約により即座に対応できない場合があります**。あらかじめご理解・ご了承いただけますと幸いです。

### 導入手順
1. Minecraft 1.20.1 用の Minecraft Forge（推奨: 47.4.23以上）をインストールします。
2. 本MOD（`JEIunofficial-unofficial-1.20.1-forge-<version>.jar`）を `.minecraft/mods` フォルダに配置します。
3. **注意**: 公式の `JEI` や他の `JEIU` JARファイルとは競合するため、同時に導入しないでください。

### ライセンス
本プロジェクトは、元プロジェクトのライセンス整合性を保つマルチライセンス構成です：
- 原則ライセンス: GNU General Public License v3.0 (GPL-3.0)
- JustEnoughItems 元部分: MIT License (c) 2014-2015 mezz
- GTNH NotEnoughItems 移植部分: LGPL-3.0-or-later (c) 2019-2024 mitchej123 and the GTNH Team
- NEI Features for JEI 移植部分: LGPL-3.0-or-later (c) 2024-2026
- JEIU 追加部分: (c) 2026 StardustMINUS and the Stellar Remnants Team
- JEIUU 追加・改変部分: (c) 2026 Antigravity / GPL-3.0

---

<a name="english"></a>
## English

### Overview
**JEI Unofficial Unofficial (JEIUU)** is an unofficial fork of "JEI unofficial (JEIU)" by StardustMINUS, dedicated, updated, and optimized exclusively for **Minecraft 1.20.1 Minecraft Forge**.

Built on top of a highly compatible JEI foundation enriched with convenient GTNH NEI-style capabilities, JEIUU preserves the fundamental JEI core architecture without intrusive changes, while providing specialized Forge 1.20.1 bug fixes, BigInteger calculations, and a high-performance Crafting Tree system.

### Features
- **1.20.1 Forge Dedicated**: Cleaned from multi-loader toolchains and optimized strictly for Forge 1.20.1.
- **Advanced Interactive Crafting Tree (from NEI Features for JEI)**:
  - Ported and thoroughly refactored from `nei-features-for-jei` into a modern 1.20.1 GUI canvas.
  - **Optimization & Lightweight Rendering**: Frustum culling for off-screen nodes and lines, geometry caching, and minimal GC allocations.
  - **Bidirectional Chain-Tree Conversion**: Open crafting trees directly from bookmark recipe chains and save/export modified tree trees back into bookmark chains.
  - **Recipe Specification**: Right-click any node to select alternate crafting recipes from all available recipes producing that item.
  - **Manual Quantity Adjustments**: Shift+Left Click or Middle-Click to open the adjustment dialog for direct BigInteger quantity/multiplier input, quick step buttons (+1, -1, +10, -10, +64, x2, /2), Force Craft toggle (ignoring inventory stock), and branch pruning.
  - **Bottom Statistics Panel**: Live breakdown of missing raw ingredients, inventory stock, intermediate crafts, final results, and machine operations.
- **Full BigInteger (bigint) Support**:
  - Built-in arbitrary-precision arithmetic across the crafting tree engine and recipe chains to eliminate integer overflows or crashes, even when handling numbers beyond `Long.MAX_VALUE`.
  - Dynamic font auto-scaling in slots, SI prefix + mb formatting (`Kmb`, `Mmb`, `Gmb`, `Tmb`, `Pmb`, `Emb`, `Zmb`, `Ymb`), and exponential scientific notation for massive amounts.
- **Built-in JEI Dependency Satisfaction**:
  - Registered as `modId="jei"` to automatically satisfy mod loader dependency checks for add-ons (Create, Mekanism, AE2, Jade, EMI, etc.).
  - Preserves standard JEI API compatibility (`mezz.jei.api.*`).
- **Powerful Features from JEIU**:
  - Recipe favorites via `F`, recipe tree generation via `Shift + F`.
  - GTNH NEI-inspired bookmark combining and multiplier calculations.
  - Collapsible item lists.
  - Advanced integration with large tech mods such as AE2.
- **Dedicated Assets**: Bundles dedicated JEIUU mod icons.

### Crafting Tree (Recipe Tree) Guide & Usage
An advanced, interactive crafting tree calculator inspired by NEI-GTNH and `nei-features-for-jei`.

#### 1. Opening the Tree
- **Keybindings**: Hover over an item or JEI recipe and press `Shift + F`, or press the dedicated keybinding (configurable: `key.jei.openCraftingTree`), to launch the crafting tree screen.
- **Bookmark Group Integration**: Click the crafting mode (tree icon) button in any bookmark group to directly expand the tree from your saved recipe chain.

#### 2. Canvas Navigation
- **Panning**: Drag with left or right mouse button, or use keyboard keys `W`, `A`, `S`, `D` / Arrow keys to freely move around the canvas.
- **Zooming**: Use the mouse wheel to zoom in and out smoothly.
- **Fast Horizontal Scrolling**: Hold `Shift + Mouse Wheel` to pan horizontally across large trees.
- **Auto-Fit View**: Click the **Fit** button in the top toolbar to automatically scale and center the entire tree on screen.

#### 3. Recipe Customization & Interactive Editing
- **Recipe Reassignment**:
  - **Right-click** any node in the tree to open JEI's standard Recipes GUI.
  - Navigate to the desired recipe category and click the plus button (`+`) to bind that recipe to the node and re-expand the tree.
- **Cycling / Tag Variant Selection**:
  - For recipes containing cycling or tag slots (such as colored glass, dye variants, or ore tags), scroll the slot in JEI to select your preferred variant, then press `+` to lock that specific ingredient into the crafting tree.
- **Manual Quantity & Multiplier Adjustments**:
  - **Shift + Left Click** or **Middle Click** any node to open the adjustment dialog.
  - Directly enter custom quantities or multipliers using arbitrary-precision arithmetic (`BigInteger`), or use quick step buttons (`+1`, `-1`, `+10`, `-10`, `+64`, `-64`, `x2`, `/2`).
  - **Force Craft**: Toggle to calculate total raw ingredients from scratch while ignoring current player inventory stock.
- **Pruning (Deleting) & Rebuilding Branches**:
  - Select a node and press `Delete` or `Backspace` (or use the dialog button) to remove unwanted sub-crafts or incorrect branches.
  - Pruned nodes remain on the tree canvas as uncrafted leaf slots, allowing you to freely right-click them at any time to assign new or corrected recipes.
- **Branch Collapsing**: Toggle the `+/-` icon at the top-left of any node to neatly fold up massive sub-trees.

#### 4. Bottom Statistics Panel
The expandable stats bar at the bottom provides a live breakdown of materials and operations:
- **Missing**: Raw base materials required that are not present in your inventory.
- **Inventory**: Materials fulfilled directly from the player's inventory.
- **Intermediates**: Components, parts, and fluids produced during intermediate stages.
- **Output**: The target finished products and their total yields.
- **Operations**: Total crafting operations per machine / crafting station.
- **Units**: Formatted clearly in items, standard millibuckets for fluids/chemicals/gases (`mb`, `Kmb`, `Mmb`, `Gmb`, `Tmb`, `Pmb`, `Emb`, `Zmb`, `Ymb`), or scientific exponential notation (e.g. `1.2e28mb`).

#### 5. Session Persistence & Bookmark Recipe Chain Export
- **Automatic Persistence**: All customized trees, chosen recipes, cycling variants, multipliers, and camera positions are automatically saved to `config/jei/crafting_tree_session.json` and persist seamlessly across screen closes and game restarts.
- **Chain Export**: Click the **Save** button in the top toolbar to export the crafting tree into a JEI bookmark recipe chain group. Recipes are saved in correct bottom-up engineering order (raw materials first, final product last).

### Versioning Policy
- **Naming Scheme**: `<Upstream-JEI-Version>-jeiuu.<JEIUU-Revision>` (e.g. `15.59.0.212-jeiuu.4-beta1`)
  - **Upstream JEI Version**: Represents the upstream base JEI/JEIU version (e.g., `15.59.0.212`).
  - **JEIUU Revision**: Represents JEIUU-specific bug fixes, enhancements, and optimizations.

### Requests & Feedback
- Feature requests, bug reports, and feedback are actively welcomed.
- *Note: As this project utilizes AI in its development workflow, immediate turnaround may be subject to AI quota availability.*

### Installation
1. Install Minecraft Forge for Minecraft 1.20.1 (recommended: 47.4.23 or newer).
2. Place `JEIunofficial-unofficial-1.20.1-forge-<version>.jar` into your `.minecraft/mods` directory.
3. **Note**: Do not install official `JEI` or other `JEIU` JARs simultaneously.

### License
This project maintains a dual/multi-license structure:
- Primary License: GNU General Public License v3.0 (GPL-3.0)
- JustEnoughItems: MIT License (c) 2014-2015 mezz
- GTNH NotEnoughItems: LGPL-3.0-or-later (c) 2019-2024 mitchej123 and the GTNH Team
- NEI Features for JEI: LGPL-3.0-or-later (c) 2024-2026
- JEIU Additions: (c) 2026 StardustMINUS and the Stellar Remnants Team
- JEIUU Additions & Modifications: (c) 2026 Antigravity / GPL-3.0
