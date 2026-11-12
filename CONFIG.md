# CONFIG / コンフィグ設定ガイド

[English](#english) | [日本語](#japanese)

---

<a name="japanese"></a>
## 日本語 (Japanese)

### 設定ファイルの場所
JEIUUの設定は主に以下のファイルで管理されます：
- クライアント設定: `config/jei/jei-client.ini`
- モード設定: `config/jei/jei-mod-id-format.ini`
- キーバインド設定: マインクラフトの「設定」>「操作設定」>「キー割り当て」

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
- **ブックマークチェーン結合**: ブックマーク枠の括弧をドラッグして複数レシピを合算・倍率計算

#### 4. クラフトツリー（Crafting Tree）操作ガイド
- **ツリーの起動**:
  - **作業中ツリーの直接再開**: アイテムをホバーしていない何もない場所で `T` キー（または操作設定の「クラフトツリーを開く/再開」キー）を押すと、直前まで作業していたツリーを保存された状態のまま確実に再開できます。
  - **ブックマーク欄**: アイテムまたはチェーン、グループのツリー上で `Shift + B` または `T` キー（あるいは **マウス中クリック**）
  - **アイテムリスト / レシピ画面 / インベントリ**: 任意のアイテム上で `Shift + B` または `T` キーを押すと、そのアイテムを目標（ルート）とする新しいクラフトツリーを即座に作成・展開
- **ナビゲーション**:
  - **視点移動（パン）**: 画面背景ドラッグ、または **Shift + マウスホイール** で左右スクロール、矢印キー（←/→/↑/↓）や WASD キーで高速移動
  - **親項目への移動**: ツールバーの `H` ボタン（または `H` / `Home` キー）で一番上の目標アイテムに瞬時にセンタリング
  - **ズーム拡大・縮小**: マウスホイール（0.2倍 〜 5.0倍）
  - **ノードの折りたたみ / 展開**: スロット下の `[+]` / `[-]` ボタンクリック
  - **全展開 / 全折りたたみ**: 画面右上の `[+]` / `[-]` ボタン
- **手動調整・レシピ指定**:
  - **代替レシピの選択**: 材料ノードを **右クリック** してレシピ画面を開き、使いたいレシピの「＋（プラス）」ボタンでツリーに適用
  - **数量・倍率の手動調整**: 材料ノードを `Shift + 左クリック` または `中クリック` して数量編集・強制クラフト切替・ノード削除
  - **ノードとその枝を削除**: ノードにマウスホバーして `Delete` キーまたは `Backspace` キーを押すだけで、そのノードと配下の枝を瞬時に削除（枝刈り）
- **統計パネルとチェーン連携**:
  - **インベントリ在庫考慮**: 画面上部のトグルボタンで手持ちインベントリのアイテムをツリー必要数から自動控除
  - **チェーン保存**: 「チェーン保存」ボタンでツリーのクラフト工程をそのままブックマーク欄へ反映
  - **統計パネル**: 画面下部で不足原材料、手持ち原材料、中間作成品、副産物、必要な機械・作業台の数量をリアルタイム集計表示（横スクロール対応、ラベル常時表示切替対応）

---

<a name="english"></a>
## English

### Configuration File Locations
JEIUU configurations are mainly stored in:
- Client Settings: `config/jei/jei-client.ini`
- Mod ID Format: `config/jei/jei-mod-id-format.ini`
- Key Bindings: Minecraft Options > Controls > Key Binds

### Key Configuration Options

#### 1. Cheat Mode (`cheatModeEnabled`)
- **Data Type**: `boolean` (`true` / `false`)
- **Default**: `false`
- **Description**: When enabled, clicking an item in the ingredient list gives the item instead of displaying its recipes.

#### 2. Ingredient List Search Prefixes
- **Search Prefixes**:
  - `@`: Mod name (e.g. `@create`)
  - `#`: Tooltip (e.g. `#energy`)
  - `$`: Tag (e.g. `$forge:ingots`)
  - `%`: Creative tab name

#### 3. JEIU Extension Keys
- **Bookmark Recipe**: Default `F` key on recipe layouts.
- **Auto-Generate Crafting Tree**: Default `Shift + F` key to open multi-step crafting hierarchy.
- **Bookmark Chain Linking**: Drag group braces in the bookmark panel to merge multiple recipes and calculate scaling.

#### 4. Crafting Tree Controls & Guide
- **Opening the Tree**:
  - **Resume In-Progress Tree**: Press `T` (or the custom key in Controls) anywhere on an empty screen or non-hovered item to instantly resume your working tree.
  - **From Bookmark Panel**: `Shift + B`, `T`, or **Middle-Click** on an item or chain group.
  - **From Ingredient List / Recipe View / Inventory**: Press `Shift + B` or `T` over any item to create and explore a new crafting tree from that root.
- **Navigation**:
  - **Panning**: Drag canvas background, or **Shift + Mouse Wheel** for horizontal panning, or Arrow / WASD keys.
  - **Center on Root Item**: Click `H` on the top bar or press `H` / `Home` to re-center onto the root item.
  - **Zoom In / Out**: Mouse Wheel (0.2x to 5.0x zoom).
  - **Collapse / Expand Branch**: Click `[+]` / `[-]` button below any branch node.
  - **Expand / Collapse All**: Click `[+]` / `[-]` button in the top control bar.
- **Manual Customization & Recipe Assignment**:
  - **Pick Alternate Recipe**: **Right-Click** any node to view all recipes in JEI's Recipe GUI, then click `+` to assign it to the node.
  - **Adjust Quantity & Multiplier**: `Shift + Left-Click` or `Middle-Click` to customize amounts, toggle force craft, or remove nodes.
  - **Prune / Remove Node & Branch**: Hover over any node and press `Delete` or `Backspace` to prune that node and all its sub-branches immediately.
- **Summary Panel & Chain Integration**:
  - **Inventory Matching**: Toggle top bar button to deduct in-stock items from tree requirements.
  - **Save to Bookmarks**: Click "Save" to export the entire tree structure to a bookmark recipe chain.
  - **Summary Bar**: Real-time aggregation of missing raw materials, intermediate crafts, byproducts, and required machines.
