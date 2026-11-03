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
