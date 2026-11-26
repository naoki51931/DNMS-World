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
