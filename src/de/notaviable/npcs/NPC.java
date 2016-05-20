package de.notaviable.npcs;

import com.avaje.ebeaninternal.server.lib.util.InvalidDataException;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import de.notaviable.npcs.data.Animations;
import de.notaviable.npcs.data.EquipSlot;
import de.notaviable.npcs.data.MCVersion;
import de.notaviable.npcs.exceptions.NotCompatibleException;
import de.notaviable.npcs.utils.EntityUtils;
import de.notaviable.npcs.utils.PacketUtils;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.notaviable.npcs.data.MCVersion.V1_7_X;
import static de.notaviable.npcs.data.MCVersion.V1_9_X;

/**
 * Project: KillSwitch
 * Created by notaviable on 14.04.2016.
 */
@SuppressWarnings("deprecation")
public class NPC {
    private final Player player;
    WrappedDataWatcher dataWatcher;
    private boolean spawned;
    private Location location;
    private float headYaw;
    private boolean hidden;
    private int entityId;
    private boolean autoAnimations = true;
    private boolean autoRespawn = true;
    public boolean randomIDeachSpawn = true;
    public String username;
    public UUID uuid;
    public boolean sprinting;
    public boolean sneaking;
    public boolean using;
    public boolean burning;
    public boolean flying;
    public boolean glowing;
    public boolean silent;
    private float health = 20f;
    public boolean onGround;
    public boolean showInTab = false;
    private boolean inTab = false;
    private int moveCounter = 0;


    public NPC(Player player) throws NotCompatibleException {
        this.player = player;
        entityId = EntityUtils.findFreeEntityId(player.getWorld());
        if (NPCLib.getMCVersion() == MCVersion.Unknown)
            throw new NotCompatibleException();
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) throws InvocationTargetException, NotCompatibleException {
        boolean changed = this.health != health;
        boolean isLower = health < this.health;
        boolean dies = health <= 0 && this.health > 0;
        this.health = health;
        if (changed) {
            if (is19())
                dataWatcher.getWatchableObject(6).setValue(health);
            else dataWatcher.setObject(6, health);
            if (isLower && autoAnimations)
                sendAnimation(Animations.TAKE_DAMAGE);
            sendMetaUpdate();
            if (dies) {
                unequip();
                despawn();
                if (autoRespawn) {
                    spawn();
                }
            }
        }
    }

    public void unequip() throws InvocationTargetException {
        for (EquipSlot slot : EquipSlot.values()) {
            if (slot == EquipSlot.OFF_HAND && !is19())
                continue;
            try {
                setEquipmentInSlot(slot, null);
            } catch (NotCompatibleException e) {
                // Ignore
            }
        }
    }

    public void setEquipmentInSlot(EquipSlot slot, ItemStack stack) throws NotCompatibleException, InvocationTargetException {
        if (slot == EquipSlot.OFF_HAND && !is19()) throw new NotCompatibleException();
        if (slot == EquipSlot.MAIN_HAND && is17()) // Bug (Client Crash)
            return;
        PacketContainer equip = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
        equip.getIntegers().write(0, entityId);
        if (is19())
            equip.getItemSlots().write(0, slot.toWrapped());
        else
            equip.getIntegers().write(1, slot.ordinal() - (is19() ? 0 : 1));
        equip.getItemModifier().write(0, stack);
        NPCLib.getProtocolManager().sendServerPacket(player, equip);
    }

    public void swingArm() throws InvocationTargetException {
        sendAnimation(Animations.SWING_ARM);
    }

    private void sendAnimation(Animations animations) throws InvocationTargetException {
        PacketContainer animation = new PacketContainer(PacketType.Play.Server.ANIMATION);
        animation.getIntegers().write(0, entityId);
        animation.getIntegers().write(1, animations.ordinal());
        NPCLib.getProtocolManager().sendServerPacket(player, animation);
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) throws NotCompatibleException, InvocationTargetException {
        if (this.location == null) {
            this.location = location;
            sendPosTP();
            return;
        }
        float dist = (float) this.location.distance(location);
        Location oldLoc = this.location;
        boolean anglesChanged = (this.location.getYaw() != location.getYaw() || this.location.getPitch() != location.getPitch());
        this.location = location;
        if (dist > getMaxRelMoveRange())
            sendPosTP();
        else if (dist == 0 && anglesChanged)
            sendAngleUpdate();
        else if (dist != 0 && !anglesChanged)
            sendMoveUpdate(oldLoc);
        else if (dist != 0 && anglesChanged)
            sendMoveAngleUpdate(oldLoc);
        if (anglesChanged)
            setHeadYaw(location.getYaw());
        moveCounter++;
        if (moveCounter > 100) {
            sendPosTP();
            moveCounter = 0;
        }
    }

    private void sendMoveAngleUpdate(Location prev) throws NotCompatibleException, InvocationTargetException {
        PacketContainer tp = new PacketContainer(PacketType.Play.Server.ENTITY_MOVE_LOOK);
        tp.getIntegers().write(0, entityId);
        switch (NPCLib.getMCVersion()) {
            case V1_7_X:
                tp.getBytes().write(0, (byte) ((location.getX() * 32 - prev.getX() * 32) * 1));
                tp.getBytes().write(1, (byte) ((location.getY() * 32 - prev.getY() * 32) * 1));
                tp.getBytes().write(2, (byte) ((location.getZ() * 32 - prev.getZ() * 32) * 1));
                break;
            case V1_8_X:
                tp.getBytes().write(0, (byte) ((location.getX() * 32 - prev.getX() * 32) * 1));
                tp.getBytes().write(1, (byte) ((location.getY() * 32 - prev.getY() * 32) * 1));
                tp.getBytes().write(2, (byte) ((location.getZ() * 32 - prev.getZ() * 32) * 1));
                break;
            case V1_9_X:
                tp.getIntegers().write(1, (int) ((location.getX() * 32 - prev.getX() * 32) * 128));
                tp.getIntegers().write(2, (int) ((location.getY() * 32 - prev.getY() * 32) * 128));
                tp.getIntegers().write(3, (int) ((location.getZ() * 32 - prev.getZ() * 32) * 128));
                break;
            default:
                throw new NotCompatibleException();
        }
        tp.getBooleans().write(0, onGround);
        tp.getBytes().write(NPCLib.getMCVersion() == V1_9_X ? 0 : 3, (byte) PacketUtils.getCompressedAngle(location.getYaw()));
        tp.getBytes().write(NPCLib.getMCVersion() == V1_9_X ? 1 : 4, (byte) PacketUtils.getCompressedAngle(location.getPitch()));
        NPCLib.getProtocolManager().sendServerPacket(player, tp);
    }

    private void sendMoveUpdate(Location prev) throws NotCompatibleException, InvocationTargetException {
        if (!spawned) return;
        PacketContainer tp = new PacketContainer(PacketType.Play.Server.REL_ENTITY_MOVE);
        tp.getIntegers().write(0, entityId);
        switch (NPCLib.getMCVersion()) {
            case V1_7_X:
                tp.getBytes().write(0, (byte) ((location.getX() * 32 - prev.getX() * 32) * 1));
                tp.getBytes().write(1, (byte) ((location.getY() * 32 - prev.getY() * 32) * 1));
                tp.getBytes().write(2, (byte) ((location.getZ() * 32 - prev.getZ() * 32) * 1));
                break;
            case V1_8_X:
                tp.getBytes().write(0, (byte) ((location.getX() * 32 - prev.getX() * 32) * 1));
                tp.getBytes().write(1, (byte) ((location.getY() * 32 - prev.getY() * 32) * 1));
                tp.getBytes().write(2, (byte) ((location.getZ() * 32 - prev.getZ() * 32) * 1));
                break;
            case V1_9_X:
                tp.getIntegers().write(1, (int) ((location.getX() * 32 - prev.getX() * 32) * 128));
                tp.getIntegers().write(2, (int) ((location.getY() * 32 - prev.getY() * 32) * 128));
                tp.getIntegers().write(3, (int) ((location.getZ() * 32 - prev.getZ() * 32) * 128));
                break;
            default:
                throw new NotCompatibleException();
        }
        tp.getBooleans().write(0, onGround);
        NPCLib.getProtocolManager().sendServerPacket(player, tp);
    }

    private void sendAngleUpdate() throws InvocationTargetException {
        if (!spawned) return;
        PacketContainer tp = new PacketContainer(PacketType.Play.Server.ENTITY_LOOK);
        tp.getIntegers().write(0, entityId);
        tp.getBytes().write(0, (byte) PacketUtils.getCompressedAngle(location.getYaw()));
        tp.getBytes().write(1, (byte) PacketUtils.getCompressedAngle(location.getPitch()));
        tp.getBooleans().write(0, onGround);
        NPCLib.getProtocolManager().sendServerPacket(player, tp);
    }

    private void sendPosTP() throws NotCompatibleException, InvocationTargetException {
        if (!spawned) return;
        PacketContainer tp = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        tp.getIntegers().write(0, entityId);
        switch (NPCLib.getMCVersion()) {
            case V1_7_X:
                tp.getIntegers().write(1, (int) (location.getX() * 32));
                tp.getIntegers().write(2, (int) (location.getY() * 32));
                tp.getIntegers().write(3, (int) (location.getZ() * 32));
                break;
            case V1_8_X:
                tp.getIntegers().write(1, (int) (location.getX() * 32));
                tp.getIntegers().write(2, (int) (location.getY() * 32));
                tp.getIntegers().write(3, (int) (location.getZ() * 32));
                break;
            case V1_9_X:
                tp.getDoubles().write(0, location.getX());
                tp.getDoubles().write(1, location.getY());
                tp.getDoubles().write(2, location.getZ());
                break;
            default:
                throw new NotCompatibleException();
        }
        tp.getBytes().write(0, (byte) PacketUtils.getCompressedAngle(location.getYaw()));
        tp.getBytes().write(1, (byte) PacketUtils.getCompressedAngle(location.getPitch()));
        tp.getBooleans().write(0, onGround);
        NPCLib.getProtocolManager().sendServerPacket(player, tp);
    }

    public float getHeadYaw() {
        return headYaw;
    }

    private void setHeadYaw(float headYaw) throws InvocationTargetException {
        boolean changed = this.headYaw != headYaw;
        this.headYaw = headYaw;
        if (changed)
            sendHeadYawUpdate();
    }

    private void sendHeadYawUpdate() throws InvocationTargetException {
        if (!spawned) return;
        PacketContainer headYaw = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        headYaw.getIntegers().write(0, entityId);
        headYaw.getBytes().write(0, (byte) PacketUtils.getCompressedAngle(this.headYaw));
        NPCLib.getProtocolManager().sendServerPacket(player, headYaw);
    }

    public boolean isVisible() {
        return !hidden;
    }

    public void setVisible(boolean visible) throws InvocationTargetException {
        boolean changed = visible == hidden;
        this.hidden = !visible;
        if (changed) {
            if (dataWatcher != null) {
                if (is19()) {
                    dataWatcher.getWatchableObject(0).setValue(generateBitMask());
                }
                else {
                    dataWatcher.setObject(0, generateBitMask());
                }
                sendMetaUpdate();
            }
        }
    }

    private void sendMetaUpdate() throws InvocationTargetException {
        if (!spawned) return;
        PacketContainer update = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        update.getIntegers().write(0, entityId);
        if (!is19())
            update.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
        else
            update.getDataWatcherModifier().write(0, dataWatcher);
        NPCLib.getProtocolManager().sendServerPacket(player, update);
    }

    public void hide() throws InvocationTargetException {
        setVisible(false);
    }

    public void show() throws InvocationTargetException {
        setVisible(true);
    }

    public void spawn() throws NotCompatibleException, InvocationTargetException {
        if (spawned) return;
        if (randomIDeachSpawn)
            entityId = EntityUtils.findFreeEntityId(player.getWorld());
        boolean tab = false;
        try {
            sendTabPacket();
            tab = true;
        } catch (InvalidDataException e) {
            tab = false;
        }
        spawnNPC();
        if (!showInTab && tab)
            sendTabRemovePacket();
        inTab = showInTab && tab;
        spawned = true;
    }

    private void sendTabPacket() throws InvocationTargetException, InvalidDataException {
        Player bukkitPlayer = Bukkit.getPlayer(uuid);
        if (bukkitPlayer != null)
            if (bukkitPlayer.isOnline())
                if (player.canSee(bukkitPlayer) || player == bukkitPlayer)
                    throw new InvalidDataException("");
        PacketContainer tab = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        if (NPCLib.getMCVersion() == V1_7_X) {
            tab.getIntegers().write(0, 0);
            tab.getGameProfiles().write(0, new WrappedGameProfile(uuid, username));
            tab.getIntegers().write(1, 0);
            tab.getIntegers().write(2, NPCLib.getRdm().nextInt(500));
            tab.getStrings().write(0, username);
        } else {
            tab.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            List<PlayerInfoData> playerData = new ArrayList<>();
            playerData.add(new PlayerInfoData(new WrappedGameProfile(uuid, username), NPCLib.getRdm().nextInt(900), EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(username)));
            tab.getPlayerInfoDataLists().write(0, playerData);
        }
        NPCLib.getProtocolManager().sendServerPacket(this.player, tab);
    }

    private void spawnNPC() throws NotCompatibleException, InvocationTargetException {
        if (spawned) return;
        switch (NPCLib.getMCVersion()) {
            case V1_7_X:
                spawnNPC17();
                break;
            case V1_8_X:
                spawnNPC18();
                break;
            case V1_9_X:
                spawnNPC19();
                break;
            default:
                throw new NotCompatibleException();
        }
        sendHeadYawUpdate();
    }

    private void spawnNPC19() throws InvocationTargetException {
        throw new NotImplementedException("1.9 Spawning is not done. I need help with the DataWatcher, feel free to help me!");
        /*
        PacketContainer spawn = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        spawn.getIntegers().write(0, entityId);
        spawn.getSpecificModifier(UUID.class).write(0, uuid);
        spawn.getDoubles().write(0, location.getX());
        spawn.getDoubles().write(1, location.getY());
        spawn.getDoubles().write(2, location.getZ());
        spawn.getBytes().write(0, (byte) PacketUtils.getCompressedAngle(location.getYaw()));
        spawn.getBytes().write(1, (byte) PacketUtils.getCompressedAngle(location.getPitch()));
        dataWatcher = PacketUtils.getDefaultWatcher(location.getWorld(), EntityType.PLAYER); // Cant Spawn Player...
        dataWatcher.getWatchableObject(0).setValue(generateBitMask());
        dataWatcher.getWatchableObject(1).setValue(20);
        dataWatcher.getWatchableObject(2).setValue(username);
        dataWatcher.getWatchableObject(3).setValue(true);
        dataWatcher.getWatchableObject(4).setValue(silent);
        dataWatcher.getWatchableObject(5).setValue(0x01);
        dataWatcher.getWatchableObject(6).setValue(health);
        dataWatcher.getWatchableObject(7).setValue(0);
        dataWatcher.getWatchableObject(8).setValue(0);
        dataWatcher.getWatchableObject(9).setValue(0);
        dataWatcher.getWatchableObject(10).setValue(0);
        dataWatcher.getWatchableObject(11).setValue(NPCLib.getRdm().nextInt(10000));
        dataWatcher.getWatchableObject(12).setValue(0x00); // Skin flags
        dataWatcher.getWatchableObject(13).setValue(0x01);
        spawn.getDataWatcherModifier().write(0, dataWatcher);
        NPCLib.getProtocolManager().sendServerPacket(player, spawn);
        */
    }

    private byte generateBitMask() {
        return (byte) ((burning ? 0x01 : 0x00) + (sneaking ? 0x02 : 0x00) + (sprinting ? 0x08 : 0x00) + (using ? 0x10 : 0x00) + (hidden ? 0x20 : 0x00) + (is19() ? ((glowing ? 0x40 : 0x00) + (flying ? 0x80 : 0x00)) : 0x00));
    }

    private void spawnNPC18() throws InvocationTargetException {
        PacketContainer spawn = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        spawn.getIntegers().write(0, entityId);
        spawn.getSpecificModifier(UUID.class).write(0, uuid);
        spawn.getIntegers().write(1, (int) (location.getX() * 32));
        spawn.getIntegers().write(2, (int) (location.getY() * 32));
        spawn.getIntegers().write(3, (int) (location.getZ() * 32));
        spawn.getBytes().write(0, (byte) PacketUtils.getCompressedAngle(location.getYaw()));
        spawn.getBytes().write(1, (byte) PacketUtils.getCompressedAngle(location.getPitch()));
        spawn.getIntegers().write(4, 0);
        dataWatcher = new WrappedDataWatcher();
        dataWatcher.setObject(0, generateBitMask());
        dataWatcher.setObject(1, (short) 20);
        dataWatcher.setObject(2, username);
        dataWatcher.setObject(3, (byte) 1);
        dataWatcher.setObject(4, (byte) (silent ? 0x01 : 0x00));
        dataWatcher.setObject(5, 0x01);
        dataWatcher.setObject(6, health);
        dataWatcher.setObject(7, 0);
        dataWatcher.setObject(8, (byte) 0);
        dataWatcher.setObject(9, (byte) 0);
        dataWatcher.setObject(10, (byte) 127); // Skin flags
        dataWatcher.setObject(15, (byte) 1);
        dataWatcher.setObject(16, 0x00); // Unused
        dataWatcher.setObject(17, 0f);
        dataWatcher.setObject(18, NPCLib.getRdm().nextInt(10000)); // Score
        spawn.getDataWatcherModifier().write(0, dataWatcher);
        NPCLib.getProtocolManager().sendServerPacket(player, spawn);
    }

    private void spawnNPC17() throws InvocationTargetException {
        PacketContainer spawn = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        spawn.getIntegers().write(0, entityId);
        spawn.getGameProfiles().write(0, new WrappedGameProfile(uuid, username));
        spawn.getIntegers().write(1, (int) (location.getX() * 32));
        spawn.getIntegers().write(2, (int) (location.getY() * 32));
        spawn.getIntegers().write(3, (int) (location.getZ() * 32));
        spawn.getBytes().write(0, (byte) PacketUtils.getCompressedAngle(location.getYaw()));
        spawn.getBytes().write(1, (byte) PacketUtils.getCompressedAngle(location.getPitch()));
        spawn.getIntegers().write(4, 0);
        dataWatcher = new WrappedDataWatcher();
        dataWatcher.setObject(0, generateBitMask());
        dataWatcher.setObject(1, 20);
        dataWatcher.setObject(2, username);
        dataWatcher.setObject(3, 0x01);
        dataWatcher.setObject(4, silent ? 0x01 : 0x00);
        dataWatcher.setObject(5, 0x01);
        dataWatcher.setObject(6, health);
        dataWatcher.setObject(7, 0);
        dataWatcher.setObject(8, (byte) 0);
        dataWatcher.setObject(9, (byte) 0);
        dataWatcher.setObject(10, (byte) 127); // Skin flags
        dataWatcher.setObject(15, (byte) 1);
        dataWatcher.setObject(16, 0x00); // Unused
        dataWatcher.setObject(17, 0f);
        dataWatcher.setObject(18, NPCLib.getRdm().nextInt(10000)); // Score
        spawn.getDataWatcherModifier().write(0, dataWatcher);
        NPCLib.getProtocolManager().sendServerPacket(player, spawn);
    }

    public void despawn() throws InvocationTargetException {
        despawnNPC();
        if (inTab)
            sendTabRemovePacket();
        inTab = false;
        spawned = false;
    }

    private void sendTabRemovePacket() throws InvocationTargetException {
        if (!inTab) return;
        PacketContainer tab = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        if (NPCLib.getMCVersion() == V1_7_X) {
            tab.getIntegers().write(0, 4);
            tab.getGameProfiles().write(0, new WrappedGameProfile(uuid, username));
            tab.getStrings().write(0, username);
        } else {
            tab.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
            List<PlayerInfoData> playerData = new ArrayList<>();
            playerData.add(new PlayerInfoData(new WrappedGameProfile(uuid, username), NPCLib.getRdm().nextInt(900), EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(username)));
            tab.getPlayerInfoDataLists().write(0, playerData);
        }
        NPCLib.getProtocolManager().sendServerPacket(this.player, tab);
    }

    private void despawnNPC() throws InvocationTargetException {
        if (!spawned) return;
        PacketContainer destroy = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        destroy.getIntegerArrays().write(0, new int[]{entityId});
        NPCLib.getProtocolManager().sendServerPacket(player, destroy);
    }

    private boolean is19() {
        return NPCLib.getMCVersion() == V1_9_X;
    }

    private float getMaxRelMoveRange() throws NotCompatibleException {
        switch (NPCLib.getMCVersion()) {

            case V1_7_X:
                return 4f;
            case V1_8_X:
                return 4f;
            case V1_9_X:
                return 8f;
            default:
                throw new NotCompatibleException();
        }
    }

    public int getID() {
        return entityId;
    }

    public boolean isSpawned() {
        return spawned;
    }

    public boolean isDoingAnimations() {
        return autoAnimations;
    }

    public void doAnimations(boolean animation) {
        this.autoAnimations = animation;
    }

    public boolean isAutoRespawning() {
        return autoRespawn;
    }

    public void shouldAutoRespawn(boolean respawn) {
        this.autoRespawn = respawn;
    }

    public boolean is17() {
        return NPCLib.getMCVersion() == V1_7_X;
    }

    public void setID(int eid) {
        if (spawned)
            throw new UnsupportedOperationException("Cannot change Entity ID while NPC is spawned!");
        entityId = eid;
    }
}
