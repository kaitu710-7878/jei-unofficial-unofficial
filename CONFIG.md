# CONFIG / コンフィグ設定ガイド

[English](#english) | [日本語](#japanese)

---

<a name="japanese"></a>
## 日本語 (Japanese)

### 設定ファイルの場所
JEIUUの設定は主に以下のファイルで管理されます：
- クライアント設定: `config/jei/jei-client.ini`
- モード設定: `config/jei/jei-mod-id-format.ini`
- キーバインド設定: マインクラフトの「設定」->「操作設定」->「キー割り当て」内

### 主要な設定項目

#### 1. チートモード (`cheatModeEnabled`)
- **データ型**: `boolean` (`true` / `false`)
- **デフォルト値**: `false`
- **説明**: 有効にすると、アイテムリストをクリックした際にレシピ表示ではなくアイテムそのものを入手します。

#### 2. アイテムリストの折りたたみ / 検索設定
- **プレフィックス検索**:
  - `@`: MOD名で検索 (例: `@create`)
  - `#`: ツールチップで検索 (例: `#energy`)
  - `$`: タグで検索 (例: `$forge:ingots`)
  - `%`: クリエイティブタブ名で検索

#### 3. JEIU 拡張機能キー
- **レシピお気に入り登録**: デフォルト `F` キー（レシピ画面上）
- **レシピツリー自動生成**: デフォルト `Shift + F` キー（複数段階のクラフト工程をツリー表示）
- **ブックマークチェーン結合**: ブックマーク欄の括弧をドラッグして複数レシピを合成・倍率計算

#### 4. クラフトツリー（Crafting Tree）操作ガイド
- **ツリーの起動**:
  - **作業中ツリーの直接再開**: アイテムをホバーしていない何もない場所で `T` キー（または操作設定の「クラフトツリーを開く/再開」キー）を押すと、直前まで作業していたツリーを保存された状態のまま確実に再開できます（アイテムにホバーして上書きされる心配がありません）。
  - **ブックマーク欄**: アイテムまたはチェーン、グループヘッダー上で `Shift + B` または `T` キー（あるいは **マウス中クリック**）
  - **アイテムリスト / レシピ画面 / インベントリ**: 任意のアイテム上で `Shift + B` または `T` キーを押すと、そのアイテムを目標（ルート）とする新しいクラフトツリーを即座に作成・展開
- **ナビゲーション**:
  - **視点移動（パン）**: 画面背景を左ドラッグ、または **Shift + マウスホイール** で左右スクロール、矢印キー（←/→/↑/↓）や WASD キーで高速移動
  - **親項目への移動**: ツールバーの `H` ボタン（または `H` / `Home` キー）で一番上の目標アイテムに即座にセンタリング
  - **ズーム拡大・縮小**: マウスホイール（0.2倍 〜 5.0倍）
  - **ノードの折りたたみ / 展開**: スロット横の `[+]` / `[-]` ボタンをクリック
  - **全展開 / 全折りたたみ**: 画面右上の `[+]` / `[-]` ボタン
- **手動調整・レシピ指定**:
  - **代替レシピの選択**: 材料ノードを **右クリック** してレシピ候補ダイアログから選択
  - **数量・倍率の手動調整**: 材料ノードを `Shift + 左クリック` または `中クリック` して数量編集・強制クラフト切替・ノード削除
- **統計パネルとチェーン連携**:
  - **インベントリ在庫引当**: 画面上部のトグルボタンで手持ちインベントリのアイテムをツリー要求から自動差し引き
  - **チェーン保存**: 「チェーン保存」ボタンでツリーのクラフト手順をそのままブックマーク欄へ反映
  - **統計パネル**: 画面下部で不足原材料、手持ち原材料、中間作成物、副産物、必要な機械・作業台の数量をリアルタイム集計表示（横スクロール対応）

---

<a name="english"></a>
## English

### Configuration File Locations
JEIUU settings are primarily stored in:
- Client Configuration: `config/jei/jei-client.ini`
- Mod ID Format Configuration: `config/jei/jei-mod-id-format.ini`
- Keybindings: Standard Minecraft Options -> Controls -> Key Binds

### Key Parameters

#### 1. Cheat Mode (`cheatModeEnabled`)
- **Data Type**: `boolean` (`true` / `false`)
- **Default**: `false`
- **Description**: When enabled, clicking an item in the item panel gives the item directly instead of showing its recipes.

#### 2. Search Prefixes
- **Prefixes**:
  - `@`: Search by mod name (e.g. `@create`)
  - `#`: Search in tooltips (e.g. `#energy`)
  - `$`: Search by tag (e.g. `$forge:ingots`)
  - `%`: Search by creative tab name

#### 3. JEIU Extended Controls
- **Favorite Recipe**: Default key `F` (on recipe screen)
- **Recipe Tree Generation**: Default key `Shift + F` (visualizes multi-stage craft chains)
- **Bookmark Chaining**: Drag bookmark brackets to merge crafting steps and compute material multipliers.

#### 4. Crafting Tree Controls & Guide
- **Opening the Tree**:
  - **Direct Resume Working Tree**: Press `T` (or the configured `key.jei.openCraftingTree` key) in empty space without hovering over any item to directly resume your saved working tree without risking overwriting it with a new item tree.
  - **From Bookmarks**: Hover over any bookmark item, recipe chain, or group header and press `Shift + B` or `T` (or **Middle-click**).
  - **From Item List / Recipe GUI / Inventory**: Hover over any item and press `Shift + B` or `T` to immediately generate a new crafting tree rooted at that item.
- **Navigation**:
  - **Pan Camera**: Click and drag on background, scroll horizontally with **Shift + Mouse Wheel**, or use Arrow / WASD keys.
  - **Center on Target**: Click `H` in the toolbar (or press `H` / `Home` key) to instantly center camera on the root target item.
  - **Zoom In / Out**: Mouse wheel (0.2x to 5.0x scale).
  - **Collapse / Expand Subtree**: Click the `[+]` / `[-]` toggle badge beside any node.
  - **Expand All / Collapse All**: Use the top-right `[+]` / `[-]` toolbar buttons.
- **Recipe Selection & Manual Adjustments**:
  - **Select Alternative Recipe**: **Right-click** any ingredient node to open the full JEI Recipes GUI, and click the "+" button on your desired recipe to select it for the tree node.
  - **Adjust Quantity / Multiplier**: `Shift + Left-click` or `Middle-click` any node to open the quantity adjustment dialog (supports direct BigInteger input, multiplier adjustment, force craft toggling, and node pruning).
- **Statistics & Chain Integration**:
  - **Inventory Deduction Toggle**: Click "Inventory: Use / Ignore" in top toolbar to subtract player inventory items from needed materials.
  - **Save to Chain**: Click "Save to Chain" to export current tree into bookmark groups.
  - **Analytics Panel**: Bottom bar provides real-time summary of missing raw materials, held materials, intermediate crafts, byproducts, and required crafting stations/machines.
  - **Statistics Label Display Toggle**: Click `Lbl:ON` / `Lbl:OFF` in the toolbar to toggle between inline section headers ("Final Results:", "Missing Raw Ingredients:") and compact icon-only tooltip mode.
  - **Persistent Tree Session**: In-progress crafting trees are automatically preserved across screen closes. Click `New` (Reset) in the toolbar whenever you want to wipe and start fresh.
