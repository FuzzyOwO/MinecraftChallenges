# Shared limbo and game entry validation

Date: 2026-11-21

The automated Gradle checks use Java 25 and the project's configured Paper API dependency. A local Paper server and server configuration were not available in this worktree, so no live-server outcomes are claimed.

The following remain to be exercised on a local Paper server with the built loader JAR: first login, participant and spectator reconnects, pause and resume, combined modes, stale browser selection after reset, restart with an item on the cursor, and all existing modes including MobArmy phase destinations and Bingo resource-pack admission. Confirm no automatic admission or item duplication, and verify real armor, offhand, custom metadata and durability survive serialization. Record the exact Paper build and plugin JAR when that playtest is performed.

Built loader artifact: loader/build/libs/KatzeGames-0.0.1-SNAPSHOT+26.3.jar. This is the plugin JAR, not an installed or tested Paper server build.
