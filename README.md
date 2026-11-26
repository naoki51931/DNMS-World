# DNMS World

Minecraft不要の独立型ブロック世界シミュレーター。既存DNMS Fabric 1.21.1版を維持し、共通AIを段階的に接続する。

## Roadmap
1. Java 21 headless world API and deterministic tests
2. Voxel terrain, collision, gathering and building
3. TypeScript / Three.js browser client
4. Pin and reuse upstream DNMS core; adapter contract tests
5. 24–256 NPCs, memory integration, snapshots and rollback
6. Performance profiling and optional C++ acceleration

## Compatibility
Upstream: https://github.com/naoki51931/DNMS_Doppelganger-NPC-Manegement-System
Minecraft protocol and save-format compatibility are not promised. Existing Fabric code remains unchanged. Shared behavior must be tested against a pinned upstream revision.

## Status
Initial development. No playable release yet.

## Browser preview
With Java 21, Gradle 8.10.2 and Node 22 installed, run:

```sh
gradle exportTerrain
cd client
npm install
npm run dev -- --host 0.0.0.0
```
Open the URL printed by Vite. This preview renders Java-generated terrain; no NPCs or player movement yet.

## DNMSとの責務分離

DNMS本体を分身生成・人格・永続ID・記憶統合・技能学習・人間関係の管理元とする。DNMS-Worldは地形、描画、衝突、身体状態、行動実行、世界スナップショットを担当する。現在の独立生成・ローカル記憶管理は接続前の暫定テスト用であり、DNMSからの生成指示や個体転送は未実装。共通AIの直接再利用は重複実装ではないため維持する。

## 共通World APIの進捗

DNMS本体の DnmsWorldApi を再利用し、NaoSimulation が生成・一覧・時刻取得を実装。V8保存形式でUUIDを永続化し、旧V1〜V7にはスロット由来の暫定UUIDを割り当てる。生成座標は現状、次の区画の基点に限定。WorldApiTest を追加したがCI未確認。Minecraft側Adapter、外部接続、認証、記憶転送は未実装。旧UUIDはDNMS発行IDではなく、世界間の同一性を保証しない。

## 外部World API

環境変数 DNMS_WORLD_TOKEN（32文字以上）を設定すると、127.0.0.1:8080/api/v1/npcs が有効になる。Authorization: Bearer <token> が必要。GET はUUID・座標・体力・tickをJSONで返す。POST はUTF-8の uuid,x,y,z を受け取り、201生成、409重複または上限、400不正座標を返す。既存24人の次の基点は100,1,16。既存ブラウザ操作は維持され、管理元の完全な一本化は未完了。HTTPテスト追加済み、CI未確認。

## 今後の実装

以下は未完了の開発項目。実装済みとCI・実動作検証済みを区別する。

1. CI確認・既存不具合修正
   - DNMS生成API・個体交換形式・管理元認証を実装し、接続テスト後に独立生成とローカル管理の重複経路を無効化する。
2. 個体別の自宅座標管理
3. ベッド・寝室・帰宅行動
4. 個体別人格パラメーター
5. 人格・共有記憶による意思決定
6. 個体別の日課・生活時間
7. 農業・食料備蓄・調理
8. 職業・経済活動・住居管理
9. NPC同士の会話
10. 友情・恋愛・結婚
11. 出産・育児・家族関係
12. 人物台帳の永続化
13. 経験による人格の自動成長
14. 個体別の長期目標
15. 任意の奈央への操作切り替え
16. 世界巻き戻しの完全性検証
17. 256人の長期稼働・負荷試験
18. 性能測定に基づくC++高速化
19. Minecraft版との共通AI互換性検証
20. 無料ゲームとしての一般公開

