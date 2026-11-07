# JEI Unofficial Unofficial (JEIUU)

[English](#english) | [日本語](#japanese)

---

<a name="japanese"></a>
## 日本語 (Japanese)

### 概要
**JEI Unofficial Unofficial (JEIUU)** は、StardustMINUS氏による「JEI unofficial (JEIU)」をベースに、**Minecraft 1.20.1 Minecraft Forge 専用** に特化・最適化・更新した非公式フォークMODです。

Fabric等の他ローダー向けコードを完全に除外して構成をスリム化しつつ、JEIUが誇る先進的な利便性向上機能（レシピツリー生成、チェーンブックマーク、高速クラフト等）をForge 1.20.1環境で安定して提供します。非公式版ではありますが、コミュニティ向けの継続的な不具合修正・機能改善・最適化アップデートを行っています。

### 特徴
- **Minecraft 1.20.1 Forge 専用設計**: 不要なマルチローダー依存性を排除し、ビルド構成および実行環境を軽量化。
- **BigInteger (bigint) レシピチェーン作成・計算対応**:
  - `Long.MAX_VALUE` を超える超膨大な流体・ガス・アイテム要求や倍率計算でもオーバーフローやクラッシュを起こさず、高精度にレシピチェーンの作成・計算・描画を行う演算エンジンを新設。
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
  - **表記形式**: `<上流JEIバージョン>-jeiuu.<JEIUUリビジョン>` (例: `15.59.0.212-jeiuu.3-beta2`)
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
- JEIU 追加部分: (c) 2026 StardustMINUS and the Stellar Remnants Team
- JEIUU 追加・改変部分: (c) 2026 Antigravity / GPL-3.0

---

<a name="english"></a>
## English

### Overview
**JEI Unofficial Unofficial (JEIUU)** is an unofficial fork of "JEI unofficial (JEIU)" by StardustMINUS, dedicated, updated, and optimized exclusively for **Minecraft 1.20.1 Minecraft Forge**.

By removing Fabric and multi-loader dependencies, the project is streamlined for maximum stability and performance on Forge 1.20.1, while preserving all the powerful features of JEIU (recipe tree generation, bookmark chaining, fast crafting, etc.). Although this is an unofficial fork, it is actively maintained with continuous updates, bug fixes, and performance improvements.

### Features
- **1.20.1 Forge Dedicated**: Cleaned from multi-loader toolchains and optimized strictly for Forge 1.20.1.
- **BigInteger (bigint) Recipe Chain Creation & Calculation**:
  - Built-in arbitrary-precision arithmetic engine to create, calculate, and render recipe chains without integer overflow or crashes, even when recipes require astronomical amounts of items, fluids, or gases exceeding `Long.MAX_VALUE`.
  - Dynamic text auto-scaling in slots, compact rounding (K, M, B, T, P, E, Z, Y, scientific notation), and specialized fluid units (e.g., `bmd`).
- **Built-in JEI Dependency Satisfaction**:
  - Identifies as `modId="jei"`, satisfying dependency requirements for any mods and addons requiring JEI (e.g., Create, Mekanism, Applied Energistics 2, Jade, EMI).
  - Fully embeds and provides official JEI APIs (`mezz.jei.api.*`) with seamless compatibility.
- **Enhanced JEIU Capabilities**:
  - Favorite recipes via `F`, full recipe tree generation via `Shift + F`.
  - Recipe bookmark chaining and multiplier calculation (inspired by GTNH NEI).
  - Item list folding and batch management.
  - Deep integration with AE2 and major tech mods.
- **Custom Brand Asset**: Custom JEIUU icon.

### Versioning & Upstream Updates
- **Versioning Scheme**:
  - **Format**: `<Upstream JEI Version>-jeiuu.<JEIUU Revision>` (e.g. `15.59.0.212-jeiuu.3-beta2`)
  - **Upstream JEI Version**: Intact base version from upstream JEI / JEIU (e.g. `15.59.0.212`) preserved as a landmark.
  - **JEIUU Revision**: Incremented for fork-specific fixes and optimizations.
- **Upstream JEI Updates**:
  - We may update the underlying upstream JEI base version as needed, while strictly preserving all existing JEIU and JEIUU features and compatibility.

### Requests & Feedback
- Feature requests, improvement ideas, and bug reports are actively welcomed and reviewed.
- *Note*: Development is assisted by AI tooling; due to **AI usage quotas and allowance limits, requests may not always be addressed immediately**. We appreciate your understanding and patience.

### Installation
1. Install Minecraft Forge for 1.20.1 (recommended: 47.4.23 or newer).
2. Place `JEIunofficial-unofficial-1.20.1-forge-<version>.jar` into your `.minecraft/mods` folder.
3. **Important**: Do not install alongside the official JEI or standard JEIU to prevent mod ID collisions.

### License
This project follows a multi-license configuration to respect upstream licenses:
- Overall Project: GNU General Public License v3.0 (GPL-3.0)
- Original JustEnoughItems portions: MIT License (c) 2014-2015 mezz
- Adapted GTNH NotEnoughItems portions: LGPL-3.0-or-later (c) 2019-2024 mitchej123 and the GTNH Team
- JEIU original additions: (c) 2026 StardustMINUS and the Stellar Remnants Team
- JEIUU modifications: (c) 2026 Antigravity / GPL-3.0
