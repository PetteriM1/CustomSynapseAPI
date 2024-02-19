package org.itxtech.synapseapi;

import cn.nukkit.AdventureSettings;
import cn.nukkit.AdventureSettings.Type;
import cn.nukkit.Player;
import cn.nukkit.PlayerFood;
import cn.nukkit.Server;
import cn.nukkit.event.player.*;
import cn.nukkit.event.server.DataPacketSendEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.*;
import cn.nukkit.network.SourceInterface;
import cn.nukkit.network.protocol.*;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.TextFormat;
import org.itxtech.synapseapi.event.player.SynapseFullServerPlayerTransferEvent;
import org.itxtech.synapseapi.event.player.SynapsePlayerConnectEvent;
import org.itxtech.synapseapi.event.player.SynapsePlayerTransferEvent;
import org.itxtech.synapseapi.network.protocol.spp.PlayerLoginPacket;
import org.itxtech.synapseapi.network.protocol.spp.TransferPacket;
import org.itxtech.synapseapi.utils.ClientData;
import org.itxtech.synapseapi.utils.ClientData.Entry;
import org.itxtech.synapseapi.utils.DataPacketEidReplacer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by boybook on 16/6/24.
 */
public class SynapsePlayer extends Player {

    public static final long REPLACE_ID = Long.MAX_VALUE;

    protected SynapseEntry synapseEntry;
    private boolean isFirstTimeLogin;
    private boolean connectedToCurrentInstance = true;
    private boolean joinedAsDead;
    private static final Method updateName;

    static {
        try {
            updateName = Server.class.getDeclaredMethod("updateName", UUID.class, String.class);
            updateName.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public SynapsePlayer(SourceInterface interfaz, SynapseEntry synapseEntry, Long clientID, InetSocketAddress address) {
        super(interfaz, clientID, address);
        this.synapseEntry = synapseEntry;
    }

    private static int getClientFriendlyGamemode(int gamemode) {
        gamemode &= 0x03;
        if (gamemode == Player.SPECTATOR) {
            return Player.CREATIVE;
        }
        return gamemode;
    }

    public void handleLoginPacket(PlayerLoginPacket packet) {
        this.isFirstTimeLogin = packet.isFirstTime;
        SynapsePlayerConnectEvent ev;
        this.server.getPluginManager().callEvent(ev = new SynapsePlayerConnectEvent(this, this.isFirstTimeLogin));
        if (!ev.isCancelled()) {
            DataPacket pk = SynapseAPI.getInstance().getPacket(packet.cachedLoginPacket);
            pk.setOffset(1);
            pk.decode();
            this.handleDataPacket(pk);
        }
    }

    @Override
    public void handleDataPacket(DataPacket packet) {
        if (this.connectedToCurrentInstance) {
            super.handleDataPacket(DataPacketEidReplacer.replaceBack(packet, REPLACE_ID, this.getId()));
        }
    }

    @Override
    public void close() {
        super.close();
        this.connectedToCurrentInstance = false;
    }

    public SynapseEntry getSynapseEntry() {
        return synapseEntry;
    }

    public boolean isFirstTimeLogin() {
        return this.isFirstTimeLogin;
    }

    public boolean isConnectedToCurrentInstance() {
        return this.connectedToCurrentInstance;
    }

    @Override
    protected void processLogin() {
        if (!this.server.isWhitelisted((this.getName()).toLowerCase())) {
            this.kick(PlayerKickEvent.Reason.NOT_WHITELISTED, "Server is white-listed");
            return;
        } else if (this.isBanned()) {
            String reason = this.server.getNameBans().getEntires().get(this.getName().toLowerCase()).getReason();
            this.kick(PlayerKickEvent.Reason.NAME_BANNED, reason);
            return;
        } else if (this.server.getIPBans().isBanned(this.getAddress())) {
            String reason = this.server.getIPBans().getEntires().get(this.getAddress()).getReason();
            this.kick(PlayerKickEvent.Reason.IP_BANNED, reason);
            return;
        }

        Player oldPlayer = null;
        for (Player p : new ArrayList<>(this.server.getOnlinePlayers().values())) {
            if (p != this && p.getName() != null && p.getName().equalsIgnoreCase(this.getName()) ||
                    this.getUniqueId().equals(p.getUniqueId())) {
                oldPlayer = p;
                break;
            }
        }

        if (oldPlayer != null) {
            oldPlayer.saveNBT();
            CompoundTag nbt = oldPlayer.namedTag;
            oldPlayer.close("", "disconnectionScreen.loggedinOtherLocation");
            continueProcessLogin(nbt);
        } else {
            continueProcessLogin(loadNBT());
        }
    }

    private CompoundTag loadNBT() {
        CompoundTag nbt;
        File legacyDataFile = new File(server.getDataPath() + "players/" + this.username.toLowerCase() + ".dat");
        File dataFile = new File(server.getDataPath() + "players/" + this.uuid.toString() + ".dat");
        boolean dataFound = dataFile.exists();
        if (!dataFound && legacyDataFile.exists()) {
            nbt = this.server.getOfflinePlayerData(this.username, false);
            if (!legacyDataFile.delete()) {
                this.server.getLogger().warning("Could not delete legacy player data for " + this.username);
            }
        } else {
            nbt = this.server.getOfflinePlayerData(this.uuid, !dataFound);
        }
        return nbt;
    }

    private void continueProcessLogin(CompoundTag nbt) {
        if (nbt == null) {
            this.close(this.getLeaveMessage(), "Invalid data");
            return;
        }

        if (this.getLoginChainData().isXboxAuthed() || !server.getPropertyBoolean("xbox-auth", true)) {
            try {
                updateName.invoke(server, this.uuid, this.username);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }

        this.playedBefore = (nbt.getLong("lastPlayed") - nbt.getLong("firstPlayed")) > 1;

        nbt.putString("NameTag", this.username);

        if (nbt.getShort("Health") < 1) {
            joinedAsDead = true;
        }

        this.setExperience(nbt.getInt("EXP"), nbt.getInt("expLevel"));

        if (this.server.getForceGamemode()) {
            this.gamemode = this.server.getGamemode();
            nbt.putInt("playerGameType", this.gamemode);
        } else {
            this.gamemode = nbt.getInt("playerGameType") & 0x03;
        }

        this.adventureSettings = new AdventureSettings(this)
                .set(Type.WORLD_IMMUTABLE, isAdventure() || isSpectator())
                .set(Type.MINE, !isAdventure() && !isSpectator())
                .set(Type.BUILD, !isAdventure() && !isSpectator())
                .set(Type.NO_PVM, this.isSpectator())
                .set(Type.AUTO_JUMP, true)
                .set(Type.ALLOW_FLIGHT, isCreative())
                .set(Type.NO_CLIP, isSpectator());

        Level level;
        if (SynapseAPI.alwaysSpawn || joinedAsDead || (level = this.server.getLevelByName(nbt.getString("Level"))) == null) {
            this.setLevel(this.server.getDefaultLevel());
            nbt.putString("Level", this.level.getName());
            Position sp = this.level.getSpawnLocation();
            nbt.getList("Pos", DoubleTag.class)
                    .add(new DoubleTag("0", sp.x))
                    .add(new DoubleTag("1", sp.y))
                    .add(new DoubleTag("2", sp.z));
        } else {
            this.setLevel(level);
        }

        if (nbt.contains("SpawnLevel")) {
            Level spawnLevel = server.getLevelByName(nbt.getString("SpawnLevel"));
            if (spawnLevel != null) {
                this.spawnPosition = new Position(
                        nbt.getInt("SpawnX"),
                        nbt.getInt("SpawnY"),
                        nbt.getInt("SpawnZ"),
                        spawnLevel
                );
            }
        }

        nbt.putLong("lastPlayed", System.currentTimeMillis() / 1000);

        UUID uuid = getUniqueId();
        nbt.putLong("UUIDLeast", uuid.getLeastSignificantBits());
        nbt.putLong("UUIDMost", uuid.getMostSignificantBits());

        if (this.server.getAutoSave()) {
            this.server.saveOfflinePlayerData(this.uuid, nbt, true);
        }

        for (Tag achievement : nbt.getCompound("Achievements").getAllTags()) {
            if (!(achievement instanceof ByteTag)) {
                continue;
            }

            if (((ByteTag) achievement).getData() > 0) {
                this.achievements.add(achievement.getName());
            }
        }

        if (this.isFirstTimeLogin) {
            this.sendPlayStatus(PlayStatusPacket.LOGIN_SUCCESS);
        }

        ListTag<DoubleTag> posList = nbt.getList("Pos", DoubleTag.class);

        super.init(this.level.getChunk(NukkitMath.floorDouble(posList.get(0).data) >> 4, NukkitMath.floorDouble(posList.get(2).data) >> 4, true), nbt);

        if (!this.namedTag.contains("foodLevel")) {
            this.namedTag.putInt("foodLevel", 20);
        }
        int foodLevel = this.namedTag.getInt("foodLevel");
        if (!this.namedTag.contains("FoodSaturationLevel")) {
            this.namedTag.putFloat("FoodSaturationLevel", 20);
        }
        float foodSaturationLevel = this.namedTag.getFloat("foodSaturationLevel");
        this.foodData = new PlayerFood(this, foodLevel, foodSaturationLevel);

        if (this.isSpectator()) this.keepMovement = true;

        this.forceMovement = this.teleportPosition = this.getPosition();

        if (this.isFirstTimeLogin) {
            ResourcePacksInfoPacket infoPacket = new ResourcePacksInfoPacket();
            infoPacket.resourcePackEntries = this.server.getResourcePackManager().getResourceStack();
            infoPacket.mustAccept = this.server.getForceResources();
            this.dataPacket(infoPacket);
        } else {
            this.shouldLogin = true;
        }
    }

    @Override
    protected void completeLoginSequence() {
        if (this.loggedIn) {
            this.server.getLogger().warning("(BUG) Tried to call completeLoginSequence but player is already logged in");
            return;
        }

        PlayerLoginEvent ev;
        this.server.getPluginManager().callEvent(ev = new PlayerLoginEvent(this, "Plugin reason"));
        if (ev.isCancelled()) {
            this.close(this.getLeaveMessage(), ev.getKickMessage());
            return;
        }

        if (this.isFirstTimeLogin) {
            StartGamePacket startGamePacket = new StartGamePacket();
            startGamePacket.entityUniqueId = REPLACE_ID;
            startGamePacket.entityRuntimeId = REPLACE_ID;
            startGamePacket.playerGamemode = getClientFriendlyGamemode(this.gamemode);
            startGamePacket.x = (float) this.x;
            startGamePacket.y = (float) this.y;
            startGamePacket.z = (float) this.z;
            startGamePacket.yaw = (float) this.yaw;
            startGamePacket.pitch = (float) this.pitch;
            startGamePacket.dimension = 0/*(byte) (this.level.getDimension() & 0xff)*/;
            startGamePacket.worldGamemode = getClientFriendlyGamemode(this.gamemode);
            startGamePacket.difficulty = this.server.getDifficulty();
            if (this.level.getProvider() == null || this.level.getProvider().getSpawn() == null) {
                startGamePacket.spawnX = (int) this.x;
                startGamePacket.spawnY = (int) this.y;
                startGamePacket.spawnZ = (int) this.z;
            } else {
                Vector3 spawn = this.level.getProvider().getSpawn();
                startGamePacket.spawnX = (int) spawn.x;
                startGamePacket.spawnY = (int) spawn.y;
                startGamePacket.spawnZ = (int) spawn.z;
            }
            startGamePacket.commandsEnabled = this.enableClientCommand;
            startGamePacket.gameRules = this.getLevel().getGameRules();
            startGamePacket.worldName = this.getServer().getNetwork().getName();
            if (this.getLevel().isRaining()) {
                startGamePacket.rainLevel = this.getLevel().getRainTime();
                if (this.getLevel().isThundering()) {
                    startGamePacket.lightningLevel = this.getLevel().getThunderTime();
                }
            }
            startGamePacket.isMovementServerAuthoritative = true;
            this.directDataPacket(startGamePacket);
        }

        this.noDamageTicks = 100;
        this.loggedIn = true;

        this.server.getLogger().info(this.getServer().getLanguage().translateString("nukkit.player.logIn",
                        TextFormat.AQUA + this.username + TextFormat.WHITE,
                        this.getAddress(),
                        String.valueOf(this.getPort()),
                        String.valueOf(this.id),
                        this.level.getName(),
                        String.valueOf(NukkitMath.round(this.x, 4)),
                        String.valueOf(NukkitMath.round(this.y, 4)),
                        String.valueOf(NukkitMath.round(this.z, 4))));

        try {
            if (this.isFirstTimeLogin) {
                this.dataPacket(new BiomeDefinitionListPacket());
                this.dataPacket(new AvailableEntityIdentifiersPacket());
            }

            this.getLevel().sendTime(this);

            SetDifficultyPacket diffucultyPK = new SetDifficultyPacket();
            diffucultyPK.difficulty = this.getServer().getDifficulty();
            this.dataPacket(diffucultyPK);
            SetCommandsEnabledPacket enableCommandsPK = new SetCommandsEnabledPacket();
            enableCommandsPK.enabled = this.isEnableClientCommand();
            this.dataPacket(enableCommandsPK);

            if (this.isEnableClientCommand()) {
                this.getServer().getScheduler().scheduleDelayedTask(null, () -> {
                    if (this.isOnline()) {
                        this.sendCommandData();
                    }
                }, 2);
            }
            this.adventureSettings.update();

            GameRulesChangedPacket gameRulesPK = new GameRulesChangedPacket();
            gameRulesPK.gameRules = level.getGameRules();
            this.dataPacket(gameRulesPK);

            if (this.isFirstTimeLogin) {
                Map<UUID, Player> tempOnlinePlayers = getServer().getOnlinePlayers();
                CompletableFuture.runAsync(() -> sendFullPlayerListInternal(tempOnlinePlayers));
            }

            this.sendAttributes();

            this.inventory.sendCreativeContents();
            this.sendAllInventories();
            this.inventory.sendHeldItem(this);
            this.server.sendRecipeList(this);

            if (!this.isFirstTimeLogin) {
                SetPlayerGameTypePacket pk = new SetPlayerGameTypePacket();
                pk.gamemode = getClientFriendlyGamemode(gamemode);
                this.dataPacket(pk);
            }

            this.sendPotionEffects(this);
            this.sendData(this);
            this.setCanClimb(true);
            this.setNameTagVisible(true);
            this.setNameTagAlwaysVisible(true);

            if (this.isOp() || this.hasPermission("nukkit.textcolor")) {
                this.setRemoveFormat(false);
            }
        } catch (Exception e) {
            this.close("", "Internal Server Error");
            getServer().getLogger().logException(e);
        }

        this.server.addOnlinePlayer(this);

        ChunkRadiusUpdatedPacket chunkRadiusUpdatePacket = new ChunkRadiusUpdatedPacket();
        chunkRadiusUpdatePacket.radius = this.chunkRadius;
        this.dataPacket(chunkRadiusUpdatePacket);

        if (!this.isFirstTimeLogin) {
            this.doFirstSpawn();
        }
    }

    private void sendFullPlayerListInternal(Map<UUID, Player> playerList) {
        PlayerListPacket pk = new PlayerListPacket();
        pk.type = PlayerListPacket.TYPE_ADD;
        pk.entries = playerList.values().stream()
                .map(p -> new PlayerListPacket.Entry(
                        p.getUniqueId(),
                        p.getId(),
                        p.getDisplayName(),
                        p.getSkin(),
                        p.getLoginChainData().getXUID()))
                .toArray(PlayerListPacket.Entry[]::new);
        pk = (PlayerListPacket) DataPacketEidReplacer.replace(pk, this.getId(), REPLACE_ID);
        pk.tryEncode();
        this.interfaz.putPacket(this, pk.compress(9), false, true);
    }

    public boolean transferByDescription(String serverDescription) {
        return this.transfer(this.getSynapseEntry().getClientData().getHashByDescription(serverDescription));
    }

    public boolean transfer(String hash) {
        return this.transfer(hash, true);
    }

    public boolean transfer(String hash, boolean loadScreen) {
        ClientData clients = this.getSynapseEntry().getClientData();
        Entry clientData = clients.clientList.get(hash);

        if (clientData != null) {
            SynapsePlayerTransferEvent event = new SynapsePlayerTransferEvent(this, clientData);
            this.server.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return false;
            }

            this.transferToHash(hash);
            return true;
        }

        return false;
    }

    int transferCommand(String serverDescription) {
        String hash = this.getSynapseEntry().getClientData().getHashByDescription(serverDescription);
        ClientData clients = this.getSynapseEntry().getClientData();
        Entry clientData = clients.clientList.get(hash);

        if (clientData != null) {
            SynapsePlayerTransferEvent event = new SynapsePlayerTransferEvent(this, clientData);
            this.server.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return 2;
            }

            this.transferToHash(hash);
            return 1;
        }

        return 0;
    }

    private void transferToHash(String hash) {
        for (Effect e : this.getEffects().values()) {
            MobEffectPacket removeEffect = new MobEffectPacket();
            removeEffect.eid = this.getId();
            removeEffect.effectId = e.getId();
            removeEffect.eventId = MobEffectPacket.EVENT_REMOVE;
            this.dataPacket(removeEffect);
        }
        if (this.inventory != null) {
            InventoryContentPacket removeInventory = new InventoryContentPacket();
            removeInventory.inventoryId = this.getWindowId(this.inventory);
            removeInventory.slots = new Item[this.inventory.getSize()];
            this.dataPacket(removeInventory);
        }
        PlayerListPacket removePlayers = new PlayerListPacket();
        removePlayers.type = PlayerListPacket.TYPE_REMOVE;
        removePlayers.entries = this.getServer().getOnlinePlayers().values().stream()
                .map(p -> new PlayerListPacket.Entry(p.getUniqueId()))
                .toArray(PlayerListPacket.Entry[]::new);
        this.dataPacket(removePlayers);
        /*if (this.level.getDimension() != Level.DIMENSION_OVERWORLD) {
            this.setDimension(Level.DIMENSION_OVERWORLD);
        }*/
        this.connectedToCurrentInstance = false;
        TransferPacket pk = new TransferPacket();
        pk.uuid = this.getUniqueId();
        pk.clientHash = hash;
        this.getSynapseEntry().sendDataPacket(pk);
    }

    public void setUniqueId(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean dataPacket(DataPacket packet) {
        return sendDataPacket(packet, false, false);
    }

    @Override
    public int dataPacket(DataPacket packet, boolean needACK) {
        return sendDataPacket(packet, needACK, false) ? 0 : -1;
    }

    @Override
    public boolean directDataPacket(DataPacket packet) {
        return sendDataPacket(packet, false, true);
    }

    @Override
    public boolean batchDataPacket(DataPacket packet) {
        return sendDataPacket(packet, false, false);
    }

    @Override
    public int directDataPacket(DataPacket packet, boolean needACK) {
        return sendDataPacket(packet, needACK, true) ? 0 : -1;
    }

    @Override
    public void forceDataPacket(DataPacket packet, Runnable callback) {
        sendDataPacket(packet, false, true);
    }

    public boolean sendDataPacket(DataPacket packet, boolean needACK, boolean direct) {
        if (!this.connected || !this.connectedToCurrentInstance) return false;
        packet = DataPacketEidReplacer.replace(packet, this.getId(), REPLACE_ID);

        DataPacketSendEvent ev = new DataPacketSendEvent(this, packet);
        this.server.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            return false;
        }

        packet.tryEncode();

        this.interfaz.putPacket(this, packet, false, false);
        return true;
    }

    // HACK: Transfer players to lobby when the server is full
    @Override
    public boolean kick(PlayerKickEvent.Reason reason, String reasonString, boolean isAdmin) {
        if (PlayerKickEvent.Reason.SERVER_FULL == reason) {
            SynapseFullServerPlayerTransferEvent event = new SynapseFullServerPlayerTransferEvent(this);
            this.server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            List<String> l = SynapseAPI.getInstance().getConfig().getStringList("lobbies");
            int size = l.size();
            if (size == 0) {
                return super.kick(reason, reasonString, isAdmin);
            }
            this.sendMessage("§cServer is full");
            if (!this.transferByDescription(l.get(size == 1 ? 0 : ThreadLocalRandom.current().nextInt(size)))) {
                return super.kick(reason, reasonString, isAdmin);
            }
            return false;
        }

        return super.kick(reason, reasonString, isAdmin);
    }
}
