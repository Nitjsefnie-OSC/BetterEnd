package org.betterx.betterend.item;

import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.interfaces.BetterEndElytra;
import org.betterx.betterend.interfaces.MultiModelItem;
import org.betterx.betterend.registry.EndItems;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.jetbrains.annotations.Nullable;

public class CrystaliteElytra extends CrystaliteArmor implements MultiModelItem, BetterEndElytra {

    private final ResourceLocation wingTexture;
    private final double movementFactor;
    private final int defense;
    private final float toughness;

    public CrystaliteElytra(int durability, double movementFactor) {
        super(Type.CHESTPLATE, EndItems.makeEndItemSettings().durability(durability).rarity(Rarity.EPIC));
        this.wingTexture = BetterEnd.makeID("textures/entity/elytra_crystalite.png");
        this.movementFactor = movementFactor;
        this.defense = (int) ((double) material.getDefenseForType(Type.CHESTPLATE) / 1.25);
        this.toughness = material.getToughness() / 1.25F;
        addAttributeModifier(
                Attributes.ARMOR,
                new AttributeModifier(
                        ARMOR_MODIFIER_UUID_PER_SLOT[2],
                        "Armor modifier",
                        defense,
                        AttributeModifier.Operation.ADDITION
                )
        );
        addAttributeModifier(
                Attributes.ARMOR_TOUGHNESS,
                new AttributeModifier(
                        ARMOR_MODIFIER_UUID_PER_SLOT[2],
                        "Armor toughness",
                        toughness,
                        AttributeModifier.Operation.ADDITION
                )
        );
    }

    @Override
    public boolean isValidRepairItem(ItemStack itemStack, ItemStack itemStack2) {
        return super.isValidRepairItem(itemStack, itemStack2) || itemStack2.getItem() == EndItems.ENCHANTED_MEMBRANE;
    }

    @Override
    public double getMovementFactor() {
        return movementFactor;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public ResourceLocation getModelTexture() {
        return wingTexture;
    }

    @Override
    public int getDefense() {
        return defense;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void registerModelPredicate() {
        ItemProperties.register(
                this,
                new ResourceLocation("broken"),
                // an anonymous class instead of a lambda: the synthetic lambda method would stay in this common class with a
                // client-only ClientLevel parameter and break reflection on dedicated servers (quiqueck/BCLib#240)
                new ClampedItemPropertyFunction() {
                    @Override
                    public float unclampedCall(
                            ItemStack itemStack,
                            @Nullable ClientLevel clientLevel,
                            @Nullable LivingEntity livingEntity,
                            int seed
                    ) {
                        return ElytraItem.isFlyEnabled(itemStack) ? 0.0F : 1.0F;
                    }
                }
        );
    }
}
