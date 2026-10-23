# One life: permadeath, the world lock, and the reinstall reset

## The rule, stated precisely

1. The peaceful world and the forest are **survival**. Death there is normal; the player respawns and continues.
2. The Other is **one life per player**. The first death in `theother:the_other` spends the life.
3. After the life is spent, the player cannot re-enter The Other in that installation. The world may remain on disk, but the story is over.
4. A fresh installation of the modpack creates a new installation token, which resets the lock. That is the in-fiction "redownload and reinstall" reset.

"One life" is a **server-side rule**. The client can render a lock screen, but it must not decide whether the life is spent.

## What "cannot play this world anymore" means

The plan can be read in two ways:

- **Lock the player out of The Other** — the world is still playable, but the one-life dimension is closed.
- **Lock the entire world** — the save cannot be loaded at all.

For a singleplayer story, the second reading is stronger and matches "cannot play this world anymore." Implement it as a **world-level lock for the owning player**, not by deleting the save:

- Keep the world file intact. Deleting a player's world is hostile and makes bug reports impossible.
- On login, if the player's life is spent for this installation, disconnect with a clear message.
- Do not merely set spectator mode; the player could switch back.
- Do not rely on hardcore mode; the plan explicitly says survival, and hardcore changes the death screen and difficulty semantics.

For multiplayer, lock the **player**, not the world. Other players may still play. The plan is single-player-shaped; document the scope you ship.

## Event flow

```text
LivingDeathEvent
  └─ entity is ServerPlayer
  └─ level is theother:the_other
  └─ life not already spent
       ├─ mark life spent (SavedData, with current install id)
       ├─ set phase LOCKED
       └─ let the death screen play

PlayerEvent.PlayerRespawnEvent
  └─ if life spent for this install
       └─ disconnect with "the story ends here"

PlayerEvent.PlayerLoggedInEvent
  └─ if life spent for this install
       └─ disconnect

PlayerEvent.PlayerChangedDimensionEvent
  └─ to the_other and life spent
       └─ send back / disconnect (safety net)

PlayerTickEvent (optional safety net)
  └─ in the_other and life spent
       └─ disconnect
```

Marking on death and disconnecting on respawn lets the vanilla death animation and death screen happen. Kicking inside `LivingDeathEvent` can cut the death screen off and look like a crash.

## The life record

Store one record per player UUID in the world `SavedData`:

```java
public record LifeRecord(boolean spent, String installationId) {
}
```

`TheOtherState` additions:

```java
private final Map<UUID, LifeRecord> lives = new HashMap<>();

public boolean isLifeSpent(UUID playerId, String installationId) {
    LifeRecord record = lives.get(playerId);
    return record != null && record.spent() && record.installationId().equals(installationId);
}

public void spendLife(UUID playerId, String installationId) {
    lives.put(playerId, new LifeRecord(true, installationId));
    setDirty();
}

public void resetLife(UUID playerId, String installationId) {
    lives.put(playerId, new LifeRecord(false, installationId));
    setDirty();
}
```

Save and load with a `ListTag` of `CompoundTag`s:

```java
@Override
public CompoundTag save(CompoundTag tag) {
    ListTag list = new ListTag();
    lives.forEach((uuid, record) -> {
        CompoundTag entry = new CompoundTag();
        entry.putUUID("player", uuid);
        entry.putBoolean("spent", record.spent());
        entry.putString("installation", record.installationId());
        list.add(entry);
    });
    tag.put("lives", list);
    return tag;
}

public static TheOtherState load(CompoundTag tag) {
    TheOtherState state = new TheOtherState();
    ListTag list = tag.getList("lives", Tag.TAG_COMPOUND);
    for (int i = 0; i < list.size(); i++) {
        CompoundTag entry = list.getCompound(i);
        state.lives.put(entry.getUUID("player"),
                new LifeRecord(entry.getBoolean("spent"), entry.getString("installation")));
    }
    return state;
}
```

`CompoundTag#putUUID` and `getUUID` exist in 1.20.1. `Tag.TAG_COMPOUND` is the type constant.

## The installation token

The reset is tied to an installation, not to a world file. Generate a random token on first launch and keep it outside the world:

```java
public final class InstallationToken {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static String cached;

    public static String get() {
        if (cached != null) {
            return cached;
        }
        Path path = FMLPaths.CONFIGDIR.get().resolve("theother").resolve("installation.json");
        try {
            if (Files.exists(path)) {
                JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                cached = json.get("id").getAsString();
                return cached;
            }
            cached = UUID.randomUUID().toString();
            Files.createDirectories(path.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("id", cached);
            Files.writeString(path, GSON.toJson(json));
            return cached;
        } catch (IOException exception) {
            TheOther.LOGGER.error("Could not read or create the installation token", exception);
            cached = UUID.randomUUID().toString();
            return cached;
        }
    }
}
```

`FMLPaths.CONFIGDIR` is the instance's `config/` directory. A fresh modpack installation has no `config/theother/installation.json`, so a new token is generated.

Why not store the token in the world?

- The rule is "reinstall the modpack", not "delete the world".
- A token in the world survives a world copy and makes the reset impossible without editing the save.

Why not store the token in the world **and** the config?

- The world record says which installation spent the life. The config says which installation is running now. Both are needed to decide.
- If they match, block. If they differ, the player is on a new installation; reset the record.

## The handlers

```java
@Mod.EventBusSubscriber(modid = TheOther.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OneLifeEvents {
    private static final ResourceLocation THE_OTHER =
            new ResourceLocation(TheOther.MODID, "the_other");

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.level().dimension().location().equals(THE_OTHER)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        TheOtherState state = TheOtherState.get(level);
        UUID id = player.getUUID();
        String install = InstallationToken.get();

        if (!state.isLifeSpent(id, install)) {
            state.spendLife(id, install);
            state.setPhase(ProgressionPhase.LOCKED);
            TheOther.LOGGER.info("Life spent: {} in {}", player.getGameProfile().getName(), THE_OTHER);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TheOtherState state = TheOtherState.get(player.serverLevel());
        if (state.isLifeSpent(player.getUUID(), InstallationToken.get())) {
            player.connection.disconnect(Component.translatable("theother.disconnect.life_spent"));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TheOtherState state = TheOtherState.get(player.serverLevel());
        if (state.isLifeSpent(player.getUUID(), InstallationToken.get())) {
            player.connection.disconnect(Component.translatable("theother.disconnect.life_spent"));
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getTo().location().equals(THE_OTHER)) {
            return;
        }
        TheOtherState state = TheOtherState.get(player.serverLevel());
        if (state.isLifeSpent(player.getUUID(), InstallationToken.get())) {
            player.connection.disconnect(Component.translatable("theother.disconnect.life_spent"));
        }
    }
}
```

Notes:

- `event.getEntity()` on `PlayerEvent` returns `Player`; the `instanceof ServerPlayer` check is required.
- `player.serverLevel()` is the 1.20.1 method that returns the `ServerLevel` the player is in.
- `disconnect(Component)` is on `ServerGamePacketListenerImpl`; `player.connection` is public in 1.20.1.
- `PlayerChangedDimensionEvent` may not be cancellable. A disconnect is the reliable enforcement; a "send back" requires a scheduled teleport.
- `LivingDeathEvent` fires for `/kill`, void damage, explosions, and normal damage. It does not fire for `player.setHealth(0)`? It does; `setHealth(0)` triggers `die`. It does not fire if the player is already dead or in spectator mode.
- Do not call `disconnect` in `LivingDeathEvent`; let the death screen play and disconnect on respawn.

## Resetting on a new installation

On login, before the lock check:

```java
TheOtherState state = TheOtherState.get(player.serverLevel());
LifeRecord record = state.lifeRecord(player.getUUID());
String install = InstallationToken.get();

if (record != null && record.spent() && !record.installationId().equals(install)) {
    state.resetLife(player.getUUID(), install);
    player.displayClientMessage(Component.translatable("theother.message.new_installation"), false);
}
```

This is the "redownload and reinstall" behavior. It is intentionally permissive: a new installation is treated as a new run. It is also the main bypass, so do not pretend it is secure.

If the story is single-player only, gate the reset to the world owner. If a friend joins a LAN world from a different install, their record would reset independently; that may be fine, or it may be an exploit.

## Entering The Other

Before the teleport:

- Force survival: `player.setGameMode(GameType.SURVIVAL)`. A creative player should not skip the one-life rule by flying.
- Clear any respawn point that would put them back inside The Other: `player.setRespawnPosition(Level.OVERWORLD, overworldSpawn, 0.0F, false, false)`. Check the mapped signature; 1.20.1 uses `(ResourceKey<Level>, BlockPos, float, boolean, boolean)`.
- Do not give the player a bed or anchor that works in The Other. The dimension type should have `bed_works: false` and `respawn_anchor_works: false`.
- Send the phase packet and the entry message.

## What the player sees

- **On death**: the vanilla death screen, then a custom disconnect message. The message should be clear, not a crash-looking stack trace:
  - "The Other has taken its life."
  - "This world is closed. A fresh installation begins a new story."
- **On login after death**: the same message, immediately. Do not load them into the world and then kick; disconnect during login.
- **On reinstall**: a subtle "a new story begins" message, not a celebration. The horror tone depends on restraint.

Add the lang keys to `assets/theother/lang/en_us.json`:

```json
{
  "theother.disconnect.life_spent": "The Other has taken its life. This world is closed.",
  "theother.message.new_installation": "A new story begins.",
  "theother.message.rose_taken": "The sign has changed."
}
```

## Anti-tamper reality

A local mod cannot be DRM. Be honest about the boundary:

- The player owns the files. They can edit the config token, the world NBT, or the mod jar.
- Obfuscation is not security. A determined player will bypass any client-side check.
- The server is authoritative only while the player respects the server. In singleplayer, the player is the server admin.
- Do not add malware, hidden files outside the instance, or destructive anti-tamper. It breaks trust and antivirus heuristics, and it does not stop a determined user.
- The intended experience is an **honor-system lock** reinforced by atmosphere, not a cryptographic guarantee.

If the project later ships a dedicated server, the server operator can enforce the rule for all players, and the client cannot bypass it. That is the only version that is actually enforceable.

## Edge cases

| Case | Behavior | Why |
| --- | --- | --- |
| Death in the peaceful world | Normal respawn | The one-life rule is scoped to The Other |
| Death in the forest | Normal respawn | The forest is explicitly livable |
| Death in The Other by `/kill` | Life spent | `LivingDeathEvent` fires |
| Death in The Other by void | Life spent | Void damage kills |
| Quit during the death screen | Lock persists; login is blocked | The record is saved before the respawn |
| Server crash before autosave | May lose the lock | Mark dirty and accept; a hard crash can lose the last seconds |
| Restore an old world backup | The backup's record may predate the death | Backups are outside the rule; document it |
| Delete `config/theother/installation.json` | New token, life resets | This is the reinstall reset, by design |
| Copy the world to a new install | Life resets | The token is per installation |
| Change gamemode to creative | Does not undo a spent life | The lock is checked on login and dimension change |
| Spectator mode | Cannot die; can still be locked out | Force survival on entry |
| Multiplayer friend dies | Their record is spent; the world remains | Per-player records |
| LAN world owner reinstalls | Their record resets | Intended |
| `keepInventory` | Death still counts | The rule is death, not inventory loss |
| Hardcore mode | Not used | Survival is the stated mode |

## Testing matrix

| Test | Steps | Expected |
| --- | --- | --- |
| Peaceful death | Die in `theother:peaceful`, respawn | No lock |
| Forest death | Die in `theother:forest`, respawn | No lock |
| The Other death | Die in `theother:the_other` | Death screen, then disconnect on respawn |
| Relog after death | Close the game, reopen, join | Disconnect on login |
| New install | Delete `config/theother/installation.json`, join | A new life is allowed |
| Dimension bypass | `/execute in theother:the_other run tp @s ...` after death | Disconnect / blocked |
| Creative bypass | `/gamemode creative`, enter The Other | Forced survival |
| Multiplayer | Two players, one dies | Only that player is locked |
| Backup restore | Restore a pre-death world backup | Depends on the backup; document the result |

## Alternatives and why not

| Alternative | Why not |
| --- | --- |
| Hardcore mode | Changes the death screen, difficulty, and world semantics; the plan says survival |
| Delete the world on death | Destructive and hostile; destroys bug reports and player trust |
| Spectator mode | Reversible; the player can switch back |
| Health lock at 1 heart | Still allows play; does not match "cannot play this world anymore" |
| A server-side plugin | Only works on a dedicated server; the modpack is single-player-first |
| Encrypting the world | Not feasible or desirable for a mod; the player owns the files |

## Sources

- Forge 1.20.1 events: <https://docs.minecraftforge.net/en/1.20.1/events/>
- `LivingDeathEvent`: <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/event/entity/living/LivingDeathEvent.java>
- `PlayerEvent`: <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/event/entity/player/PlayerEvent.java>
- `FMLPaths`: <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/fml/loading/FMLPaths.java>
- Minecraft Wiki, hardcore: <https://minecraft.wiki/w/Hardcore>
