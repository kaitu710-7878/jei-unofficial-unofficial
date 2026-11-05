# CHANGELOG / 変更履歴

[English](#english) | [日本語](#japanese)

---

<a name="japanese"></a>
## 日本語 (Japanese)

### バージョニング方針
本MODでは、上流のJEI / JEIU のバージョンを追跡しやすくするため、以下の命名規則を採用しています：
[上流JEIバージョン]-jeiuu.[JEIUUリビジョン番号] (例: 15.59.0.212-jeiuu.2, 15.59.0.212-jeiuu.3-beta1)
- **上流JEIバージョン** (15.59.0.212): ベースとなっているJEI / JEIUの公式バージョンを表します。
- **JEIUUリビジョン** (jeiuu.3-beta1): 本フォーク（JEIUU）独自で行われたバグ修正や最適化の更新回数およびリリース種別（Alpha/Beta/Release）を表します。

---

### [15.59.0.212-jeiuu.3-beta2] - 2026-11-05 (Beta)
#### 修正・改善 (Fixed & Improved)
- **流体・ガスの巨大数量ホバー時クラッシュおよびフリーズの修正**:
  - FluidHelper.copyWithAmount において Math.toIntExact による整数オーバーフロー例外（ArithmeticException: integer overflow）を Integer.MAX_VALUE クランプ処理へ修正。
  - ProjectedBookmarkElement でのツールチップ用流体スタック生成時に安全クランプと例外防護を追加し、外部MODのツールチップ生成処理におけるフリーズ・クラッシュを完全に防止。
- **ブックマークツールチップへの BigInteger / long 数量行の追加**:
  - アイテム・液体・ガスのブックマークスロットにホバーした際、ツールチップに安全かつ正確な数量表示（短縮フォーマットおよびカンマ区切りの実数表記）を追加。
- **液体・ガスの単位表記を md (1,000,000 mB) へ変更**:
  - 1,000 バケツ（1,000,000 mB）を表す流体単位表記を kB から md に更新。BigInteger の流体量とも一貫して統合。
- **プレビューツールチップの文字重なり防止**:
  - RecipeChainPreviewTooltipComponent の数量テキスト描画に動的オートスケーリングと境界クランプを適用。

---

### [15.59.0.212-jeiuu.3-beta2] - 2026-11-05 (Beta)
#### Fixed & Improved
- **Fixed Crash and Freezes on Tooltip Hover for Huge Fluid/Gas Amounts**:
  - Fixed ArithmeticException: integer overflow in FluidHelper.copyWithAmount by clamping to Integer.MAX_VALUE instead of calling Math.toIntExact.
  - Added clamp guards and exception isolation in ProjectedBookmarkElement.createTooltipIngredient to completely prevent external mod chemical/fluid renderers from freezing or crashing.
- **Added BigInteger & Long Quantity Display to Bookmark Tooltips**:
  - Added safe, clear bookmark amount info in tooltips (both formatted unit representation and full comma-delimited numbers) on hover for items and fluids.
- **Updated Fluid & Gas Unit to md (1,000,000 mB)**:
  - Replaced the 1,000-bucket (1,000,000 mB) unit suffix from kB to md, fully integrated with BigInteger fluid calculations.
- **Prevented Text Overflow in Preview Tooltip Component**:
  - Added dynamic font auto-scaling and slot boundary clamping in RecipeChainPreviewTooltipComponent.

---

### [15.59.0.212-jeiuu.3-beta1] - 2026-11-05 (Beta)
#### 追加・改善 (Added & Improved)
- **レシピチェーンの BigInteger (bigint) 計算・表示対応**:
  - Long.MAX_VALUE 付近の液体やガスを大量に消費・生産するアイテムや複数要求レシピでも、オーバーフローやクラッシュを起こさずに無限精度で計算・描画できる BigIntMath 演算エンジンを新設。
  - BookmarkItemMetadata、RecipeChainItem、RecipeChainMath を BigInteger ベースに全面拡張。
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
  - 独立したレシピツリーや折りたたみブロック（Collapsed Blocks）が存在する場合に、安全に並行計算（parallelStream）を活用して高速処理。

---

### [15.59.0.212-jeiuu.2] - 2026-11-03
#### 変更 (Changed)
- **バージョニング体系の刷新**:
  - 上流のJEI由来のバージョン番号（15.59.0.212）をベースの印として完全保持し、JEIUU独自の更新を接尾辞 -jeiuu.<N> として分離・識別できるように変更。

#### 修正 (Fixed)
- **砥石（Grindstone）レシピ画面のクラッシュバグを修正**:
  - エンチャント除去など下段スロットが空（ItemStack.EMPTY）になる砥石レシピにおいて、スロット具材数の不一致により createFocusLink で IllegalArgumentException がスローされ「このレシピはクラッシュしました」と表示される不具合を修正。
  - GrindstoneRecipeCategory にて空スロットの存在を判定し、適切なスロットのみをフォーカスリンクするように改善。
  - RecipeLayoutBuilder においてスロット具材数が不一致の場合に例外でレシピ全体をクラッシュさせず、安全にリンクのみをスキップする防護策を追加。

---

### [15.59.0.212-jeiuu.1] - 2026-11-03
#### 追加 (Added)
- **JEI Unofficial Unofficial (JEIUU)** としての初期フォークリリース。
- JEIUU専用の新しいMODアイコンアセット（jei-icon.png）を登録。
- 日英バイリンガルのドキュメント（README.md, CHANGELOG.md, CONFIG.md）を追加。
- GPL-3.0 ライセンス定義の統合。

#### 変更 (Changed)
- **Forge 1.20.1 専用化**:
  - Fabric および FabricApi サブプロジェクト、Fabric-Loom等のマルチローダー依存関係を完全に除去。
  - GradleビルドシステムをForge 1.20.1専用にスリム化・最適化。
  - 出力成果物のアーカイブ名を JEIunofficial-unofficial-1.20.1-forge に同期。
- **JEI前提要求達成ロジックの強化**:
  - modId="jei" として登録し、全JEI依存MODの前提条件を満たす構造を確立。

---

<a name="english"></a>
## English

### Versioning Policy
To clearly trace the upstream JEI / JEIU base version, this mod adopts the following versioning scheme:
[Upstream JEI Version]-jeiuu.[JEIUU Revision] (e.g. 15.59.0.212-jeiuu.2, 15.59.0.212-jeiuu.3-beta1)
- **Upstream JEI Version** (15.59.0.212): Preserves the exact upstream JEI / JEIU release version as a baseline mark.
- **JEIUU Revision** (jeiuu.3-beta1): Distinguishes and tracks fixes, optimizations, and release types (Alpha/Beta/Release) specific to this fork.

---

### [15.59.0.212-jeiuu.3-beta1] - 2026-11-05 (Beta)
#### Added & Improved
- **Recipe Chain BigInteger (bigint) Calculation & Display**:
  - Introduced BigIntMath computation engine to calculate and render recipe chains consuming or producing massive amounts of fluids/gases (e.g. near Long.MAX_VALUE) without overflow, negative wrapping, or crashes.
  - Fully enhanced BookmarkItemMetadata, RecipeChainItem, and RecipeChainMath with BigInteger precision.
- **Fixed Visual Overlap of Huge Numbers with Adjacent Slots**:
  - Added dynamic text auto-scaling in IngredientListRenderer whenever formatted text exceeds the available slot width.
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
  - Enabled parallel processing (parallelStream) for independent recipe branches and collapsed blocks without shared mutable state.

---

### [15.59.0.212-jeiuu.2] - 2026-11-03
#### Changed
- **New Versioning Scheme**:
  - Preserved the upstream JEI base version (15.59.0.212) intact, separating fork revisions with the -jeiuu.<N> suffix.

#### Fixed
- **Fixed Grindstone Recipe Crash**:
  - Fixed an issue where grindstone disenchantment recipes with an empty bottom slot caused createFocusLink to throw an IllegalArgumentException due to ingredient count mismatch, triggering the "This recipe crashed" error screen.
  - Improved GrindstoneRecipeCategory to inspect valid ingredients and only link active slots.
  - Added safety guard in RecipeLayoutBuilder.createFocusLink to gracefully skip invalid focus links without crashing the entire recipe layout.

---

### [15.59.0.212-jeiuu.1] - 2026-11-03
#### Added
- Initial fork release of **JEI Unofficial Unofficial (JEIUU)**.
- Integrated custom mod icon asset (jei-icon.png).
- Bilingual documentation in English and Japanese (README.md, CHANGELOG.md, CONFIG.md).
- Integrated GPL-3.0 license structure.

#### Changed
- **Forge 1.20.1 Specialization**:
  - Fully removed Fabric and FabricApi subprojects, Fabric-Loom toolchains, and multi-loader overhead.
  - Streamlined and optimized the Gradle build system strictly for Minecraft Forge 1.20.1.
  - Synchronized output archive name to JEIunofficial-unofficial-1.20.1-forge.
- **JEI Dependency Satisfaction**:
  - Preserved modId="jei" registration ensuring complete compatibility with all JEI-dependent mods and addons.