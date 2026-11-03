# CHANGELOG / 変更履歴

[English](#english) | [日本語](#japanese)

---

<a name="japanese"></a>
## 日本語 (Japanese)

### [15.59.0.214] - 2026-11-03
#### 修正 (Fixed)
- **砥石（Grindstone）レシピ画面のクラッシュバグを修正**:
  - エンチャント除去など下段スロットが空（`ItemStack.EMPTY`）になる砥石レシピにおいて、無効なアイテムがスロットから除外された結果、スロット間の具材数不一致により `createFocusLink` で `IllegalArgumentException` がスローされ「このレシピはクラッシュしました」と表示される不具合を修正。
  - `GrindstoneRecipeCategory` にて空スロットの存在を判定し、適切なスロットのみをフォーカスリンクするように改善。
  - `RecipeLayoutBuilder` においてスロット具材数が不一致の場合に例外でレシピ全体をクラッシュさせず、安全にリンクのみをスキップする防護策を追加。

---

### [15.59.0.213] - 2026-11-03
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

### [15.59.0.214] - 2026-11-03
#### Fixed
- **Fixed Grindstone Recipe Crash**:
  - Fixed an issue where grindstone disenchantment recipes with an empty bottom slot caused `createFocusLink` to throw an `IllegalArgumentException` due to ingredient count mismatch, triggering the "This recipe crashed" error screen.
  - Improved `GrindstoneRecipeCategory` to inspect valid ingredients and only link active slots.
  - Added safety guard in `RecipeLayoutBuilder.createFocusLink` to gracefully skip invalid focus links without crashing the entire recipe layout.

---

### [15.59.0.213] - 2026-11-03
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
