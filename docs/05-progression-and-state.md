# Progression and state: the chest, the sign, the trapdoor, and the phase machine

## The phase machine

The story is a one-way state machine. Model it explicitly instead of scattering booleans across blocks and players.

```java
public enum ProgressionPhase {
    PEACEFUL,
    CHEST_OPENED,
    FOREST,
    ROSE_PLACED,
    THE_OTHER,
    LOCKED
}
```

| Phase | Entered when | World | What is true |
| --- | --- | --- | --- |
| `PEACEFUL` | New world | `theother:peaceful` | House intact, chest closed, sign says "do not open, or else" |
| `CHEST_OPENED` | The basement chest is opened | `theother:peaceful` | Rose is in the player's inventory or chest, sign says "take the rose", the trapdoor exit is armed |
| `FOREST` | The player climbs out of the basement | `theother:forest` | Eternal night, mud road, podium, player can live here indefinitely |
| `ROSE_PLACED` | The rose is placed in the pot | `theother:forest` → cinematic | Darkness begins; input may be frozen briefly |
| `THE_OTHER` | The cinematic ends | `theother:the_other` | One life; death locks the world |
| `LOCKED` | The player dies in The Other | — | World cannot be played; reinstall resets |

The server owns the phase. The client may ask "what phase is this?" for rendering, but it must never advance the phase on its own.

## Where to store state

| State | Scope | Storage | Why |
| --- | --- | --- | --- |
| `phase` | World | `SavedData` in the overworld | Shared story state, survives restart |
| `housePlaced`, `chestPos`, `signPos` | World | `SavedData` | Must be stable across restarts |
| `lifeSpent` | World + player UUID | `SavedData` | The one-life rule is per player in The Other |
| `darkeningStartedAt` | World | `SavedData` | The cinematic must resume correctly after a reload |
| "seen the intro" | Player | `player.getPersistentData()` | Cosmetic, not authoritative |
| Installation token | Instance | A file under `config/theother/` | A reinstall creates a new token (see [One life](06-one-life.md)) |

`SavedData` is the right default. A plain file in the instance is not saved with the world and can desync; use it only for the installation token.

## The world state class

```java
package io.github.sekai0ni0itamio.theother.state;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class TheOtherState extends SavedData {
    private static final String DATA_NAME = "theother_state";

    private ProgressionPhase phase = ProgressionPhase.PEACEFUL;
    private boolean housePlaced;
    private BlockPos chestPos = BlockPos.ZERO;
    private BlockPos signPos = BlockPos.ZERO;
    private boolean lifeSpent;
    private long darkeningStartedAt = -1L;

    public static TheOtherState get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(TheOtherState::load, TheOtherState::new, DATA_NAME);
    }

    public static TheOtherState load(CompoundTag tag) {
        TheOtherState state = new TheOtherState();
        state.phase = readPhase(tag.getString("phase"));
        state.housePlaced = tag.getBoolean("housePlaced");
        state.chestPos = BlockPos.of(tag.getLong("chestPos"));
        state.signPos = BlockPos.of(tag.getLong("signPos"));
        state.lifeSpent = tag.getBoolean("lifeSpent");
        state.darkeningStartedAt = tag.getLong("darkeningStartedAt");
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString("phase", phase.name());
        tag.putBoolean("housePlaced", housePlaced);
        tag.putLong("chestPos", chestPos.asLong());
        tag.putLong("signPos", signPos.asLong());
        tag.putBoolean("lifeSpent", lifeSpent);
        tag.putLong("darkeningStartedAt", darkeningStartedAt);
        return tag;
    }

    private static ProgressionPhase readPhase(String name) {
        try {
            return ProgressionPhase.valueOf(name);
        } catch (IllegalArgumentException exception) {
            return ProgressionPhase.PEACEFUL;
        }
    }

    public ProgressionPhase phase() {
        return phase;
    }

    public void setPhase(ProgressionPhase phase) {
        this.phase = phase;
        setDirty();
    }

    public boolean isHousePlaced() {
        return housePlaced;
    }

    public void setHousePlaced(boolean housePlaced) {
        this.housePlaced = housePlaced;
        setDirty();
    }

    public BlockPos chestPos() {
        return chestPos;
    }

    public void setChestPos(BlockPos chestPos) {
        this.chestPos = chestPos;
        setDirty();
    }

    public BlockPos signPos() {
        return signPos;
    }

    public void setSignPos(BlockPos signPos) {
        this.signPos = signPos;
        setDirty();
    }

    public boolean isLifeSpent() {
        return lifeSpent;
    }

    public void setLifeSpent(boolean lifeSpent) {
        this.lifeSpent = lifeSpent;
        setDirty();
    }

    public long darkeningStartedAt() {
        return darkeningStartedAt;
    }

    public void setDarkeningStartedAt(long darkeningStartedAt) {
        this.darkeningStartedAt = darkeningStartedAt;
        setDirty();
    }
}
```

`computeIfAbsent(load, constructor, name)` is the 1.20.1 `DimensionDataStorage` overload. `setDirty()` is called by every mutator; without it, the data may not be written before the server stops.

## Detecting the chest opening

Two reliable hooks:

### `PlayerInteractEvent.RightClickBlock`

Fires on both sides, before the chest UI opens. Cheap and precise if you stored the chest position.

```java
@SubscribeEvent
public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
        return;
    }

    ServerLevel level = player.serverLevel();
    TheOtherState state = TheOtherState.get(level);

    if (state.phase() != ProgressionPhase.PEACEFUL) {
        return;
    }
    if (!event.getPos().equals(state.chestPos())) {
        return;
    }
    if (!(level.getBlockEntity(event.getPos()) instanceof ChestBlockEntity)) {
        return;
    }

    state.setPhase(ProgressionPhase.CHEST_OPENED);
    updateSign(level, state, "take the rose");
    player.displayClientMessage(Component.translatable("theother.message.rose_taken"), true);
}
```

### `PlayerContainerEvent.Open`

Fires on the server when a container menu opens. This is closer to "the chest was opened" and works even if the player opens it with a hopper or another menu path.

```java
@SubscribeEvent
public static void onContainerOpen(PlayerContainerEvent.Open event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
        return;
    }
    if (!(event.getContainer() instanceof ChestMenu menu)) {
        return;
    }
    if (!(menu.getContainer() instanceof ChestBlockEntity chest)) {
        return;
    }

    ServerLevel level = player.serverLevel();
    TheOtherState state = TheOtherState.get(level);
    if (state.phase() == ProgressionPhase.PEACEFUL && chest.getBlockPos().equals(state.chestPos())) {
        state.setPhase(ProgressionPhase.CHEST_OPENED);
        updateSign(level, state, "take the rose");
    }
}
```

Use one, not both. `PlayerInteractEvent` is easier to reason about; `PlayerContainerEvent.Open` is more semantically exact. If you use both, guard on the phase so the transition runs once.

The rose itself comes from the chest's loot table. The player may open the chest and walk away without taking it; the phase still advances, because "opened" is the trigger, not "took the item". The sign change tells them what to do.

## Changing the sign

1.20.1 signs store `SignText` for each side. Update the front text through the block entity:

```java
private static void updateSign(ServerLevel level, TheOtherState state, String line) {
    if (!(level.getBlockEntity(state.signPos()) instanceof SignBlockEntity sign)) {
        return;
    }

    SignText front = sign.getFrontText().setMessage(0, Component.literal(line));
    sign.setText(front, true);
    sign.setChanged();
}
```

- `getFrontText()` and `setText(SignText, boolean front)` are the 1.20.1 API.
- `SignText#setMessage(int line, Component text)` returns a new `SignText`; it is immutable-style, so assign the result.
- `setChanged()` marks the chunk dirty. Without it, the old text can reappear after a reload.
- The sign's `is_waxed` flag prevents editing by players; the block entity API still works. If the sign is waxed, `setText` is the correct path.
- If the sign is not loaded, `level.getBlockEntity` returns `null`. Load the chunk with `level.getChunkAt(state.signPos())` first, or perform the update from a command/block event where the chunk is loaded.

## Arming the trapdoor exit

The plan says the player climbs the ladder and emerges in the forest instead of the house. Detect the exit and teleport.

### Option A: a custom trapdoor block (recommended)

Place a custom `TrapDoorBlock` in the house NBT. Override `use`:

```java
public final class StoryTrapdoorBlock extends TrapDoorBlock {
    public StoryTrapdoorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = super.use(state, level, pos, player, hand, hit);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            TheOtherState story = TheOtherState.get(serverPlayer.serverLevel());
            if (story.phase() == ProgressionPhase.CHEST_OPENED) {
                // Schedule the teleport after the trapdoor animation.
                serverPlayer.server.tell(new TickTask(serverPlayer.server.getTickCount() + 10,
                        () -> sendToForest(serverPlayer)));
            }
        }
        return result;
    }
}
```

- `TrapDoorBlock` has a constructor that takes `BlockBehaviour.Properties` in 1.20.1.
- `server.tell(new TickTask(...))` schedules on the server thread. If the mapping differs, use a small tick counter in `ServerTickEvent` keyed by player UUID.
- The player may climb the ladder without right-clicking the trapdoor; the vanilla trapdoor opens when clicked. If you want "climbing out" to be enough, use Option B.

### Option B: a position check

On `PlayerTickEvent`, if the player is in the peaceful dimension, the phase is `CHEST_OPENED`, and the player is above the trapdoor (or within a small radius of the ladder top), teleport them to the forest once.

```java
@SubscribeEvent
public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
        return;
    }
    if (player.tickCount % 10 != 0 || !(player.level() instanceof ServerLevel level)) {
        return;
    }

    TheOtherState state = TheOtherState.get(level);
    if (state.phase() != ProgressionPhase.CHEST_OPENED || !level.dimension().location().equals(PEACEFUL_ID)) {
        return;
    }

    if (player.blockPosition().closerThan(EXIT_POS, 3.0D)) {
        state.setPhase(ProgressionPhase.FOREST);
        sendToForest(player);
    }
}
```

- Use a radius, not an exact block, or the player can slip past on a ladder.
- The phase check prevents re-teleporting after the player is already in the forest.
- `player.tickCount % 10` keeps the scan cheap; a 10-tick delay is invisible to the player.
- If the player is in a vehicle or has a passenger, teleport them together or dismount first.

### The basement stays behind

If the basement remains in the peaceful dimension, the player can never return after the teleport. That matches "the trapdoor no longer leads to the house". If you want the basement to remain visible, build a copy in the forest at the arrival point, or teleport the player to a forest copy of the basement room. Do not try to move the same chunk between dimensions; chunks belong to a level.

## Advancing to The Other

When the rose is placed in the pot:

1. Set `ROSE_PLACED`.
2. Send a client packet that starts the darkening overlay.
3. Freeze player input or apply a movement penalty (optional).
4. After the overlay reaches black, teleport to `theother:the_other`.
5. Set `THE_OTHER`.
6. Register the death lock (see [One life](06-one-life.md)).

The pot can be a custom block with a `use` handler, or a `BlockEntity` that checks the held item. The simplest 1.20.1 implementation is a custom block with a `use` override:

```java
if (!level.isClientSide()
        && player instanceof ServerPlayer serverPlayer
        && player.getMainHandItem().is(ModItems.ENCHANTED_ROSE.get())) {
    if (!player.getAbilities().instabuild) {
        player.getMainHandItem().shrink(1);
    }
    story.setPhase(ProgressionPhase.ROSE_PLACED);
    story.setDarkeningStartedAt(level.getGameTime());
    ModNetwork.startDarkening(serverPlayer, 60);
}
```

The block should also render a filled pot state when the rose is present. Use a `BlockState` boolean property (`filled`) and two models, or a `BlockEntity` with a renderer. A blockstate property is cheaper and sufficient.

## Syncing state to the client

The client needs two things:

1. **Phase** — for ambient music, the title screen banner, and the darkening overlay.
2. **Cinematic timer** — for the fade.

Send a small packet on login and on every phase change:

```java
public record PhasePayload(ProgressionPhase phase) {
    public static void encode(PhasePayload payload, FriendlyByteBuf buffer) {
        buffer.writeEnum(payload.phase());
    }

    public static PhasePayload decode(FriendlyByteBuf buffer) {
        return new PhasePayload(buffer.readEnum(ProgressionPhase.class));
    }

    public static void handle(PhasePayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientStoryState.setPhase(payload.phase()));
        context.setPacketHandled(true);
    }
}
```

`ClientStoryState` is a client-only static holder. It is not authoritative; it only drives rendering. On logout, reset it to `PEACEFUL` so a new world does not inherit the old overlay.

Send on:

- `PlayerLoggedInEvent` (sync the current phase)
- Every `setPhase` transition (server → affected players)
- `PlayerChangedDimensionEvent` (resync after a teleport)

For multiplayer, send to all players in the relevant level, or only to the player whose story is advancing if each player has a personal run. Decide explicitly; the plan is single-player-shaped, so a world-level phase is acceptable for the first version.

## Player-specific data

`player.getPersistentData()` is a `CompoundTag` saved with the player. It is convenient for cosmetics:

```java
CompoundTag data = player.getPersistentData();
data.putBoolean("theother.introSeen", true);
```

It is copied on respawn by Forge. If you need to copy custom data explicitly, use `PlayerEvent.Clone`:

```java
@SubscribeEvent
public static void onClone(PlayerEvent.Clone event) {
    if (event.isWasDeath()) {
        event.getEntity().getPersistentData().merge(event.getOriginal().getPersistentData());
    }
}
```

Do not store `lifeSpent` only in persistent data. A player can edit it, and a new player entity can lose it. The world `SavedData` is the authority.

## Idempotency and reload safety

Every transition must be safe to run twice:

- Check the phase before mutating.
- Use `SavedData` flags for one-time world edits (house placed, chest position recorded).
- If the server crashes mid-cinematic, `darkeningStartedAt` lets you resume or skip to the end. Decide: resume is atmospheric, skip is safer.
- Do not advance a phase from a client packet without a server-side validation of the player's position and inventory.
- On `ServerStoppingEvent`, call `setDirty()` on any state you mutated since the last save. `SavedData` is saved automatically, but explicit dirtying is cheap insurance.

## Testing the progression

```mcfunction
# Give yourself the rose for testing
give @s theother:enchanted_rose

# Watch the phase transition in the log
# (add a LOGGER.info in setPhase while developing)

# Teleport to each dimension manually
execute in theother:peaceful run tp @s 8 5 8
execute in theother:forest run tp @s 0 80 0
execute in theother:the_other run tp @s 0 80 0
```

Manual checks:

- [ ] Opening the chest once changes the sign and does not repeat on a second open.
- [ ] Relogging after the chest is open keeps the sign changed.
- [ ] Climbing out of the basement arrives in the forest, not the house.
- [ ] Returning to the peaceful dimension manually does not reset the phase.
- [ ] Placing the rose starts the overlay once; cancelling the overlay does not leave the player in a half-state.
- [ ] A server restart during `CHEST_OPENED` preserves the phase and the chest position.
- [ ] A second player in the same world sees the same phase (if the story is shared).

## Pitfalls

| Symptom | Cause | Fix |
| --- | --- | --- |
| Sign reverts after restart | `setChanged()` missing | Call it on the block entity |
| Chest triggers twice | Both interact and container events run | Use one hook, or guard on the phase |
| Phase resets on restart | State stored in a static field | Use `SavedData` |
| Teleport loops | No phase change before the teleport | Set `FOREST` before moving the player |
| Player falls through the forest | Destination chunks not loaded | Use `changeDimension`, not a raw position set |
| Client shows the old phase | No sync packet on login | Send on `PlayerLoggedInEvent` |
| Multiplayer desync | Per-player state treated as world state | Decide the scope and store it explicitly |
| Sign text is blank | `setMessage` result ignored | Reassign the returned `SignText` |
| Duplicate house | No `housePlaced` flag | Guard with `SavedData` |

## Sources

- Forge 1.20.1 events: <https://docs.minecraftforge.net/en/1.20.1/events/>
- `SavedData` and `DimensionDataStorage`: <https://mappings.dev/1.20.1/net/minecraft/world/level/saveddata/SavedData.html>
- Minecraft Wiki, sign: <https://minecraft.wiki/w/Sign>
- Minecraft Wiki, chest: <https://minecraft.wiki/w/Chest>
- Forge `PlayerContainerEvent`: <https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/event/entity/player/PlayerContainerEvent.java>
