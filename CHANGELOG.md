# CHANGELOG / 変更履歴

[English](#english) | [日本語](#japanese)

---

<a name="japanese"></a>
## 日本語 (Japanese)

### バージョニング方針
本MODでは、上流のJEI / JEIU のバージョンを追跡しやすくするため、以下の命名規則を採用しています：
`[上流JEIバージョン]-jeiuu.[JEIUUリビジョン番号]` (例: `15.59.0.212-jeiuu.2`)
- **上流JEIバージョン** (`15.59.0.212`): ベースとなっているJEI / JEIUの公式バージョンを表します。
- **JEIUUリビジョン** (`jeiuu.2`): 本フォーク（JEIUU）独自で行われたバグ修正や最適化の更新回数を表します。

---

### [15.59.0.212-jeiuu.2] - 2026-11-03
#### 変更 (Changed)
- **バージョニング体系の刷新**:
  - 上流のJEI由来のバージョン番号（`15.59.0.212`）をベースの印として完全保持し、JEIUU独自の更新を接尾辞 `-jeiuu.<N>` として分離・識別できるように変更。

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
- 日英バイリンガルのドキュメント（README.md, CHANGELOG.md, CONFIG.md）を追加。
- GPL-3.0 ライセンス定義の統合。

#### 変更 (Changed)
- **Forge 1.20.1 専用化**:
  - `Fabric` および `FabricApi` サブプロジェクト、Fabric-Loom等のマルチローダー依存関係を完全に除去。
  - GradleビルドシステムをForge 1.20.1専用にスリム化・最適化。
  - 出力成果物のアーカイブ名を `JEIunofficial-unofficial-1.20.1-forge` に同期。
- **JEI前提要求達成ロジックの強化**:
  - `modId="jei"` として登録し、全JEI依存MODの前提条件を満たす構造を確立。

---

<a name="english"></a>
## English

### Versioning Policy
To clearly trace the upstream JEI / JEIU base version, this mod adopts the following versioning scheme:
`[Upstream JEI Version]-jeiuu.[JEIUU Revision]` (e.g. `15.59.0.212-jeiuu.2`)
- **Upstream JEI Version** (`15.59.0.212`): Preserves the exact upstream JEI / JEIU release version as a baseline mark.
- **JEIUU Revision** (`jeiuu.2`): Distinguishes and tracks fixes and improvements specific to this fork.

---

### [15.59.0.212-jeiuu.2] - 2026-11-03
#### Changed
- **New Versioning Scheme**:
  - Preserved the upstream JEI base version (`15.59.0.212`) intact, separating fork revisions with the `-jeiuu.<N>` suffix.

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
- Bilingual documentation in English and Japanese (README.md, CHANGELOG.md, CONFIG.md).
- Integrated GPL-3.0 license structure.

#### Changed
- **Forge 1.20.1 Specialization**:
  - Fully removed `Fabric` and `FabricApi` subprojects, Fabric-Loom toolchains, and multi-loader overhead.
  - Streamlined and optimized the Gradle build system strictly for Minecraft Forge 1.20.1.
  - Synchronized output archive name to `JEIunofficial-unofficial-1.20.1-forge`.
- **JEI Dependency Satisfaction**:
  - Preserved `modId="jei"` registration ensuring complete compatibility with all JEI-dependent mods and addons.
