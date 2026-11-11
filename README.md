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
  - スロット内の数値描画オートスケーリング、短縮丸め込み（K, M, B, T, P, E, Z, Y, 指数表記）、流体専用単位（bmd等）に対応。
- **JEI前提要求達成ロジックの内蔵**:
  - `modId="jei"` として登録されるため、JEIを前提MODとして要求するアドオンや外部MOD（Create, Mekanism, AE2, Jade, EMI等）の依存関係チェックをそのまま完全にパスします。
  - 公式JEI API（`mezz.jei.api.*`）を完全内包しており、互換性を損ないません。
- **JEIUの強力な追加機能**:
  - `F` キーによるレシピお気に入り登録、`Shift + F` による多層クラフトツリー自動生成。
  - レシピブックマークの結合・倍率計算機能（GTNH NEI着想）。
  - アイテムリストの折りたたみ機能。
  - AE2などの大規模技術MODとの高度な連携。
- **専用アセット**: JEIUU専用のアイコンを同梱。

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
  - Dynamic font auto-scaling in slots, compact rounding (K, M, B, T, P, E, Z, Y, scientific notation), and fluid units (`bmd`).
- **Built-in JEI Dependency Satisfaction**:
  - Registered as `modId="jei"` to automatically satisfy mod loader dependency checks for add-ons (Create, Mekanism, AE2, Jade, EMI, etc.).
  - Preserves standard JEI API compatibility (`mezz.jei.api.*`).
- **Powerful Features from JEIU**:
  - Recipe favorites via `F`, recipe tree generation via `Shift + F`.
  - GTNH NEI-inspired bookmark combining and multiplier calculations.
  - Collapsible item lists.
  - Advanced integration with large tech mods such as AE2.
- **Dedicated Assets**: Bundles dedicated JEIUU mod icons.

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
