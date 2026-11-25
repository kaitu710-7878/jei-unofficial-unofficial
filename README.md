# JEI Unofficial Unofficial (JEIUU)

[English](#english) | [日本語](#japanese)

---

<a name="english"></a>
## English

### Overview
**JEI Unofficial Unofficial (JEIUU)** is an unofficial fork of "JEI unofficial (JEIU)" by StardustMINUS, dedicated, updated, and optimized exclusively for **Minecraft 1.20.1 Minecraft Forge**.

Preserving JEIU's philosophy of providing rich GTNH NEI-inspired conveniences on top of a lightweight, highly compatible JEI core without breaking baseline APIs, JEIUU delivers specialized Forge 1.20.1 bug fixes, arbitrary-precision `BigInteger` calculations, and an advanced multi-tab interactive Crafting Tree built on JEIU's foundation and enriched with practical features inspired by NEI for JEI.

### Features
- **1.20.1 Forge Dedicated**: Cleaned from multi-loader toolchains and optimized strictly for Forge 1.20.1.
- **Multi-Tab Interactive Crafting Tree (JEIU-Tree Foundation with NEI for JEI Enhancements)**:
  - **Multi-Tab Workspace (Up to 16 Tabs)**: Manage up to 16 independent crafting tree tabs simultaneously in the top tab bar, including multiple independent tabs for the same target item (e.g., comparing progression-unlocked recipes or alternate routes).
  - **Tab Context Menu & Safe Tab Limit Handling**: Right-click any tab to **Duplicate**, **Reset**, or **Close** it. When opening or duplicating a tree at the 16-tab limit, a warning dialog lets you choose which existing tab to delete (or cancel safely without losing any work).
  - **Clearer Readability & Zoom Limits**: Enlarged `20×20` icons in `24×24` slots, always-visible `1` quantity labels, and zoom-scaled limits for amount labels, collapse toggles, and branch lines so zoomed-out views stay clean.
  - **Optimization & Lightweight Rendering**: Frustum culling for off-screen nodes and lines, geometry caching, and minimal GC allocations.
  - **Bidirectional Chain-Tree Conversion**: Open crafting trees directly from bookmark recipe chains and save/export modified trees back into bookmark chains—including custom mod recipes without registered recipe IDs (`getRegistryName == null`).
  - **Recipe Specification & Tag Locking**: Right-click any node to pick alternate recipes from JEI's Recipes GUI; cycling/tag selections remain deterministically locked across restarts.
  - **Manual Quantity Adjustments**: Shift+Left Click or Middle-Click to open the adjustment dialog for direct `BigInteger` quantity/multiplier input, quick step buttons (`+1`, `-1`, `+10`, `-10`, `+64`, `x2`, `/2`), Force Craft toggle (ignoring inventory stock), and single/identical branch pruning.
  - **Bottom Statistics Panel**: Live breakdown of missing raw ingredients, inventory stock, intermediate crafts, final results, and machine operations.
- **Full BigInteger (bigint) Support**:
  - Built-in arbitrary-precision arithmetic across the crafting tree engine and recipe chains to eliminate integer overflows or crashes, even when handling numbers beyond `Long.MAX_VALUE`.
  - Dynamic font auto-scaling in slots, SI prefix + mb formatting (`Kmb`, `Mmb`, `Gmb`, `Tmb`, `Pmb`, `Emb`, `Zmb`, `Ymb`), and exponential scientific notation for massive amounts.
- **Broad Mod Compatibility & Built-in JEI Dependency Satisfaction**:
  - Registered as `modId="jei"` to automatically satisfy mod loader dependency checks for add-ons (Create, Mekanism, AE2, GTCEu, Jade, EMI, etc.) while preserving standard JEI API compatibility (`mezz.jei.api.*`).
  - Includes fallback support for legacy 3-argument `IIngredientRenderer.getTooltip` implementations (e.g., GTCEu / LDLib machine recipes) and synthetic UID resolution for custom mod recipes without registry names.
- **Powerful Features from JEIU**:
  - Recipe favorites via `F`, recipe tree generation via `Shift + F`.
  - GTNH NEI-inspired bookmark combining and multiplier calculations.
  - Collapsible item lists and advanced AE2 integration.
- **Dedicated Assets**: Bundles dedicated JEIUU mod icons.

### Crafting Tree (Recipe Tree) Guide & Usage
Built upon the crafting tree foundation of JEIU (JEI unofficial), this feature incorporates practical workflow enhancements inspired by `NEI Features for JEI` and NEI-GTNH into an advanced, interactive crafting tree calculator.

#### 1. Opening & Managing Tabs
- **`T` / `Shift + B`**: Hover over an item, bookmark, or group and press `T` or `Shift + B` to focus its existing crafting tree tab if already open, or create a new tab otherwise.
- **`Shift + T`**: Unconditionally creates a brand-new crafting tree tab for the hovered item/group, even if one is already open.
- **Tab Bar & Right-Click Menu**: Click any tab to switch, middle-click or click `x` to close, or **right-click** a tab to **Duplicate Tab**, **Reset**, or **Close**. Up to 16 tabs fit simultaneously in the tab bar, with full names shown in hover tooltips.

#### 2. Canvas Navigation
- **Panning**: Drag with left or right mouse button, or use `W`, `A`, `S`, `D` / Arrow keys to move around the canvas.
- **Zooming**: Use the mouse wheel to zoom in and out smoothly.
- **Fast Horizontal Scrolling**: Hold `Shift + Mouse Wheel` to pan horizontally across large trees.
- **Auto-Fit & Home**: Click **Fit** to center the entire tree, or press `Home` / `H` to focus the root target.

#### 3. Recipe Customization & Interactive Editing
- **Recipe Reassignment**: **Right-click** any node to open JEI's standard Recipes GUI, then click `+` on a recipe to bind it to that node.
- **Cycling / Tag Variant Selection**: Scroll cycling/tag slots in JEI to pick your preferred variant before pressing `+` to lock that exact ingredient into the tree (persisted across restarts).
- **Manual Quantity & Multiplier Adjustments**: **Shift + Left Click** or **Middle Click** any node to enter custom `BigInteger` quantities/multipliers, use quick step buttons, or toggle **Force Craft** (ignore player inventory stock).
- **Pruning (Deleting) & Rebuilding Branches**: Press `Delete` / `Backspace` on a hovered node to remove that branch (or `Shift + Delete` to remove all identical branches). Removing the last child reverts the parent back to an uncrafted state so you can reassign another recipe cleanly.
- **Branch Collapsing**: Toggle the `+/-` button at the top-left of any node to fold/unfold sub-trees.

#### 4. Bottom Statistics Panel
The expandable stats bar at the bottom provides a live breakdown of materials and operations:
- **Missing**: Raw base materials required that are not present in your inventory.
- **Inventory**: Materials fulfilled directly from the player's inventory.
- **Intermediates**: Components, parts, and fluids produced during intermediate stages.
- **Output**: The target finished products and their total yields.
- **Operations**: Total crafting operations per machine / crafting station.
- **Units**: Formatted clearly in items, millibuckets for fluids/chemicals/gases (`mb`, `Kmb`, `Mmb`, `Gmb`, `Tmb`, `Pmb`, `Emb`, `Zmb`, `Ymb`), or scientific exponential notation (e.g. `1.2e28mb`).

#### 5. Session Persistence & Bookmark Recipe Chain Export
- **Automatic Persistence**: All tabs, chosen recipes, cycling variants, multipliers, and camera positions are automatically saved to `config/jei/crafting_tree_session.json` across screen closes and game restarts.
- **Chain Export**: Click **Save** in the top toolbar to export the crafting tree into a JEI bookmark recipe chain group in bottom-up engineering order (raw materials first, final product last).

### Versioning Policy
- **Naming Scheme**: `<Upstream-JEI-Version>-jeiuu.<JEIUU-Revision>` (e.g. `15.59.0.212-jeiuu.4`)
  - **Upstream JEI Version**: Preserves the upstream base JEI/JEIU version (e.g., `15.59.0.212`).
  - **JEIUU Revision**: Tracks JEIUU-specific updates, bug fixes, and release types (Alpha/Beta/Release).

### Requests & Feedback
- Feature requests, bug reports, and feedback are actively welcomed.
- *Note: As this project utilizes AI in its development workflow, immediate turnaround may be subject to AI quota availability.*

### License
This project maintains a dual/multi-license structure:
- Primary License: GNU General Public License v3.0 (GPL-3.0)
- JustEnoughItems: MIT License (c) 2014-2015 mezz
- GTNH NotEnoughItems: LGPL-3.0-or-later (c) 2019-2024 mitchej123 and the GTNH Team
- NEI Features for JEI Inspirations: LGPL-3.0-or-later (c) 2024-2026
- JEIU Additions: (c) 2026 StardustMINUS and the Stellar Remnants Team
- JEIUU Additions & Modifications: (c) 2026 Antigravity / GPL-3.0

### Installation
1. Install Minecraft Forge for Minecraft 1.20.1 (recommended: 47.4.23 or newer).
2. Place `JEIunofficial-unofficial-1.20.1-forge-<version>.jar` into your `.minecraft/mods` directory.
3. **Note**: Do not install official `JEI` or other `JEIU` JARs simultaneously.

---

<a name="japanese"></a>
## 日本語 (Japanese)

### 概要
**JEI Unofficial Unofficial (JEIUU)** は、StardustMINUS氏による「JEI unofficial (JEIU)」をベースに、**Minecraft 1.20.1 Minecraft Forge 専用** に特化・最適化・更新した非公式フォークMODです。

互換性が高く軽量なJEIをベースにNEI-GTNH等の便利機能を追加したJEIUの設計思想を継承し、基礎となるJEIの仕様やAPIを破壊することなく、1.20.1 Forge環境に最適化されたアップデート・バグ修正・BigInteger拡張、そしてJEIUのツリー機能を基盤にNEI for JEI等の実用的な要素を融合・発展させた複数タブ対応の対話型クラフトツリー機能を提供します。

### 特徴
- **Minecraft 1.20.1 Forge 専用設計**: 不要なマルチローダー依存性を排除し、ビルド構成および実行環境を軽量化。
- **複数タブ対応の対話型クラフトツリー（JEIUツリー基盤 + NEI for JEI着想）**:
  - **マルチタブ管理（最大16タブ）**: 画面上部のタブバーで最大16個の独立したクラフトツリーを同時に管理可能。同一目標アイテムであっても複数タブを作成して進捗別のレシピ比較などができます。
  - **タブ右クリックメニューと上限保護**: タブを右クリックして **タブを複製**・**リセット**・**閉じる** を実行可能。16個の上限到達時に新規作成・複製を行う場合は、自動で古いタブを消さずに警告ダイアログを表示し、削除したい既存タブを指定して続行（またはキャンセル）できます。
  - **視認性向上とズームリミット**: `24×24` スロット・`20×20` アイコンへの大型化、数量 `1` の常時表示、画面縮小時に数量・折りたたみボタン・分岐線がアイコンを覆わないサイズ制限を搭載。
  - **最適化・軽量化**: 画面外ノードや接続線のフラスタムカリング、幾何レイアウトキャッシュにより大規模ツリーでも高速に動作。
  - **チェーン・ツリー間の双方向相互変換**: ブックマークのレシピチェーンからツリーを展開でき、編集したツリーをチェーンとして保存可能。レシピID未登録（`getRegistryName == null`）のカスタム機械レシピでも正しく連動します。
  - **レシピ指定とタグ固定**: ノード右クリックでJEIレシピ画面を開き、`+` ボタンでレシピを個別指定。タグ／サイクリング素材を選択した内容も再起動をまたいで決定的に維持されます。
  - **手動調整機能**: `Shift + 左クリック` または中クリックで `BigInteger` 対応の数量・倍率入力、クイックボタン（`+1`, `-1`, `+10`, `-10`, `+64`, `x2`, `/2` 等）、在庫引当を無視する「強制クラフト」トグル、単体/同種ノードの削除に対応。
  - **下部統計パネル**: 不足原材料、手持ち原材料、中間生産物、完成目標物、機械・ハンドラ別の実行回数をリアルタイム集計。
- **BigInteger (bigint) 完全対応**:
  - クラフトツリーおよびレシピチェーンのすべての計算エンジン・数量フィールドを `BigInteger` 任意精度演算に拡張。
  - `Long.MAX_VALUE` を超える超膨大な要求や倍率計算でもオーバーフローせず高精度に計算・描画。SI接頭辞+mb表記（`Kmb`, `Mmb`, `Gmb`, `Tmb`, `Pmb`, `Emb`, `Zmb`, `Ymb` 等）および指数表記に対応。
- **高い他MOD互換性とJEI前提要求の内蔵**:
  - `modId="jei"` として登録されるため、JEIを前提MODとする外部MOD（Create, Mekanism, AE2, GTCEu, Jade, EMI 等）の依存チェックを完全にパスし、公式JEI API（`mezz.jei.api.*`）の互換性を保ちます。
  - GTCEu / LDLib などの旧3引数版 `IIngredientRenderer.getTooltip` へのフォールバックや、登録名を持たないカスタムレシピへの合成UID対応を内蔵。
- **JEIUの強力な追加機能**:
  - `F` キーによるレシピお気に入り登録、`Shift + F` による多層クラフトツリー自動生成、レシピブックマークの結合・倍率計算、アイテムリストの折りたたみ、AE2連携。
- **専用アセット**: JEIUU専用のアイコンを同梱。

### クラフトツリー（レシピツリー）機能の使い方と詳細

#### 1. ツリーの起動とタブ操作
- **`T` / `Shift + B`**: アイテムやブックマーク、グループ上で `T` または `Shift + B` を押すと、すでにそのアイテムのタブが開いていれば該当タブへ移動し、なければ新規タブを作成します（設定名: `key.jei.openCraftingTree` / `key.jei.showRecipeTree`）。
- **`Shift + T`**: 既存タブの有無にかかわらず、無条件で新しいクラフトツリータブを作成します。
- **タブバーと右クリックメニュー**: クリックでタブ切り替え、中クリックまたは `x` で閉じます。タブを **右クリック** すると「タブを複製」「リセット」「閉じる」メニューが開きます。16個すべてのタブがタブバーに表示され、ホバーでフルネームを確認できます。

#### 2. キャンバスのナビゲーション
- **パン（画面移動）**: ドラッグ（左 / 右）、または `W`, `A`, `S`, `D` / 矢印キーで自由に移動できます。
- **ズーム（拡大・縮小）**: マウスホイールで拡大・縮小します。
- **高速水平移動**: `Shift + マウスホイール` で横方向に素早くスクロールできます。
- **自動調整（Fit） / Home**: 左上の「Fit」ボタンで全体表示、`Home` / `H` キーで目標アイテムへカメラを戻します。

#### 3. レシピの選択と自由なカスタマイズ
- **レシピの個別指定・変更**: 各ノードを **右クリック** してJEIのレシピ画面を開き、目的のレシピの `+` ボタンを押すと、そのノードにレシピを割り当てられます。
- **サイクリング・タグ素材の選択保持**: 複数候補があるスロットでは、JEI画面上で目的の素材を表示させた状態で `+` を押すと、その素材がツリーに固定され再起動後も維持されます。
- **数量・倍率の手動調整**: ノードを **Shift + 左クリック** または **中クリック** すると調整ダイアログが開き、`BigInteger` での直接数値入力やクイック増減、**強制クラフト（在庫無視）** の切り替えができます。
- **枝の剪定（削除）と再選択**: ホバー中のノードで `Delete` / `Backspace`（`Shift + Delete` で同種全削除）を押すと枝を削除できます。最後の子ノードを削除した場合は親ノードが未クラフト状態に戻るため、別のレシピを割り当て直せます。
- **枝の折りたたみ**: ノード左上の `+/-` トグルでサブツリーを折りたためます。

#### 4. 下部統計パネル（統計サマリー）
画面下部のパネルで、不足原材料・所持原材料・中間生産物・完成目標物・副産物・機械別実行回数をリアルタイム集計します。アイテム数に加え、流体・ガスもミリバケツ基準（`mb` 〜 `Ymb`）および指数表記で表示されます。

#### 5. セッション永続化とレシピチェーン保存
- **自動保存**: 全タブの状態（レシピ・タグ選択・数量・カメラ位置）は `config/jei/crafting_tree_session.json` に自動保存され、再起動後もそのまま再開できます。
- **チェーンエクスポート（保存）**: 上部バーの「保存」ボタンで、ツリーをJEIブックマークのレシピチェーンとして原材料から最終製品への正しい順序で保存できます。

### バージョニング・更新方針
- **表記形式**: `<上流JEIバージョン>-jeiuu.<JEIUUリビジョン>` (例: `15.59.0.212-jeiuu.4`)
  - **上流JEIバージョン**: ベース元のJEI/JEIUバージョン（例: `15.59.0.212`）を保持。
  - **JEIUUリビジョン**: 本フォーク独自で行われた機能追加・バグ修正・最適化の更新番号およびリリース種別（Alpha/Beta/Release）。

### リクエスト・フィードバックについて
- 改善要望・新機能のリクエストや不具合報告は積極的に歓迎・検討いたします。
- ※本プロジェクトの開発にはAIを活用しているため、**AIの使用量・利用枠の制約により即座に対応できない場合があります**。あらかじめご理解・ご了承いただけますと幸いです。

### ライセンス
本プロジェクトは、元プロジェクトのライセンス整合性を保つマルチライセンス構成です：
- 原則ライセンス: GNU General Public License v3.0 (GPL-3.0)
- JustEnoughItems 元部分: MIT License (c) 2014-2015 mezz
- GTNH NotEnoughItems 移植部分: LGPL-3.0-or-later (c) 2019-2024 mitchej123 and the GTNH Team
- NEI Features for JEI 参考・着想部分: LGPL-3.0-or-later (c) 2024-2026
- JEIU 追加部分: (c) 2026 StardustMINUS and the Stellar Remnants Team
- JEIUU 追加・改変部分: (c) 2026 Antigravity / GPL-3.0

### 導入手順
1. Minecraft 1.20.1 用の Minecraft Forge（推奨: 47.4.23以上）をインストールします。
2. 本MOD（`JEIunofficial-unofficial-1.20.1-forge-<version>.jar`）を `.minecraft/mods` フォルダに配置します。
3. **注意**: 公式の `JEI` や他の `JEIU` JARファイルとは競合するため、同時に導入しないでください。
