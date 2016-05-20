package de.notaviable.npcs.data;

import com.comphenix.protocol.wrappers.EnumWrappers;

/**
 * Project: KillSwitch
 * Created by notaviable on 17.04.2016.
 */
public enum EquipSlot {
    MAIN_HAND,
    OFF_HAND, // 1.9 only
    BOOTS,
    LEGGINGS,
    CHESTPLATE,
    HELMET;

    public EnumWrappers.ItemSlot toWrapped() {
        switch (this) {
            case MAIN_HAND:
                return EnumWrappers.ItemSlot.MAINHAND;
            case OFF_HAND:
                return EnumWrappers.ItemSlot.OFFHAND;
            case BOOTS:
                return EnumWrappers.ItemSlot.FEET;
            case LEGGINGS:
                return EnumWrappers.ItemSlot.LEGS;
            case CHESTPLATE:
                return EnumWrappers.ItemSlot.CHEST;
            case HELMET:
                return EnumWrappers.ItemSlot.HEAD;
        }
        return null;
    }
}
