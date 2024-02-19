package org.itxtech.synapseapi.utils;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.EntityData;
import cn.nukkit.entity.data.EntityMetadata;
import cn.nukkit.network.protocol.*;
import cn.nukkit.utils.MainLogger;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Arrays;

/**
 * DataPacketEidReplacer
 * ===============
 * author: boybook
 * EaseCation Network Project
 * codefuncore
 * ===============
 */
public class DataPacketEidReplacer {

    private static final IntSet REPLACE_METADATA = new IntOpenHashSet(Arrays.asList(Entity.DATA_OWNER_EID, Entity.DATA_LEAD_HOLDER_EID, Entity.DATA_TRADING_PLAYER_EID, Entity.DATA_TARGET_EID));

    public static DataPacket replace(DataPacket pk, long from, long to) {
        DataPacket packet = pk.clone();
        boolean change = true;

        switch (packet.pid()) {
            case ProtocolInfo.ADD_PLAYER_PACKET:
                AddPlayerPacket app = (AddPlayerPacket) packet;
                app.metadata = replaceMetadata(app.metadata, from, to);
                break;
            case ProtocolInfo.ADD_ENTITY_PACKET:
                AddEntityPacket aep = (AddEntityPacket) packet;
                aep.metadata = replaceMetadata(aep.metadata, from, to);
                break;
            case ProtocolInfo.ADD_ITEM_ENTITY_PACKET:
                AddItemEntityPacket aiep = (AddItemEntityPacket) packet;
                aiep.metadata = replaceMetadata(aiep.metadata, from, to);
                break;
            case ProtocolInfo.ANIMATE_PACKET:
                if (((AnimatePacket) packet).eid == from) ((AnimatePacket) packet).eid = to;
                break;
            case ProtocolInfo.TAKE_ITEM_ENTITY_PACKET:
                if (((TakeItemEntityPacket) packet).entityId == from) ((TakeItemEntityPacket) packet).entityId = to;
                break;
            case ProtocolInfo.SET_ENTITY_MOTION_PACKET:
                if (((SetEntityMotionPacket) packet).eid == from) ((SetEntityMotionPacket) packet).eid = to;
                break;
            case ProtocolInfo.SET_ENTITY_LINK_PACKET:
                SetEntityLinkPacket selp = (SetEntityLinkPacket) packet;
                if (selp.vehicleUniqueId == from) selp.vehicleUniqueId = to;
                if (selp.riderUniqueId == from) selp.riderUniqueId = to;
                break;
            case ProtocolInfo.SET_ENTITY_DATA_PACKET:
                SetEntityDataPacket sedp = (SetEntityDataPacket) packet;
                if (sedp.eid == from) sedp.eid = to;
                sedp.metadata = replaceMetadata(sedp.metadata, from, to);
                break;
            case ProtocolInfo.UPDATE_ATTRIBUTES_PACKET:
                if (((UpdateAttributesPacket) packet).entityId == from) ((UpdateAttributesPacket) packet).entityId = to;
                break;
            case ProtocolInfo.ENTITY_EVENT_PACKET:
                if (((EntityEventPacket) packet).eid == from) ((EntityEventPacket) packet).eid = to;
                break;
            case ProtocolInfo.MOVE_ENTITY_DELTA_PACKET:
                if (((MoveEntityDeltaPacket) packet).runtimeEntityId == from) ((MoveEntityDeltaPacket) packet).runtimeEntityId = to;
                break;
            case ProtocolInfo.MOVE_PLAYER_PACKET:
                if (((MovePlayerPacket) packet).eid == from) ((MovePlayerPacket) packet).eid = to;
                break;
            case ProtocolInfo.MOB_EQUIPMENT_PACKET:
                if (((MobEquipmentPacket) packet).eid == from) ((MobEquipmentPacket) packet).eid = to;
                break;
            case ProtocolInfo.MOB_EFFECT_PACKET:
                if (((MobEffectPacket) packet).eid == from) ((MobEffectPacket) packet).eid = to;
                break;
            case ProtocolInfo.MOVE_ENTITY_ABSOLUTE_PACKET:
                if (((MoveEntityAbsolutePacket) packet).eid == from) ((MoveEntityAbsolutePacket) packet).eid = to;
                break;
            case ProtocolInfo.MOB_ARMOR_EQUIPMENT_PACKET:
                if (((MobArmorEquipmentPacket) packet).eid == from) ((MobArmorEquipmentPacket) packet).eid = to;
                break;
            case ProtocolInfo.PLAYER_LIST_PACKET:
                Arrays.stream(((PlayerListPacket) packet).entries).filter(entry -> entry.entityId == from).forEach(entry -> entry.entityId = to);
                break;
            case ProtocolInfo.BOSS_EVENT_PACKET:
                if (((BossEventPacket) packet).bossEid == from) ((BossEventPacket) packet).bossEid = to;
                break;
            case ProtocolInfo.ADVENTURE_SETTINGS_PACKET:
                if (((AdventureSettingsPacket) packet).entityUniqueId == from) ((AdventureSettingsPacket) packet).entityUniqueId = to;
                break;
            case ProtocolInfo.UPDATE_EQUIPMENT_PACKET:
                if (((UpdateEquipmentPacket) packet).eid == from) ((UpdateEquipmentPacket) packet).eid = to;
                break;
            case ProtocolInfo.CONTAINER_OPEN_PACKET:
                if (((ContainerOpenPacket) packet).entityId == from) ((ContainerOpenPacket) packet).entityId = to;
                break;
            case ProtocolInfo.SHOW_CREDITS_PACKET:
                if (((ShowCreditsPacket) packet).eid == from) ((ShowCreditsPacket) packet).eid = to;
                break;
            case ProtocolInfo.EMOTE_PACKET:
                if (((EmotePacket) packet).runtimeId == from) ((EmotePacket) packet).runtimeId = to;
                break;
            case ProtocolInfo.EMOTE_LIST_PACKET:
                if (((EmoteListPacket) packet).runtimeId == from) ((EmoteListPacket) packet).runtimeId = to;
                break;
            case ProtocolInfo.UPDATE_TRADE_PACKET:
                if (((UpdateTradePacket) packet).player == from) ((UpdateTradePacket) packet).player = to;
                break;
            case ProtocolInfo.CAMERA_PACKET:
                if (((CameraPacket) packet).playerUniqueId == from) ((CameraPacket) packet).playerUniqueId = to;
                break;
            case ProtocolInfo.UPDATE_PLAYER_GAME_TYPE_PACKET:
                if (((UpdatePlayerGameTypePacket) packet).entityId == from) ((UpdatePlayerGameTypePacket) packet).entityId = to;
                break;
            case ProtocolInfo.SPAWN_PARTICLE_EFFECT_PACKET:
                if (((SpawnParticleEffectPacket) packet).uniqueEntityId == from) ((SpawnParticleEffectPacket) packet).uniqueEntityId = to;
                break;
            case ProtocolInfo.UPDATE_ABILITIES_PACKET:
                if (((UpdateAbilitiesPacket) packet).getEntityId() == from) ((UpdateAbilitiesPacket) packet).setEntityId(to);
                break;
            default:
                change = false;
        }

        if (change) {
            packet.isEncoded = false;
        }

        return packet;
    }

    public static DataPacket replaceBack(DataPacket packet, long from, long to) {
        boolean change = true;

        switch (packet.pid()) {
            case ProtocolInfo.MOVE_PLAYER_PACKET:
                MovePlayerPacket movePlayerPacket = (MovePlayerPacket) packet;
                if (movePlayerPacket.eid == from) movePlayerPacket.eid = to;
                break;
            case ProtocolInfo.ADVENTURE_SETTINGS_PACKET:
                AdventureSettingsPacket adventureSettingsPacket = (AdventureSettingsPacket) packet;
                if (adventureSettingsPacket.entityUniqueId == from) adventureSettingsPacket.entityUniqueId = to;
                break;
            case ProtocolInfo.MOB_EQUIPMENT_PACKET:
                MobEquipmentPacket mobEquipmentPacket = (MobEquipmentPacket) packet;
                if (mobEquipmentPacket.eid == from) mobEquipmentPacket.eid = to;
                break;
            case ProtocolInfo.PLAYER_ACTION_PACKET:
                PlayerActionPacket playerActionPacket = (PlayerActionPacket) packet;
                if (playerActionPacket.entityId == from) playerActionPacket.entityId = to;
                break;
            case ProtocolInfo.INTERACT_PACKET:
                InteractPacket interactPacket = (InteractPacket) packet;
                if (interactPacket.target == from) interactPacket.target = to;
                break;
            case ProtocolInfo.ANIMATE_PACKET:
                AnimatePacket animatePacket = (AnimatePacket) packet;
                if (animatePacket.eid == from) animatePacket.eid = to;
                break;
            case ProtocolInfo.ENTITY_EVENT_PACKET:
                EntityEventPacket entityEventPacket = (EntityEventPacket) packet;
                if (entityEventPacket.eid == from) entityEventPacket.eid = to;
                break;
            case ProtocolInfo.SET_LOCAL_PLAYER_AS_INITIALIZED_PACKET:
                SetLocalPlayerAsInitializedPacket setLocalPlayerAsInitializedPacket = (SetLocalPlayerAsInitializedPacket) packet;
                if (setLocalPlayerAsInitializedPacket.eid == from) setLocalPlayerAsInitializedPacket.eid = to;
                break;
            case ProtocolInfo.RESPAWN_PACKET:
                RespawnPacket respawnPacket = (RespawnPacket) packet;
                if (respawnPacket.runtimeEntityId == from) respawnPacket.runtimeEntityId = to;
                break;
            case ProtocolInfo.EMOTE_PACKET:
                EmotePacket emotePacket = (EmotePacket) packet;
                if (emotePacket.runtimeId == from) emotePacket.runtimeId = to;
                break;
            case ProtocolInfo.EMOTE_LIST_PACKET:
                EmoteListPacket emoteListPacket = (EmoteListPacket) packet;
                if (emoteListPacket.runtimeId == from) emoteListPacket.runtimeId = to;
                break;
            case ProtocolInfo.UPDATE_ABILITIES_PACKET:
                UpdateAbilitiesPacket abilitiesPacket = (UpdateAbilitiesPacket) packet;
                if (abilitiesPacket.getEntityId() == from) abilitiesPacket.setEntityId(to);
                break;
            default:
                change = false;
        }

        if (change) {
            packet.isEncoded = false;
        }

        return packet;
    }

    private static EntityMetadata replaceMetadata(EntityMetadata data, long from, long to) {
        boolean changed = false;

        for (Integer key : REPLACE_METADATA) {
            try {
                if (data.getLong(key) == from) {
                    if (!changed) {
                        data = cloneMetadata(data);
                        changed = true;
                    }

                    data.putLong(key, to);
                }
            } catch (Exception e) {
                MainLogger.getLogger().error("Exception while replacing metadata '" + key + '\'', e);
            }
        }

        return data;
    }

    @SuppressWarnings("rawtypes")
    private static EntityMetadata cloneMetadata(EntityMetadata data) {
        EntityMetadata newData = new EntityMetadata();

        for (EntityData value : data.getMap().values()) {
            newData.put(value);
        }

        return newData;
    }
}