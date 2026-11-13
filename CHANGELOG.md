# CHANGELOG / 変更履歴

[English](#english) | [日本語](#japanese)

---

<a name="japanese"></a>
## 日本語 (Japanese)

### バージョニング方針
本MODでは、上流のJEI / JEIU のバージョンを追跡しやすくするため、以下の命名規則を採用しています：
`[上流JEIバージョン]-jeiuu.[JEIUUリビジョン番号]` (例: `15.59.0.212-jeiuu.2`, `15.59.0.212-jeiuu.4-beta9`)
- **上流JEIバージョン** (`15.59.0.212`): ベースとなっているJEI / JEIUの公式バージョンを表します。
- **JEIUUリビジョン** (`jeiuu.4-beta9`): 本フォーク（JEIUU）独自で行われた機能追加・バグ修正・最適化の更新回数およびリリース種別（Alpha/Beta/Release）を表します。

---

### [15.59.0.212-jeiuu.4-beta25] - 2026-11-13 (Beta)
#### 修正・改善 (Fixed & Improved)
- **画面下部統計バーの「機械・ハンドラ別実行数」アイコンおよびツールチップ描画の修正**:
  - 統計バーの `HANDLER` セクションで `ITypedIngredient` が空だったためスロットアイコンが描画されていなかった問題を修正。JEIの `IRecipeCategory` からカテゴリアイコン（`getIcon()`）または作業台カタリスト（`createRecipeCatalystLookup`）を取得して16×16スロットにフィット描画するように改善。
  - 加工回数・中間クラフト回数が1回の場合でも回数が明示されるようにし、ホバー時のツールチップに機械・カテゴリ名と使用レシピ数を表示するように修正。

---

### [15.59.0.212-jeiuu.4-beta24] - 2026-11-13 (Beta)
#### 修正・改善 (Fixed & Improved)
- **液体・気体アイコンの1px細線化（表示不安定）の根本修正**:
  - `CraftingTreeScreen.renderTypedIngredient` において、レシピ由来の微量な数量（10mB等）をそのまま `FluidTankRenderer` / 化学物質レンダラーへ渡していたため、1000mB容量に対する割合で高さ1ピクセルの帯しか描画されない現象が発生していました。
  - `IIngredientHelper.normalizeIngredient()` による正規化（バケツ・タンク満タン表示）を適用してから描画するように修正し、あらゆる液体・気体・化学物質が16×16スロット全面に安定して表示されるよう改善。
- **操作設定およびAlt長押しショートカット説明への `Shift+B` / `T` キー用途明記**:
  - Minecraftの「操作設定 > キー割り当て」において、`Shift+B`（カーソル下のアイテムから新規展開）と `T`（展開 / 作業中ツリーの再開）の用途を明確化。
  - アイテム、ブックマーク、通常グループ、レシピチェーン、お気に入りレシピ等のすべてのAlt長押しショートカットツールチップ（`BookmarkHotkeyTooltipUtil`）に、`Shift+B` と `T` それぞれの用途を分かりやすく個別表示するように改善。

---

### [15.59.0.212-jeiuu.4-beta23] - 2026-11-13 (Beta)
#### 修正・改善 (Fixed & Improved)
- **金型等の非消費・再利用可能アイテムの必要数計算を修正**:
  - 金型・プレス・ダイス・回路・クラフト残留アイテム（自己返却型）などの非消費アイテムが、ツリー内の複数工程で使用された際に統計計算（`TreeStats`）で足し算（合算）され複数個要求されていた問題を修正。非消費アイテムは複数箇所で使われても最大値（`BigInteger::max`、通常1個）のみを要求するよう変更。
  - `TreeMath.isNonConsumable` の判定を強化し、`stamp` / `shape.` / `extruder_shape` / `integrated_circuit` 等のキーワード、および `BookmarkItemType.NONCONSUMABLE` メタデータ、クラフト残留アイテム（`getCraftingRemainingItem`）も非消費として正しく認識するよう拡張。
- **液体・気体・化学物質のレンダリングズレおよび描画不安定の修正**:
  - `CraftingTreeScreen` および統計バーの描画パスを「背景・枠・接続線 → `g.flush()` → アイコン本体 → 数量オーバーレイ → `g.flush()`」の3パスへ分離し、`Tesselator` を直接使用する流体・気体レンダラーと `GuiGraphics` バッファの描画順逆転や干渉を解消。
  - `renderTypedIngredient` 前後で `g.flush()` と `RenderSystem.setShaderColor(1, 1, 1, 1)` / ブレンド状態のリセットを徹底し、他スロットへの色・ブレンド漏れを防止。
  - ズーム倍率に応じた描画座標の浮動小数点配置とレンダラーサイズ（`getWidth` / `getHeight`）に基づく自動フィット補正を導入し、ズーム時やカスタムサイズの液体・気体アイコンが枠からズレないように修正。

---

### [15.59.0.212-jeiuu.4-beta22] - 2026-11-13 (Beta)
#### 修正・改善 (Fixed & Improved)
- **ガス・液体リチウム等の同一ID・異種別素材の厳密な区別を復元**:
  - `TreeRecipeBuilder.findRecipe` において、同一レシピUID（Mekanismの回転凝縮機等）を共有するガス生成・液体生成レシピを検索する際、出力ターゲットの型（`GasStack` / `FluidStack` 等）の一致検証（`requireTargetMatch`）が抜けて先頭レシピを誤返却していたバグを修正。
  - フォールバック検索でも出力スロットが存在するレシピに対してターゲット型・キーが一致しない場合は確実に除外するよう安全ガードを復元。
  - `TreeMath` のノードパス生成（`prunedPaths` 用）およびループ検出（`visitedRecipes`）を `ingredientUid` 単体や `recipeUid` 単体ではなく、`ingredientTypeUid` を含む `stableKey()` と `BookmarkIngredientKey` の組み合わせで厳密に区別するよう改修。

---

### [15.59.0.212-jeiuu.4-beta21] - 2026-11-13 (Beta)
#### 修正・改善 (Fixed & Improved)
- **WASDおよび矢印キーによる画面移動（パン操作）の反転修正**:
  - `W` / `↑` キーで上方向、`S` / `↓` キーで下方向、`A` / `←` キーで左方向、`D` / `→` キーで右方向へ直感的にカメラが移動するよう符号を修正。またズーム倍率に依存せず一定の画面移動速度になるよう改善。
- **2種類の削除モード（単品削除・同種一括削除）の選択対応**:
  - 数量調整ダイアログに「このノードと枝のみ削除 (`Del`)」ボタンと「同じ項目すべてと枝を削除 (`Shift+Del`)」ボタンの2つを配置し、用途に応じて使い分けられるよう改善。
  - ツリー画面上でのショートカットキーも、`Delete` / `Backspace` 単体押しで「カーソル下のノードと枝のみを単品削除」、`Shift + Delete` / `Shift + Backspace` で「ツリー内の同じアイテムすべてと枝を一括削除」の両方に対応。
- **子項目削除後のレシピ再選択による完全復元修正**:
  - 複数の子項目のうち1つを削除した後、親ノード（または対象ノード）のレシピを選択し直した際に削除した子項目が復活しない問題を解消。
  - 親ノードのレシピ再選択時に配下の削除パス（`prunedPaths`）や除外キー（`prunedKeys` / `uncraftedKeys`）を一括解除し、さらに `R` / `U` キーからJEI画面を開いて親や子のレシピへ遷移してから「+」ボタンを押した場合でも対象ノードを自動特定して確実に元通り復元するよう改善。
  - 初回ツリー生成時と手動レシピ選択時で、同一レシピ内の同じ材料スロットが常に数量合算（集約）されるよう計算ロジックを統一。
- **クラフトツリー全体のリファクタリング・最適化**:
  - `TreeLayout` のカメラ追従アンカー照合をノードパス（`nodePath`）およびキーベースに改修し、再計算時の画面揺れを完全に防止。
  - `TreeRecipeBuilder` で一時的なスロット表示がグローバルキャッシュを汚染しないよう修正。
  - 検索ハイライト判定にツールチップ文字列キャッシュを導入し、大規模ツリー描画時のフレームレートを向上。

---

### [15.59.0.212-jeiuu.4-beta20] - 2026-11-13 (Beta)
#### 修正・改善 (Fixed & Improved)
- **ノード削除の完全消去・パスベース除外（`prunedPaths`）とタグ材料置換の完全対応**:
  - **ノード削除の完全消去**: 従来の「ノード自身を未加工素材として画面上に残す」挙動から、ユーザーの意図通り「対象ノード自身とその枝（子孫ノード）すべてをツリー上から完全に消去（非表示・除外）」する仕様へ刷新。削除操作を行った瞬間に画面から該当項目が綺麗に消去され、統計や必要素材からも正しく除外されるよう修正。
  - **パスベース除外管理（`prunedPaths`）の導入**: ツリー内の階層パス（`nodePath`）に基づく一意な除外判定を導入。別枝にある同一アイテム（鉄インゴットやガラスなど）への巻き添え削除を完全に防止し、グローバルキー除外の競合やゴーストキーによる新レシピ展開の阻害を根絶。
  - **タグ材料置換（ガラスの色切り替え等）の完全対応**: タグレシピなどで選択された材料（例: 無色ガラス）に対し、右クリックレシピ画面から別候補（例: 赤色ガラス）を選択して「+」ボタンを押した際、親ノードの入力材料リスト（`customRecipeInputs`）内の該当アイテムを自動置換し、新材料のレシピを展開する機構を新設。タグレシピでスクロールした任意の色・材料への切り替えが100%確実にツリーに反映されるよう改善。
  - **セッション永続化の強化**: `crafting_tree_session.json` に `prunedPaths` を追加し、ゲームを再起動しても削除した項目が勝手に復活しないよう状態保存を徹底。

---

### [15.59.0.212-jeiuu.4-beta19] - 2026-11-13 (Beta)
#### 修正・改善 (Fixed & Improved)
- **枝削除・レシピ再追加・変更の完全安定化（未加工状態管理 `uncraftedKeys` の導入）**:
  - ノードや枝を削除した際、従来の「子孫キーをグローバルな除外リスト（`prunedKeys`）に登録する」方式を撤廃。対象ノード自身を「レシピ未割り当ての葉ノード（未加工素材）」として明確に管理する `uncraftedKeys` 機構を導入。
  - レシピが解除されたノードの子供たちは自動的に生成されなくなるため、子孫キーを禁止リストへ入れる必要がなくなり、他の枝にある同一アイテムの誤削除や、キー不一致によるブロックが完全に解消。
  - 枝を削除したノードに対して右クリックから同じレシピや別のレシピを何度でも自由に再選択・再構築可能になり、新レシピの入力材料が100%確実にツリーへ展開されるよう抜本的に改善。
  - 既存ノードを削除せずに別のレシピに変更する際も、古い入力材料に惑わされず即座に新しいレシピとその子供たちへスムーズに切り替わるよう修正。
  - セッションファイル（`crafting_tree_session.json`）にも `uncraftedKeys` を完全永続化し、ゲームを再起動しても削除・変更したツリーの状態が崩れず正確に復元されるよう強化。
  - ブックマークグループツリーにおいて、手動設定したレシピやカスタム入力が再計算時に消失していた不具合を修正。

---


### [15.59.0.212-jeiuu.4-beta18] - 2026-11-12 (Beta)
#### 修正・改善 (Fixed & Improved)
- **操作設定・ショートカットキー表示におけるツリー展開操作の複数キー（Shift+B / T）併記対応**:
  - ゲーム内のアイテムやブックマーク、レシピ行にカーソルを合わせた際のツールチップにおいて、ツリー展開キー表示を `Shift + B / T` と併記表示するように改善。
  - Minecraftの「設定 > 操作設定 > キー割り当て」画面におけるツリー関連のキー設定項目名を、「クラフトツリーを展開 (Shift+B)」および「クラフトツリーを展開/再開 (Tキー)」に改め、どちらのキーでもツリー展開操作が可能であることが一目でわかるよう明瞭化。
  - フォーカス入力およびブックマークオーバーレイの入力ハンドラーを強化し、ユーザーが操作設定で割り当てた任意のキー（`openCraftingTree` / `showRecipeTree`）および標準の `T` / `Shift+B` のいずれでも確実にツリーが展開されるよう入力判定を統合。

---

### [15.59.0.212-jeiuu.4-beta17] - 2026-11-12 (Beta)
#### 修正・改善 (Fixed & Improved)
- **ツリーからレシピチェーン変換時のレシピ出力順序を通常順（下位原料→完成品）に統一**:
  - クラフトツリー画面の「保存」機能でツリーをブックマークのレシピチェーン（Bookmark Group）にエクスポートした際、ツリーの探索順（ルート完成品が先頭）のまま登録されてしまい、通常の手順で作成したレシピチェーンと上下が逆（反転）に出力されていた問題を修正。
  - エクスポート時のレシピリストを反転させ、下位工程・原材料レシピから順に並び、最終成果物（完成品）が末尾に配置される通常チェーンと完全に同一の上下順序で登録されるよう改善。

---

### [15.59.0.212-jeiuu.4-beta16] - 2026-11-12 (Beta)
#### 修正・改善 (Fixed & Improved)
- **流体・化学物質・ガスの数量単位表記をSI接頭辞+mbに完全統一**:
  - これまで流体・ガス・化学物質（使用済み核廃棄物等）の数量表記において、`bmb`（1,000,000mb）、`kmb`（1,000mb）、`B`（バケツ）、`MB`（メガバケツ）等の表記が混在・混乱していた問題を全面的に是正。
  - すべての流体・化学物質（Gas, Infusion, Pigment, Slurry等）の数量単位を **`mb`（ミリバケツ）基準** に統一。
  - 1,000以上はK以降のSI接頭辞 + `mb`（`Kmb`, `Mmb`, `Gmb`, `Tmb`, `Pmb`, `Emb`, `Zmb`, `Ymb`）で一貫して表示。
  - SI接頭辞（Yotta / 10^24）を超える超巨大数量については、レシピチェーンと同様に科学表記（例: `1.2e28mb`）で明確に表示。

---

### [15.59.0.212-jeiuu.4-beta15] - 2026-11-12 (Beta)
#### 修正・改善 (Fixed & Improved)
- **Mekanism化学物質（Infusion / Gas等）および冶金吹込み機レシピの完全サポート & 消失防止**:
  - Mekanismの冶金吹込み機（Metallurgic Infuser）等の化学物質（`InfusionStack`, `GasStack` 等）を入力材料に含むレシピにおいて、セッション永続化・復元時（`resolveIngredient`）にデシリアライズ不全が生じて吹込み素材（XPインフュージョン等）がツリーから消失してしまう問題を修正。
  - `BookmarkIngredientKey.resolveIngredient` に `getAllTypedIngredients` および `getUniqueId` によるフォールバック走査機構を導入し、JEIレジストリに登録されたすべての特殊Ingredient（化学物質・非アイテム材料）が確実に復元・再構成されるよう強化。
- **カスタム入力材料（`customRecipeInputs`）のスコープ適正化（固定機械レシピの保護）**:
  - タグレシピ（色付きガラス等）のように複数候補を持つサイクリングスロットが存在する場合のみ `customRecipeInputs` を記録し、冶金吹込み機などの単一固定レシピでは余計な入力上書きを行わないよう制御。
  - セッションファイル読み込み時、万一一部の材料が欠落した不完全なカスタム入力が存在しても、完全な正規レシピの入力材料リスト（`info.inputs()`）へ安全にフォールバックする保護機構を `TreeMath` および `TreeRecipeBuilder` の双方に実装。
- **計算整合性 & 未加工葉ノードのレシピ割り当て強化**:
  - `TreeMath.findRecipeInputs` の数量計算で `Math.max(1, ...)` を徹底し、吹込み素材等で数量0によるクラフト計算不全を防止。
  - 枝削除後の未加工葉ノードに対し、機械レシピを含む任意のレシピを安定して再割り当て可能にした。

---

### [15.59.0.212-jeiuu.4-beta14] - 2026-11-12 (Beta)
#### 修正・改善 (Fixed & Improved)
- **タグレシピのスクロール選択材料の完全保持・ツリー反映**:
  - タグ登録レシピ（色付きガラスや鉱石タグ等）において、レシピスロットをスクロールして特定の材料（赤色ガラス等）を表示させてプラスボタン（`+`）を押した際に、スクロール表示させたアイテムではなく常に先頭のデフォルト候補（白色ガラス等）が登録されてしまう不具合を修正。
  - レシピ転送ボタンコントローラー（`RecipeTransferButtonController`）からレシピレイアウト（`IRecipeLayoutDrawable`）を直接受け取り、画面上にスクロール表示された正確な入力材料（`displayedIngredient`）を抽出・保持する `TreeRecipeBuilder.createFromLayout` および `customRecipeInputs` 管理機構を実装。
  - スクロール選択された材料はツリーセッションファイル（`crafting_tree_session.json`）にも完全永続化され、ゲームを再起動しても選択した色や特定素材が初期化されず完全に復元・維持されるよう改善。
- **枝削除後のノード存置と自由なレシピ再選択・再追加の実現**:
  - 中間ノードや枝に対して「削除」を実行した際に対象ノード自身まで削除除外（`prunedKeys`）に登録されて画面から消滅し、二度と同じノードやレシピを追加できなくなっていた問題を解決。
  - 子ノードの枝削除時は、対象ノード自身を除外とせず、その子孫（子供以降の枝）のみを剪定除外。対象ノード自身は「未加工素材（レシピ未割り当ての葉ノード）」としてツリー上に保持され、いつでも右クリックから同じレシピや別のレシピを何度でも自由に再選択・再構築できるよう修正。
- **ノード削除を行わないレシピの上書き・変更の完全安定化**:
  - ツリー上のノードを削除せずに別のレシピに変更する際、既存のカスタム入力材料と衝突せず即座に新しいレシピおよびその入力材料へスムーズに上書き更新されるよう改善。

---

### [15.59.0.212-jeiuu.4-beta13] - 2026-11-12 (Beta)
#### 修正・改善 (Fixed & Improved)
- **枝削除後のレシピ再選択・ツリー反映の正常化（動的アンプレーン機能）**:
  - 間違ったノードや枝を削除した後に、親ノードまたは目的ノードで正しいレシピを選択して再構築しようとしても、削除時に登録された剪定除外キー（`prunedKeys`）に阻まれて新しいレシピの材料がツリーに反映されない不具合を解決。
  - レシピを選択した際、対象ノードおよび新規選択レシピが必要とする入力材料群（および設定済み子孫）を `prunedKeys` から自動的に解除（アンプレーン）する機構を導入し、修正した枝が正しくツリーへ展開・反映されるよう改善。
- **レシピの確実な上書き・適用保証（出力マッチング判定の緩和 & 手動優先）**:
  - ツリー上のノードを削除せずに別のレシピへ変更しようとしても、JEIレシピ探索エンジン（`findRecipe`）における出力の完全一致判定（NBTやタグ差異）に阻まれてレシピ取得が null と判定され、上書きできない問題を解決。
  - 出力判定に `matchesCraftingAvailable` を導入し、さらにユーザーが手動選択したレシピについては出力照合失敗時でも最低限 `factor = 1` で有効レシピとして採用。
  - ブックマークグループ（`groupId >= 0`）から開いたツリーにおいても、手動選択レシピをブックマークチェーンの古いメタデータより最優先で適用し、100%確実にレシピが上書きされるよう改善。
  - レシピ適用後に保留中ターゲット（`pendingRecipeTarget`）を正常にクリアし、後続の不正上書きを防止。
- **ゲーム再起動時の削除状態の完全永続化 & セッション復元の堅牢化**:
  - ゲーム再起動時に `TypedIngredientSerializer` が `UidContext.Recipe` 起因でデシリアライズに失敗し、セッション全体のロードが破棄されて削除した項目がすべて復活してしまう不具合を根本修正。
  - 目標アイテムのシリアライズに `BookmarkIngredientKeySerializer`（SNBT・型・UID完全保持）を導入し、起動直後でも確実に同一アイテムとして認識・復元。削除状態（`prunedKeys`）が再起動後も完全に保持され、削除した項目が勝手に復活しないよう徹底。

---

### [15.59.0.212-jeiuu.4-beta12] - 2026-11-12 (Beta)
#### 修正・改善 (Fixed & Improved)
- **ゲーム再起動時のツリーセッション自動保存・完全復元（永続化）**:
  - メモリ上のみで保持されていたツリーセッションがゲーム再起動によって消失し、再起動後にツリーを開くとブックマーク先頭のレシピチェーンツリーが開かれてしまう不具合を修正。
  - セッション情報を設定ディレクトリ内の `config/jei/crafting_tree_session.json` へJSON形式で自動保存・永続化。ゲームを再起動しても作業途中のツリー（目標アイテム/数量、手動選択レシピ、削除した枝、カメラ座標、ズーム倍率等）が直前の状態のまま完全に復元されるよう改善。
- **加圧反応室（PRC）の液体登録の正常化 & スロット名除外（"extra"等）の完全撤廃**:
  - Mekanismの加圧反応室（Pressurized Reaction Chamber）において、水などの液体入力スロット名が `"extra"` であったため、beta11のスロット名除外処理により液体入力が消失・登録不能になっていた不具合を修正。
  - beta9〜11で行われていた名指しスロット除外処理（"extra" 等）を完全に撤廃し、beta8のクリーンなスロット抽出ロジックへ復帰。加圧反応室の液体を含むすべての正当な入力スロットが正常に登録されるよう改善。
- **リチウム（気体）と液体リチウム（流体）の完全型分離と混同防止**:
  - 同一ID（`mekanism:lithium`）を持つ気体と液体がレシピ選択やツリー展開時に同一視されていた問題を修正。
  - レシピUIDキャッシュを目的成分型（`BookmarkIngredientKey`）ごとに分離し、`findRecipe` で目的の型（気体 vs 液体）を出力しないレシピが誤採用されないよう厳格な一致検証を徹底。

---

### [15.59.0.212-jeiuu.4-beta11] - 2026-11-12 (Beta)
#### 修正・改善 (Fixed & Improved)
- **吹込み変換レシピおよび冶金吹込み機レシピの完全正常化**:
  - レシピ解析エンジンを安定していた beta8 ベースの設計に巻き戻し、Mekanismの「Extra」スロットのみを精密に除外する方式へ改修。
  - ガスタンクや不要な吹込み変換アイテムが冶金吹込み機のクラフト入力に混ざる不具合を解消。
  - 機械内部で行われる「アイテムから吹き込み素材への変換レシピ（Infusion Conversion）」が登録できなくなっていた不具合を完全解決。
- **副作用のない吹込み素材クラフト回数計算の適用**:
  - ノード自身が選んだレシピの生産量（`outputFactor`、例: 80mb）に基づいて自身のクラフト回数を計算し、子ノードには親の数値を引き継がせない（初期値1）ことで、吹込み素材A・素材B・素材Cの多段階層でも必要素材数が異常増殖しない本質的な修正を実装。

---

### [15.59.0.212-jeiuu.4-beta10] - 2026-11-12 (Beta)
#### 修正・改善 (Fixed & Improved)
- **補填情報アイテム（ガスタンク等）のツリー材料除外（不安定、専用対策のため非推奨。beta12でスロット名除外を完全撤廃し正常化）**:
  - Mekanismの冶金注入器や各種化学機械において、吹込み素材や気体・流体を機械に補填するための補助表示（"Extra" スロット、ガスタンク・ケミカルタンク等）がクラフトツリーの入力材料として誤って組み込まれる問題を修正。
  - レシピ内に該当する気体・吹込み素材・流体等のケミカル入力が存在する場合、またはJEIスロット名が補助/充填用（extra, container, info, charge, fuel等）である場合、これらのコンテナ・補填用アイテムをツリーのクラフト材料から自動除外。
- **「このノードとその枝を削除」の確実な実行と再発防止**:
  - `TreeMath` の計算エンジン内部に剪定除外キー（`prunedKeys`）の追跡機構を導入。
  - ルート選定、ノード構築（`buildNode`）、動的レシピ展開（`findRecipeInputs`）の全フェーズで徹底した剪定ガードを適用し、削除したノードやその枝が再計算や動的探索によってツリーに再生成・復活してしまうバグを根本解決。

---

### [15.59.0.212-jeiuu.4-beta9] - 2026-11-12 (Beta)
#### 追加・改善 (Added & Improved)
- **核廃棄物・吹込み素材・全ケミカルのmb表記統一**:
  - 1,000mB未満の流体・ケミカル（Mekanism気体、核廃棄物、吹込み素材、顔料、スラリー等）が `.16` などの単位なし小数で表示されていた不具合を解消し、`160mb` や `80mb` と明確に「mb」表記されるよう統一。
  - ツリー画面の全ツールチップ（ノードの要求総数、生産総数、統計バーの各数量）でも流体・ケミカルを単位付き（mb, B, bmb 等）で統一表示。
- **レシピ選択時のカメラ位置・ズーム倍率の完全維持**:
  - レシピ画面（`RecipesGui`）で「＋」ボタンを押してレシピを決定した際、拡大率（ズーム倍率）やカメラ位置が全体表示へリセットされていた問題を修正。作業していたノードの位置をアンカーとして追従し、元の場所・倍率をそのまま維持。
- **非消費アイテム（AE2の金型、触媒、回路等）の要求数量固定**:
  - レシピで消費されない金型（Applied Energistics 2の各種インスクライバー金型 `_press`、鋳型、ダイ、スタンプ、GregTech回路、クラフト残留アイテム、触媒スロット）を自動判定。
  - クラフト実行回数が膨大になっても、非消費アイテムの要求数量を乗算せず「1個（または基本要求数）」に固定。

#### 修正 (Fixed)
- **吹込み素材変換レシピ等の必要素材数量計算バグの修正（不安定、専用対策のため非推奨。beta11/12で本質的修正へ刷新）**:
  - 親レシピの生産量（`outputFactor`、例: 80mb）が親ノードのクラフト実行回数計算（`multiplier`）に正しく反映されず、さらに親の生産量が子ノード自身の生産量へ誤って代入されていたバグを根本解決。
  - 160mbの吹込み素材に対して80mb/個の変換レシピを割り当てた場合、消費素材が正しく「2個」と計算されるよう正常化。

---

### [15.59.0.212-jeiuu.4-beta8] - 2026-11-12 (Beta)
#### 追加・改善 (Added & Improved)
- **大規模ツリーのマルチコア並列処理とキャッシュ最適化**:
  - レシピ候補検索およびUID探索をマルチスレッド（parallel stream）で並列処理化。CPUの全コアを活用し、膨大なレシピが存在する環境でも大規模ツリーを高速に展開・再計算可能に。
  - レシピ情報（入力・出力・生産ファクター）および候補探索結果のインメモリキャッシュ（`ConcurrentHashMap`）を導入し、重複検索オーバーヘッドを劇的に低減。
- **スロット数量テキストの視認性向上と背景ボックス**:
  - スロット内および統計バーの数量テキスト背後に半透明黒背景ボックス（`0xC0000000`）を描画。回路アイテムの番号（Programmed Circuitの「8」など）や複雑なテクスチャとテキストが重なっても、視認性を損なわず鮮明に読めるよう改善。
  - 原材料ノード（葉ノード）でも要求数量が確実に右下に表示されるようフォールバックを適用。
- **Shift + マウスホイールのスクロール速度向上**:
  - 水平パン移動の感度を大幅に引き上げ、大規模ツリーにおける左右の高速スクロール・移動を快適化。
- **「ノードとその枝を削除」の強化とキーボードショートカット対応**:
  - ノードを削除した際、その配下に広がるすべての子孫ノードを再帰的に収集して確実に枝刈り（Prune）するよう修正。手動ツリー・チェーンツリーの双方に対応。
  - ホバー中に `Delete` キーまたは `Backspace` キーを押すだけで、該当ノードとその枝を瞬時に削除可能に。

#### 修正 (Fixed)
- **言語キー未翻訳露出の修正**:
  - `jei.tree.tooltip.amount` および `jei.tree.tooltip.produced` の言語定義（英語、日本語、簡体中文）を追加し、未翻訳キーが表示される問題を解消。
- **液体と気体（リチウム等）の厳格な型分離**:
  - `TreeRecipeBuilder` で `BookmarkIngredientKey` による厳格な型検証（`fluid_stack` と `mekanism.api.chemical.gas.GasStack` の区別）を行い、一致する成分を出力しないレシピを確実に除外。液体リチウムと気体リチウムが内部的に混同される問題を修正。
- **ItemStackのスタックサイズ重複描画バグの修正**:
  - `renderTypedIngredient` でアイテム単体描画用のコピー（count=1）が正しくレンダラーに渡されていなかったバグを修正。バニラのスロット数字「8」とツリーの数量表示「2.1M」が重なる不具合を完全解消。
- **液体・ガス単位のタイポ修正 (`1bmb`)**:
  - 1,000 Buckets (1,000,000 mB) の単位表記を仕様通り `1bmb` に修正（`bmd` タイポの解消）。

---

### [15.59.0.212-jeiuu.4-beta7] - 2026-11-12 (Beta)
#### 追加・改善 (Added & Improved)
- **ツリー専用ホットキーの新設とセッション保持（空中でのTキー再開）**:
  - 画面内の何もない場所（またはアイテム非ホバー時）で `T` キーを押すだけで、直前まで作業していたツリーをそのまま確実に再開できる機能を追加。
  - アイテムをホバーして開く必要がなくなったため、意図せず他のアイテムでツリーの保存済み作業内容が上書き消失する事故を完全に防止。
  - 操作設定（Controls）に「クラフトツリーを開く/再開 (`key.jei.openCraftingTree`)」キーバインドを新設し、好みのキーに割り当て可能に。
  - ブックマークグループから開いたツリーもセッションとして永続保持されるように改善。
- **WASD/矢印キーによるツリーパン操作の改善**:
  - WASDや矢印キーが画面上のボタン選択フォーカスに吸われていた問題を解消。ツリー移動キー入力を最優先で消費し、快適なキーボード操作を実現。
- **Shift + マウスホイールによる水平スクロール有効化**:
  - Shiftキーを押しながらホイールを回すことで、ツリー画面を左右にスムーズにパン移動できるよう修正。

#### 修正 (Fixed)
- **統計パネルのホバー表示が内部名称だった不具合の修正**:
  - 統計パネル（Summary Bar）のアイテムや流体にマウスホバーした際、内部ID（`minecraft:iron_ingot` 等）ではなく正式なローカライズ表示名および詳細ツールチップが表示されるよう修正。
- **統計パネルの数量表示の重なり解消**:
  - 統計パネルのスロットにおいて、桁数の多い数量テキストが隣接スロットやアイコンに食い込んで重なる問題を修正。スロット幅に応じた自動スケール縮小とスロット間隔の拡大を適用。
- **統計パネルの初期生成時に液体やガスの数量が表示されない問題の修正**:
  - ツリー初期生成時にも `BookmarkIngredientAmountResolver` を通じて流体・ガスの要求量（mB）を正確に取得・集計できるよう改修。

---

### [15.59.0.212-jeiuu.4-beta6] - 2026-11-12 (Beta)
#### 追加・改善 (Added & Improved)
- **JEI レシピビューワー画面との連携による直感的なレシピ選択**:
  - ノード右クリック時に専用ダイアログではなく、JEI の標準レシピ画面（`RecipesGui`）を開くよう変更。
  - レシピ画面内の「＋（プラス）」ボタンを押すことで、そのレシピをツリーのノードに即座に選択・採用してツリー画面に自動復帰するシームレスなUIを実現。
- **作業中ツリーのセッション保持（ツリーを閉じても消えない機能）**:
  - 手動探索やカスタマイズ作業中のツリー状態（目標アイテム、選択レシピ、数量、折りたたみ状態、カメラ位置）をセッションとして永続保持。
  - 画面を閉じてインベントリ作業やレシピ確認を行った後でも、作業途中のツリーがそのまま完全に復元されます。
  - トップバーに「リセット（`New`）」ボタンを追加し、いつでもワンクリックで初期状態に戻せるよう改善。
- **大規模ツリーの左右移動・ナビゲーションの劇的快適化**:
  - `Shift + マウスホイール` によるスムーズな水平パン（左右スクロール）を実装。
  - キーボードの矢印キー（←/→/↑/↓）および WASD による自由なパン操作に対応。
  - トップバーに「親項目（目標アイテム）にカメラ移動（`H`）」ボタンを追加（`Home` キーまたは `H` キーでも瞬時にセンタリング可能）。

#### 修正 (Fixed)
- **大規模ツリーにおけるノード重なり・衝突の解消**:
  - `TreeLayout` におけるサブツリー輪郭計算の正規化ロジックを修正。親ノードを中心とした完璧な相対座標系で輪郭を合成・照合するように改修し、階層が深くなったり分岐が増えてもノードが一切重ならないよう是正。
  - ノード間隔（X/Y間隔）を最適化し、密集時の視認性を向上。
- **折りたたみ・展開時の親項目（目標アイテム）の座標ズレ防止**:
  - レイアウト再計算時に目標アイテム（ルートノード）の変位をカメラ位置へ即座にフィードバック追従（アンカー補正）させるよう改善。
  - ノードの開閉や拡張を行っても、画面上での目標アイテムの位置が一切動かずピタッと静止するよう修正。

---

<a name="english"></a>
## English

### Versioning Policy
To clearly trace the upstream JEI / JEIU base version, this mod adopts the following versioning scheme:
`[Upstream JEI Version]-jeiuu.[JEIUU Revision]` (e.g. `15.59.0.212-jeiuu.2`, `15.59.0.212-jeiuu.4-beta9`)
- **Upstream JEI Version** (`15.59.0.212`): Preserves the exact upstream JEI / JEIU release version as a baseline mark.
- **JEIUU Revision** (`jeiuu.4-beta9`): Distinguishes and tracks fixes, optimizations, and release types (Alpha/Beta/Release) specific to this fork.

---

### [15.59.0.212-jeiuu.4-beta25] - 2026-11-13 (Beta)
#### Fixed & Improved
- **Fixed Machine/Handler Execution Count Rendering & Tooltips in Bottom Stats Bar**:
  - Fixed an issue where `HANDLER` slots in the bottom stats bar rendered empty black squares because they lacked an `ITypedIngredient`. Now looks up the corresponding `IRecipeCategory` icon (or falls back to its first recipe catalyst) and scales it cleanly into the 16x16 slot.
  - Ensured machine and intermediate craft counts display even for single executions (`1`) and added the localized category title and recipe count to the hover tooltip.

---

### [15.59.0.212-jeiuu.4-beta24] - 2026-11-13 (Beta)
#### Fixed & Improved
- **Fixed 1px-Thin / Unstable Fluid & Gas Icon Rendering**:
  - In `CraftingTreeScreen.renderTypedIngredient`, small recipe fluid/gas amounts (e.g., 10 mB) were previously passed unnormalized to `FluidTankRenderer` and chemical renderers, causing them to render only a 1-pixel strip relative to the 1000 mB tank capacity.
  - Applied `IIngredientHelper.normalizeIngredient()` before rendering so all fluids, gases, and chemicals render reliably at full 16x16 icon height.
- **Clarified `Shift+B` and `T` Key Purposes in Controls and Alt-Hold Shortcut Tooltips**:
  - Updated keybinding labels in Minecraft's Controls menu to clearly distinguish `Shift+B` (expand new crafting tree for hovered item) and `T` (expand or resume active crafting tree).
  - Updated `BookmarkHotkeyTooltipUtil` across ingredients, bookmarks, normal bookmark groups, recipe chains, and favorite recipes so holding `Alt` displays individual descriptions for both `Shift+B` and `T`.

---

### [15.59.0.212-jeiuu.4-beta23] - 2026-11-13 (Beta)
#### Fixed & Improved
- **Fixed Non-Consumable & Reusable Items (Molds, Presses, etc.) Demanding Multiple Copies**:
  - Fixed `TreeStats` summing non-consumable items across multiple branches in the crafting tree instead of taking the maximum required count (`BigInteger::max`), preventing reusable molds/presses from being requested multiple times in total requirements.
  - Expanded `TreeMath.isNonConsumable` detection to cover `stamp`, `shape.`, `extruder_shape`, `integrated_circuit`, `BookmarkItemType.NONCONSUMABLE` metadata, and self-returning crafting remainders (`getCraftingRemainingItem`).
- **Fixed Unstable & Misaligned Rendering for Fluids, Gases, and Chemicals**:
  - Separated tree canvas and stats bar rendering into distinct passes (`backgrounds/lines -> g.flush() -> ingredients -> overlays -> g.flush()`), eliminating Z-order conflicts between `GuiGraphics` buffered draws and direct `Tesselator`-based fluid/chemical renderers.
  - Added explicit `g.flush()`, `RenderSystem.setShaderColor(1, 1, 1, 1)`, and blend state cleanup around `renderTypedIngredient` to prevent shader color or blend leakage across slots.
  - Used floating-point centering and renderer-dimension scaling (`getWidth` / `getHeight`) so fluid and gas icons remain accurately aligned inside zoomed slots.

---

### [15.59.0.212-jeiuu.4-beta22] - 2026-11-13 (Beta)
#### Fixed & Improved
- **Restored Strict Separation Between Gas Lithium and Fluid Lithium (Same ID, Different Types)**:
  - Fixed a regression in `TreeRecipeBuilder.findRecipe` where `requireTargetMatch` was bypassed and recipe iteration returned prematurely, causing recipes sharing the same UID (such as Mekanism's Rotary Condensentrator) to return the wrong direction (gas vs. fluid) regardless of target ingredient type.
  - Restored output-matching validation in the fallback lookup to reject recipes whose outputs do not match the target ingredient's key and type.
  - Updated `TreeMath` node path generation (`prunedPaths`) and cycle detection (`visitedRecipes`) to include full typed keys (`stableKey()` / `BookmarkIngredientKey`) instead of bare `ingredientUid` or `recipeUid`, preventing path collisions and false cycle blocks between gas and fluid variants.

---

### [15.59.0.212-jeiuu.4-beta21] - 2026-11-13 (Beta)
#### Fixed & Improved
- **Fixed Inverted WASD & Arrow Key Camera Panning**:
  - Corrected the directional signs for `W`/`Up` (up), `S`/`Down` (down), `A`/`Left` (left), and `D`/`Right` (right) so camera movement matches intuitive user direction, scaled by zoom level for consistent panning speed.
- **Dual Deletion Modes (Single Node vs. All Identical Items)**:
  - Updated the quantity adjustment screen with two explicit removal buttons: "Remove This Node & Branch Only (`Del`)" and "Remove All Identical Items & Branches (`Shift+Del`)".
  - Added keyboard shortcuts directly on the tree canvas: `Delete` / `Backspace` removes only the hovered node and its branch, while `Shift + Delete` / `Shift + Backspace` removes all identical items and their branches across the entire tree.
- **Complete Subtree Restoration When Re-Selecting Recipes After Deleting a Child Node**:
  - Fixed an issue where deleting one of multiple child items and subsequently re-selecting the recipe did not restore the deleted child item.
  - Re-selecting a parent node's recipe now automatically clears all descendant pruned paths (`prunedPaths`) and unprunes child keys (`prunedKeys` / `uncraftedKeys`). Furthermore, opening JEI via `R` / `U` keys and navigating to parent or child recipes before clicking `+` now intelligently resolves the target node and reliably restores all child ingredients.
  - Unified duplicate ingredient slot aggregation across both initial tree generation and manual recipe selection.
- **Crafting Tree Refactoring & Optimization**:
  - Refactored `TreeLayout` anchor tracking to match nodes by `nodePath` and ingredient key, preventing camera jumping during layout recalculations.
  - Prevented temporary cycling slot displays from polluting the global recipe UID cache in `TreeRecipeBuilder`.
  - Cached lowercase tooltip strings per ingredient key to eliminate per-frame tooltip generation overhead during search highlighting.

---

### [15.59.0.212-jeiuu.4-beta20] - 2026-11-13 (Beta)
#### Fixed & Improved
- **Clean Node Pruning via Tree Paths (`prunedPaths`) & Full Tag Ingredient Substitution**:
  - **Complete Node Deletion**: Replaced the behavior of leaving pruned nodes as uncrafted leaves with complete visual and structural removal of the selected node and its entire subtree from the canvas and requirements list upon deletion.
  - **Path-Based Pruning (`prunedPaths`)**: Introduced hierarchical node path tracking (`nodePath`) for deterministic, collision-free pruning. Pruning an item in one branch will never accidentally affect identical items in other branches, eliminating key-clash issues and residual ghost keys blocking recipe expansions.
  - **Tag Ingredient Substitution (Colored Glass Switching)**: When switching a tag-resolved ingredient (e.g., clear glass) to another candidate (e.g., red stained glass) via the recipe GUI transfer button, the parent node's component inputs (`customRecipeInputs`) are automatically updated to replace the old ingredient with the chosen one, reliably reflecting custom materials in the tree.
  - **Session Persistence**: Fully serialized `prunedPaths` into `crafting_tree_session.json` to guarantee deleted branches and substituted components remain faithfully preserved across game restarts.

---

### [15.59.0.212-jeiuu.4-beta19] - 2026-11-13 (Beta)
#### Fixed & Improved
- **Rock-Solid Branch Deletion, Re-Addition, and Recipe Overwrite (`uncraftedKeys`)**:
  - Replaced the brittle descendant key pollution (`prunedKeys`) approach with an explicit uncrafted state manager (`uncraftedKeys`). Pruning a branch now directly clears the node's assigned recipe and marks it as an uncrafted leaf, automatically causing its child branches to vanish cleanly without polluting global key filters.
  - Eliminates accidental removal of identical items in other branches and prevents lingering descendant keys from blocking newly selected recipes.
  - Allows pruned nodes to be seamlessly re-assigned the same recipe or completely different recipes unlimited times, with all child ingredients reliably expanding into the tree canvas.
  - Switching recipes on existing nodes without prior deletion cleanly and instantaneously overrides and recalculates the new component tree without leftover conflict.
  - Fully persisted `uncraftedKeys` into `crafting_tree_session.json` for faithful restoration across game restarts.
  - Fixed manual recipe assignments and custom inputs being erroneously wiped during recalculations in bookmark group trees.

---


### [15.59.0.212-jeiuu.4-beta18] - 2026-11-12 (Beta)
#### Fixed & Improved
- **Dual Hotkey Display (Shift+B / T) for Crafting Tree in Controls and Tooltips**:
  - Updated in-game item and bookmark tooltips to clearly display both tree-launch hotkeys (`Shift + B / T : Open recipe tree`).
  - Clarified keybinding labels in Minecraft's Controls menu to "Expand Crafting Tree (Shift+B)" and "Expand/Resume Crafting Tree (T)" so players immediately understand both keys trigger the crafting tree.
  - Enhanced input handlers across ingredient focus and bookmark overlay to uniformly accept either configured keybinding (`showRecipeTree` / `openCraftingTree`) or default key inputs (`Shift+B` and `T`).

---

### [15.59.0.212-jeiuu.4-beta17] - 2026-11-12 (Beta)
#### Fixed & Improved
- **Standardized Tree-to-Chain Export Recipe Order (Ingredients to Final Output)**:
  - Fixed an issue where exporting a crafting tree into a bookmark recipe chain group via the "Save" button registered recipes in top-down tree traversal order (root product first), resulting in an inverted (upside down) recipe order compared to normally constructed recipe chains.
  - Reversed the exported recipe layouts list so that lower-level raw materials and sub-assembly steps are listed first, culminating with the final product at the end, exactly matching standard bottom-up recipe chains.

---

### [15.59.0.212-jeiuu.4-beta16] - 2026-11-12 (Beta)
#### Fixed & Improved
- **Standardized Fluid, Chemical, and Gas Units to SI Prefix + mb**:
  - Eliminated mixed and ambiguous unit notations (`bmb`, `kmb`, `B`, `MB`, etc.) across fluids, chemicals, and gases (such as Spent Nuclear Waste).
  - Standardized all fluid and chemical amounts strictly to **`mb` (millibuckets)** as the base unit.
  - Consistently formatted quantities >= 1,000 using standard SI prefixes + `mb` (`Kmb`, `Mmb`, `Gmb`, `Tmb`, `Pmb`, `Emb`, `Zmb`, `Ymb`).
  - For astronomical amounts exceeding the SI prefix scale (Yotta / 10^24), adopted exponential scientific notation (e.g. `1.2e28mb`), fully aligned with the recipe chain system.

---

### [15.59.0.212-jeiuu.4-beta15] - 2026-11-12 (Beta)
#### Fixed & Improved
- **Full Support & Loss Prevention for Mekanism Chemicals (Infusion/Gas) & Metallurgic Infusion Recipes**:
  - Fixed an issue where recipes using non-item ingredients (such as Mekanism's `InfusionStack` and `GasStack` in Metallurgic Infuser recipes) failed deserialization during session restoration (`resolveIngredient`), causing infusion inputs (e.g., XP infusion) to vanish from the crafting tree.
  - Added fallback scanning via `getAllTypedIngredients` and `getUniqueId` in `BookmarkIngredientKey.resolveIngredient`, ensuring all registered custom ingredient types (chemicals and non-item ingredients) are reliably restored across sessions.
- **Appropriate Scope for Custom Recipe Inputs (`customRecipeInputs`)**:
  - Restricted `customRecipeInputs` capture strictly to recipes containing cycling/tag slots with multiple candidates (e.g., colored glass), preserving standard JEI recipe extraction for fixed machine recipes like Metallurgic Infusers without redundant overrides.
  - Implemented a safety fallback in both `TreeMath` and `TreeRecipeBuilder` that automatically restores complete inputs from `info.inputs()` if a custom recipe input entry was truncated or lost ingredients.
- **Calculation Consistency & Robust Leaf Node Recipe Assignment**:
  - Enforced `Math.max(1, ...)` amount resolution in `TreeMath.findRecipeInputs` to prevent zero-amount inputs for infusion and special stacks.
  - Allowed uncrafted leaf nodes created from pruned branches to be reliably assigned any recipe, including advanced machine recipes.

---

### [15.59.0.212-jeiuu.4-beta14] - 2026-11-12 (Beta)
#### Fixed & Improved
- **Exact Displayed Ingredient Capture & Persistence for Tag / Cycling Recipes**:
  - Fixed an issue where scrolling recipe slots to display a specific ingredient variant (e.g., colored glass or tag alternatives) and pressing the `+` transfer button mistakenly registered the default first candidate (e.g., white glass) instead of the user-selected variant.
  - Passed `IRecipeLayoutDrawable` directly from `RecipeTransferButtonController` and extracted the exact active ingredient (`displayedIngredient`) via `TreeRecipeBuilder.createFromLayout`, storing it into `customRecipeInputs`.
  - Persisted user-chosen custom recipe inputs into `config/jei/crafting_tree_session.json`, ensuring selected color variants and alternative materials remain faithfully intact across game restarts.
- **Node Preservation and Freely Re-Assignable Recipes after Branch Deletion**:
  - Fixed an issue where deleting an intermediate node or branch erroneously added the target node itself into `prunedKeys`, vanishing from the tree canvas and blocking identical or replacement recipes from being added.
  - When deleting a child node's branch, pruning now only removes descendant branches while preserving the target node itself as an uncrafted leaf slot. Users can now freely right-click the preserved node to re-assign recipes or pick new alternatives without restriction.
- **Seamless Recipe Overwrite & Replacement without Prior Node Deletion**:
  - Ensured switching to a different recipe directly via the `+` button cleanly overrides existing custom input mappings and smoothly recalculates the new component tree without conflicts.

---

### [15.59.0.212-jeiuu.4-beta13] - 2026-11-12 (Beta)
#### Fixed & Improved
- **Dynamic Branch Unpruning on Recipe Re-Selection**:
  - Fixed an issue where correcting mistakes by pruning unwanted branches and picking a replacement recipe failed to reflect new ingredients because previously pruned keys remained blocked in `prunedKeys`.
  - Automatically unprunes the modified node and the input ingredients (plus any configured descendant recipes) required by the newly chosen recipe upon applying it, ensuring corrected recipe branches expand and render seamlessly.
- **Guaranteed Recipe Override & Output Match Relaxation**:
  - Fixed an issue where modifying a node's recipe without pruning failed to overwrite due to strict output matching checks in `findRecipe` rejecting valid recipes.
  - Implemented relaxed output matching using `matchesCraftingAvailable` and guaranteed fallback to `factor = 1` for user-chosen recipes, preventing user actions from being silently discarded.
  - Prioritized manual recipe overrides directly over stale bookmark chain inputs in group trees (`groupId >= 0`), guaranteeing 100% reliable recipe overrides.
  - Cleared `pendingRecipeTarget` immediately after recipe application to prevent erroneous subsequent overrides.
- **Robust Session Persistence Across Game Restarts**:
  - Fixed an issue where `TypedIngredientSerializer` failed deserialization during game launch due to `UidContext.Recipe`, causing the entire session to be discarded and restoring all previously pruned items.
  - Migrated target ingredient persistence to `BookmarkIngredientKeySerializer` (full SNBT, type, and UID preservation) with `matchesCraftingAvailable` compatibility matching, ensuring pruned node states and customized trees persist accurately across game restarts without pruned nodes resurrecting.

---

### [15.59.0.212-jeiuu.4-beta12] - 2026-11-12 (Beta)
#### Fixed & Improved
- **Tree Session Persistence Across Game Restarts**:
  - Fixed an issue where the crafting tree session in memory was lost upon restarting the game, erroneously opening the first bookmark group's recipe chain when reopening the tree screen.
  - Automatically persists the active session into `config/jei/crafting_tree_session.json`, fully restoring incomplete work (target ingredient, quantities, manually selected recipes, pruned branches, camera position, and zoom level) across game restarts.
- **Restoration of Pressurized Reaction Chamber (PRC) Fluid Inputs & Removal of Slot-Name Filtering**:
  - Fixed an issue where PRC fluid input slots (e.g. water) named `"extra"` were mistakenly omitted by beta11's slot-name filtering logic, preventing fluid recipes from registering.
  - Completely removed ad-hoc slot-name exclusions ("extra", etc.), reverting to the clean beta8 baseline and ensuring all valid recipe inputs, including PRC fluids, are accurately preserved.
- **Strict Separation of Gas Lithium & Fluid Lithium**:
  - Fixed an issue where gas lithium and liquid lithium were treated as identical during tree expansion and recipe selection due to sharing the same resource ID (`mekanism:lithium`).
  - Separated the recipe cache by typed ingredient keys (`BookmarkIngredientKey`) and enforced strict output type verification in `findRecipe`, guaranteeing that gas and fluid recipes are never confounded.

---

### [15.59.0.212-jeiuu.4-beta11] - 2026-11-12 (Beta)
#### Fixed & Improved
- **Full Restoration & Normalization of Infusion Conversion & Metallurgic Infuser Recipes**:
  - Reverted recipe parsing core to the stable beta8 architecture and strictly filtered only Mekanism's "extra" display slots.
  - Resolved extraneous gas tanks and rotation display items leaking into metallurgical infusing recipe inputs.
  - Fixed internal machine recipes ("Infusion Conversion", converting items to chemical infuse types) failing to register.
- **Side-Effect-Free Recipe Execution Calculation**:
  - Accurately calculates craft execution multiplier directly from the node's selected recipe yield (outputFactor) without polluting child nodes with parent yield values, ensuring nested conversions (A -> B -> C) calculate exact demanded quantities without distortion.

---

### [15.59.0.212-jeiuu.4-beta10] - 2026-11-12 (Beta)
#### Fixed & Improved
- **Exclusion of Supplementary Display Items (Gas Tanks / Chemical Tanks) (Deprecated: unstable, ad-hoc fix; slot-name exclusion completely removed in beta12)**:
  - Fixed an issue where supplementary container items (such as Mekanism's "Extra" slot gas tanks and chemical tanks used to supply internal machine buffers) were mistakenly included as craft ingredients in the crafting tree.
  - Automatically identifies and filters out display guide slots (e.g. slots named extra, container, info, fuel, charge) and tank items when the recipe already contains the corresponding gas, infusion, or fluid ingredient.
- **Reliable Node & Branch Pruning ("Remove Node and Branch")**:
  - Integrated `prunedKeys` tracking directly into the `TreeMath` calculation engine.
  - Enforced pruning checks across root selection, recursive node construction (`buildNode`), and dynamic recipe lookup (`findRecipeInputs`), completely preventing pruned nodes from re-spawning during recalculation or tree updates.

---

### [15.59.0.212-jeiuu.4-beta9] - 2026-11-12 (Beta)
#### Added & Improved
- **Unified "mb" Formatting for Nuclear Waste, Infusions & All Chemicals**:
  - Fixed sub-bucket amounts (< 1,000 mB) displaying as bare decimals (e.g., `.16`) by standardizing on `mb` units (e.g., `160mb`, `80mb`).
  - Standardized tooltips across the tree screen (required total, produced total, summary bar amounts) to always display typed units (mb, B, bmb, etc.).
- **Camera Pan & Zoom Preservation on Recipe Selection**:
  - Selecting a recipe in JEI via the `+` button now retains the active zoom level and anchors camera tracking to the modified node, eliminating jarring camera resets to fit-view.
- **Non-Consumable Catalysts & Molds Handling (AE2 Presses, Molds, GT Circuits)**:
  - Automatically identifies non-consumable items (AE2 inscriber presses, molds, casts, dies, stamps, GregTech circuits, crafting remainders, and catalyst slots).
  - Keeps requirements fixed at 1 (or base requirement) without multiplying by recipe execution multiplier.

#### Fixed
- **Sub-Ingredient Multiplier Calculation for Partial Yield Recipes (Deprecated: unstable, ad-hoc fix; replaced by fundamental fix in beta11/12)**:
  - Fixed a math bug where parent recipe output factors (e.g. 80mb yield) were erroneously mapped onto child output amounts rather than parent craft multipliers.
  - Correctly calculates input item demand (e.g. producing 160mb from an 80mb/item recipe accurately demands 2 items instead of 160).

---

### [15.59.0.212-jeiuu.4-beta8] - 2026-11-12 (Beta)
#### Added & Improved
- **Multi-Core Parallel Processing & Cache Optimization for Massive Trees**:
  - Multi-threaded recipe candidate lookups and UID resolution using parallel streams. Utilizes all CPU cores to calculate and expand massive recipe trees instantly in heavy modpacks.
  - Introduced in-memory concurrent caches (`ConcurrentHashMap`) for recipe metadata, slots, outputs, and candidate lookups, eliminating redundant drawable creation overhead.
- **Improved Slot Amount Visibility & Dark Background Box**:
  - Added semi-transparent dark background boxes (`0xC0000000`) behind quantity text in slots and the stats bar. Solves visual overlapping with complex textures and item labels (such as GregTech Programmed Circuit configuration numbers).
  - Ensured leaf/raw material nodes display their total required amounts correctly.
- **Enhanced Horizontal Pan Speed with Shift + Scroll**:
  - Significantly increased Shift + Mouse Wheel scroll speed for swift horizontal navigation across large crafting trees.
- **Pruning & Branch Deletion Fixes with Keyboard Shortcut**:
  - Deleting a node now recursively gathers and removes all descendant child nodes across both manual and bookmark chain trees.
  - Added `Delete` and `Backspace` hotkeys to directly remove the hovered node and its entire branch.

#### Fixed
- **Missing Tooltip Translation Keys**:
  - Added translations for `jei.tree.tooltip.amount` and `jei.tree.tooltip.produced` in English, Japanese, and Simplified Chinese.
- **Strict Separation of Fluids and Gases (e.g. Lithium)**:
  - Enforced strict ingredient type and UID matching via `BookmarkIngredientKey`, properly distinguishing between `fluid_stack` and `mekanism.api.chemical.gas.GasStack`.
- **Duplicate ItemStack Count Rendering**:
  - Fixed `renderTypedIngredient` passing the original multi-count item instead of a single item copy, preventing vanilla item stack numbers from overlapping with tree totals.
- **Fluid & Gas Unit Typo (`1bmb`)**:
  - Corrected 1,000 Buckets (1,000,000 mB) abbreviation from `bmd` to `bmb`.

---

### [15.59.0.212-jeiuu.4-beta7] - 2026-11-12 (Beta)
#### Added & Improved
- **Dedicated Crafting Tree Hotkey & Session Resume (`key.jei.openCraftingTree`)**:
  - Added dedicated keybind to resume working crafting tree anytime from anywhere without needing to hover over an item.
  - Prevents accidental overwriting of in-progress trees when opening the tree.
- **Optimized Keyboard WASD & Arrow Key Panning**:
  - Ensured tree panning consumes WASD / Arrow keys with priority over widget focus.
- **Shift + Mouse Wheel Horizontal Scroll Support**:
  - Smooth lateral camera panning across the tree canvas.

#### Fixed
- **Stats Panel Tooltip Displaying Internal IDs**:
  - Resolved localized item/fluid display names and full tooltips in the summary bar.
- **Overlapping Quantity Text in Stats Bar**:
  - Automatic scaling and enlarged slot spacing prevent text collision.
- **Initial Fluid and Gas Demand Counting**:
  - Accurate calculation of fluid and gas units (mB/B) upon initial tree generation.

---

### [15.59.0.212-jeiuu.4-beta6] - 2026-11-12 (Beta)
#### Added & Improved
- **JEI Recipes GUI Integration for Recipe Selection**:
  - Right-clicking a node now opens JEI's standard `RecipesGui`. Clicking the `+` button directly binds the recipe to the crafting tree node and returns seamlessly.
- **Crafting Tree Session Persistence**:
  - Working tree state (target item, custom recipe choices, manual quantities, collapsed branches, camera pan/zoom) is persistently retained even across GUI closes.
  - Added `New` reset button to restore default layout anytime.
- **Smooth Navigation Across Massive Trees**:
  - Horizontal camera panning with `Shift + Mouse Wheel`.
  - Directional panning with Arrow keys and WASD.
  - `H` / `Home` button to instantly center the view on the root target item.

#### Fixed
- **Resolved Node Overlapping in Massive Trees**:
  - Overhauled subtree contour calculation in `TreeLayout` to prevent overlap regardless of tree depth or branch width.
- **Eliminated Camera Jumping on Collapse/Expand**:
  - Re-anchored camera tracking to target item root node during layout updates.

---

### [15.59.0.212-jeiuu.4-beta3] - 2026-11-12 (Beta)
#### 修正 (Fixed)
- **クラフトツリー内のアイテム描画が正常に行われない不具合の修正**:
  - `IIngredientRenderer` 描画時に `PoseStack` の座標変換（`translate(x, y, 100)`）およびカメラのズームスケール適用（`scale`）を実装。画面左上端への描画崩れを解消し、各ツリースロットおよび統計バー内に正しいサイズと位置でアイテムが表示されるよう修正。
  - スロット内の数量テキストのZレイヤーを前面（Z=300）に調整し、重なりによる不可視化を防止。
- **レシピチェーンからツリーを開くキー入力（Shift+B / T）が握りつぶされていた問題の修正**:
  - `GroupInputHandler` において、キー入力（`InputType.IMMEDIATE`）に対して `input.isSimulate()` による安全な実行判定を適用。レシピチェーン表示中にも `Shift + B`、`T`、およびマウス中クリックで確実にクラフトツリーが開くよう修正。
- **レシピチェーンからの材料データ（ITypedIngredient）の欠落防止**:
  - `TreeChainBridge` において `BookmarkList.getRecipeChainInputs` を優先参照し、チェーン内の全スロットの `ITypedIngredient` を確実に復元・引当。中間素材の手動レシピ指定時にも JEI レシピから自動で子ノードを動的抽出できるよう強化。

---

### [15.59.0.212-jeiuu.4-beta2] - 2026-11-12 (Beta)
#### 修正・追加 (Fixed & Added)
- **チェーンおよびブックマークからツリーが開けない不具合の修正**:
  - ブックマークスロット上のアイテム、ブックマークチェーン、グループパネルヘッダーのどこからでも `Shift + B`、`T`、および **マウス中クリック** で確実にクラフトツリー画面（`CraftingTreeScreen`）が起動するよう入力ハンドラ階層を再構築。
  - 旧ツリー画面（`RecipeTreeScreen`）へ誘導していた残存コードを完全撤去。
- **チェーン以外（アイテムリスト・レシピ画面・インベントリ）からのツリー直接作成機能**:
  - JEIアイテムリスト、レシピ画面（`RecipesGui`）、インベントリ内の任意のアイテムにカーソルを合わせて `Shift + B` または `T` キーを押すことで、そのアイテムを完成目標（ルート）とするクラフトツリーを即座にゼロから作成・開くことが可能に。
- **目標アイテムとレシピを指定して手動でツリーのルート・階層を探索・構築する機能**:
  - 目標アイテムからツリーを開始し、ツリー内の材料ノードを右クリックして代替レシピを選択することで、その材料の原料ノードがツリーに生えて深掘り展開される手動探索に対応。
  - レシピ解除（原材料に戻す）、ノード削除（枝の剪定）、数量・倍率の手動調整に対応。
  - 手動構築したツリーは画面上部の「チェーン保存」ボタンを押すだけで、ブックマーク欄に新しいクラフトチェーングループとして一発保存可能。

---

### [15.59.0.212-jeiuu.4-beta1] - 2026-11-11 (Beta)
#### 追加・改善 (Added & Improved)
- **クラフトツリー（Crafting Tree）機能の完全移植・統合**:
  - `nei-features-for-jei` のクラフトツリー機能を 1.20.1 Forge 専用に最適化・リファクタリングして統合。
  - **BigInteger（bigint）完全対応**: 要求数、クラフト数、生産数、手動入力数量のすべてを `BigInteger` ベースで高精度演算。10^20規模の巨大数量レシピでもオーバーフローせず正確に算出。
  - **軽量化・フラスタムカリング描画**: ツリーカメラの表示領域判定（`TreeCamera.isWorldRectVisible`）を実装し、画面外のノードや接続線を描画スキップすることで、巨大ツリーでも滑らかな60fpsを維持。
  - **チェーンツリー双方向相互変換**:
    - ブックマーク欄のレシピチェーンから `Shift + B` または `T` キー（またはマウス中クリック）で瞬時にインタラクティブなクラフトツリーを生成。
    - クラフトツリー画面上部の「チェーン保存」ボタンにより、現在のツリー（折りたたみ・手動数量・選択レシピを反映）をブックマークチェーンへエクスポート可能。
  - **代替レシピの選択・指定機能**:
    - ツリー内の材料スロットを右クリックすることで、利用可能なすべての代替レシピ一覧ダイアログ（`TreeRecipeSelectorScreen`）が開き、使用するレシピを直接選択・切り替え可能。
  - **手動調整・倍率指定機能**:
    - 材料ノードを `Shift + 左クリック` または `中クリック` することで調整ダイアログ（`TreeAdjustQuantityScreen`）を開き、数量の直接入力、クイック増減（+1, +10, +64, x2, /2）、強制クラフト切り替え、ノード剪定（削除）が可能。
  - **材料・中間品・副産物・マシンの統計パネル**:
    - 画面下部の統計パネルにより、不足原材料、手持ち原材料、中間作成物、副産物、必要な機械・作業台の一覧をリアルタイム集計して横スクロール一覧表示。
  - **インクリメンタル検索・パン・ズーム**:
    - マウスホイールによる滑らかなズーム（0.2x〜5.0x）、ドラッグによるパン移動、検索バーによるツリー内ノードのインクリメンタル強調表示に対応。

---

### [15.59.0.212-jeiuu.3-beta2] - 2026-11-05 (Beta)
#### 修正・改善 (Fixed & Improved)
- **流体・ガスの巨大数量ホバー時クラッシュおよびフリーズの修正**:
  - `FluidHelper.copyWithAmount` において `Math.toIntExact` による整数オーバーフロー例外（`ArithmeticException: integer overflow`）を `Integer.MAX_VALUE` クランプ処理へ修正。
  - `ProjectedBookmarkElement` でのツールチップ用流体スタック生成時に安全クランプと例外防護を追加し、外部MODのツールチップ生成処理におけるフリーズ・クラッシュを完全に防止。
- **ブックマークツールチップへの BigInteger / long 数量行の追加**:
  - アイテム・液体・ガスのブックマークスロットにホバーした際、ツールチップに安全かつ正確な数量表示（短縮フォーマットおよびカンマ区切りの実数表記）を追加。
- **液体・ガスの単位表記を  md (1,000,000 mB) へ変更**:
  - 1,000 バケツ（1,000,000 mB）を表す流体単位表記を kB から  md に更新。BigInteger の流体量とも一貫して統合。
- **プレビューツールチップの文字重なり防止**:
  - `RecipeChainPreviewTooltipComponent` の数量テキスト描画に動的オートスケーリングと境界クランプを適用。

---

### [15.59.0.212-jeiuu.3-beta1] - 2026-11-05 (Beta)
#### 追加・改善 (Added & Improved)
- **レシピチェーンの BigInteger (bigint) 計算・表示対応**:
  - `Long.MAX_VALUE` 付近の液体やガスを大量に消費・生産するアイテムや複数要求レシピでも、オーバーフローやクラッシュを起こさずに無限精度で計算・描画できる `BigIntMath` 演算エンジンを新設。
  - `BookmarkItemMetadata`、`RecipeChainItem`、`RecipeChainMath` を BigInteger ベースに全面拡張。
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
  - 独立したレシピツリーや折りたたみブロック（Collapsed Blocks）が存在する場合に、安全に並行計算（`parallelStream`）を活用して高速処理。

---

### [15.59.0.212-jeiuu.2] - 2026-11-03
#### 変更 (Changed)
- **バージョニング体系の刷新**:
  - 上流のJEI由来のバージョン番号（15.59.0.212）をベースの印として完全保持し、JEIUU独自の更新を接尾辞 `-jeiuu.<N>` として分離・識別できるように変更。

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
- 日英バイリンガルのドキュメント（`README.md`, `CHANGELOG.md`, `CONFIG.md`）を追加。
- GPL-3.0 ライセンス定義の統合。

#### 変更 (Changed)
- **Forge 1.20.1 専用化**:
  - Fabric および FabricApi サブプロジェクト、Fabric-Loom等のマルチローダー依存関係を完全に除去。
  - GradleビルドシステムをForge 1.20.1専用にスリム化・最適化。
  - 出力成果物のアーカイブ名を `JEIunofficial-unofficial-1.20.1-forge` に同期。
- **JEI前提要求達成ロジックの強化**:
  - `modId="jei"` として登録し、全JEI依存MODの前提条件を満たす構造を確立。

---

<a name="english"></a>
## English

### Versioning Policy
To clearly trace the upstream JEI / JEIU base version, this mod adopts the following versioning scheme:
[Upstream JEI Version]-jeiuu.[JEIUU Revision] (e.g. 15.59.0.212-jeiuu.2, 15.59.0.212-jeiuu.4-beta3)
- **Upstream JEI Version** (15.59.0.212): Preserves the exact upstream JEI / JEIU release version as a baseline mark.
- **JEIUU Revision** (jeiuu.4-beta3): Distinguishes and tracks fixes, optimizations, and release types (Alpha/Beta/Release) specific to this fork.

---

### [15.59.0.212-jeiuu.4-beta3] - 2026-11-12 (Beta)
#### Fixed
- **Fixed Ingredient Rendering in Crafting Tree GUI**:
  - Added proper `PoseStack` coordinate translation (`translate(x, y, 100)`) and dynamic zoom scaling (`scale`) in `renderTypedIngredient`. Fixed an issue where ingredients were drawn at `(0, 0)` off-screen instead of inside their respective slot boundaries.
  - Adjusted quantity text Z-layer to 300 to strictly prevent clipping behind rendered item stacks.
- **Fixed Recipe Chain Keyboard Trigger (Shift+B / T) Being Consumed**:
  - Replaced the restrictive `InputType.EXECUTE` check in `GroupInputHandler` with `!input.isSimulate()`, allowing immediate keyboard inputs (`InputType.IMMEDIATE`) to correctly launch `CraftingTreeScreen` directly from recipe chains.
- **Prevented Missing Ingredient Instances from Recipe Chains**:
  - Enhanced `TreeChainBridge` to query `BookmarkList.getRecipeChainInputs`, ensuring `ITypedIngredient` instances are fully preserved and populated across all chain nodes. Added dynamic recipe lookup fallback for manually assigned intermediate recipes.

---

### [15.59.0.212-jeiuu.4-beta2] - 2026-11-12 (Beta)
#### Fixed & Added
- **Fixed Crafting Tree Opening from Chains and Bookmarks**:
  - Rebuilt input handling so `Shift + B`, `T`, or **mouse middle-click** reliably opens `CraftingTreeScreen` from any bookmark slot item, recipe chain, or group header.
  - Removed obsolete `RecipeTreeScreen` hooks that blocked event propagation.
- **Direct Tree Creation from Outside Chains (Item List, Recipes GUI, Inventory)**:
  - Hovering over any item in JEI ingredient list, recipe screen (`RecipesGui`), or player inventory and pressing `Shift + B` or `T` now immediately generates and opens a brand-new crafting tree rooted at that item.
- **Manual Root Calculation and Crafting Path Exploration from Scratch**:
  - Start from a target item and right-click intermediate ingredient nodes to choose specific recipes, expanding sub-ingredients dynamically into deeper tree branches.
  - Supports reverting back to raw ingredients, pruning branches via node deletion, and manual quantity overrides.
  - Export custom manually-built trees directly into new bookmark crafting chain groups via the "Save to Chain" button.

---

### [15.59.0.212-jeiuu.4-beta1] - 2026-11-11 (Beta)
#### Added & Improved
- **Ported and Optimized Crafting Tree Feature**:
  - Ported and refactored the interactive Crafting Tree from `nei-features-for-jei` specifically tailored for Minecraft 1.20.1 Forge.
  - **Full BigInteger (bigint) Support**: All requested amounts, craft quantities, outputs, and manual overrides are calculated with infinite-precision `BigInteger`, preventing integer overflows even at massive scales ($10^{20}+$ items/fluids).
  - **Performance Optimization & Frustum Culling**: Implemented viewport boundary checks (`TreeCamera.isWorldRectVisible`) to skip rendering of off-screen nodes and connection lines, maintaining smooth 60fps on large graphs.
  - **Bidirectional Chain & Tree Conversion**:
    - Open an interactive Crafting Tree directly from recipe chains in bookmarks using `Shift + B`, `T`, or mouse middle-click.
    - Export active crafting trees directly into bookmark groups via the "Save to Chain" button.
  - **Alternative Recipe Selection**:
    - Right-click any ingredient node to open `TreeRecipeSelectorScreen` and select custom recipes from available recipe categories.
  - **Manual Quantity & Multiplier Adjustment**:
    - `Shift + Left-click` or `Middle-click` any ingredient node to open `TreeAdjustQuantityScreen` for direct BigInteger quantity entry, quick adjustment (+1, +10, +64, x2, /2), force-craft toggling, and node pruning.
  - **Materials, Intermediates, Byproducts & Machine Statistics**:
    - Bottom analytics panel summarizes missing raw materials, held materials, intermediate crafts, byproducts, and required machines in real-time.
  - **Incremental Search, Pan, and Zoom**:
    - Smooth zooming (0.2x to 5.0x), drag-to-pan camera navigation, and live search bar highlighting matching tree nodes.

---

### [15.59.0.212-jeiuu.3-beta2] - 2026-11-05 (Beta)
#### Fixed & Improved
- **Fixed Crash and Freezes on Tooltip Hover for Huge Fluid/Gas Amounts**:
  - Fixed `ArithmeticException: integer overflow` in `FluidHelper.copyWithAmount` by clamping to `Integer.MAX_VALUE` instead of calling `Math.toIntExact`.
  - Added clamp guards and exception isolation in `ProjectedBookmarkElement.createTooltipIngredient` to completely prevent external mod chemical/fluid renderers from freezing or crashing.
- **Added BigInteger & Long Quantity Display to Bookmark Tooltips**:
  - Added safe, clear bookmark amount info in tooltips (both formatted unit representation and full comma-delimited numbers) on hover for items and fluids.
- **Updated Fluid & Gas Unit to  md (1,000,000 mB)**:
  - Replaced the 1,000-bucket (1,000,000 mB) unit suffix from kB to  md, fully integrated with BigInteger fluid calculations.
- **Prevented Text Overflow in Preview Tooltip Component**:
  - Added dynamic font auto-scaling and slot boundary clamping in `RecipeChainPreviewTooltipComponent`.

---

### [15.59.0.212-jeiuu.3-beta1] - 2026-11-05 (Beta)
#### Added & Improved
- **Recipe Chain BigInteger (bigint) Calculation & Display**:
  - Introduced `BigIntMath` computation engine to calculate and render recipe chains consuming or producing massive amounts of fluids/gases (e.g. near `Long.MAX_VALUE`) without overflow, negative wrapping, or crashes.
  - Fully enhanced `BookmarkItemMetadata`, `RecipeChainItem`, and `RecipeChainMath` with BigInteger precision.
- **Fixed Visual Overlap of Huge Numbers with Adjacent Slots**:
  - Added dynamic text auto-scaling in `IngredientListRenderer` whenever formatted text exceeds the available slot width.
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
  - Enabled parallel processing (`parallelStream`) for independent recipe branches and collapsed blocks without shared mutable state.

---

### [15.59.0.212-jeiuu.2] - 2026-11-03
#### Changed
- **New Versioning Scheme**:
  - Preserved the upstream JEI base version (15.59.0.212) intact, separating fork revisions with the `-jeiuu.<N>` suffix.

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
- Bilingual documentation in English and Japanese (`README.md`, `CHANGELOG.md`, `CONFIG.md`).
- Integrated GPL-3.0 license structure.

#### Changed
- **Forge 1.20.1 Specialization**:
  - Fully removed Fabric and FabricApi subprojects, Fabric-Loom toolchains, and multi-loader overhead.
  - Streamlined and optimized the Gradle build system strictly for Minecraft Forge 1.20.1.
  - Synchronized output archive name to `JEIunofficial-unofficial-1.20.1-forge`.
- **JEI Dependency Satisfaction**:
  - Preserved `modId="jei"` registration ensuring complete compatibility with all JEI-dependent mods and addons.
