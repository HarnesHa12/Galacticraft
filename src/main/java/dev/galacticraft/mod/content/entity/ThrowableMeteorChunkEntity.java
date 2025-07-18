/*
 * Copyright (c) 2019-2025 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.galacticraft.mod.content.entity;

import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.entity.damage.GCDamageTypes;
import dev.galacticraft.mod.content.item.GCItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrowableMeteorChunkEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> HOT = SynchedEntityData.defineId(ThrowableMeteorChunkEntity.class, EntityDataSerializers.BOOLEAN);

    public ThrowableMeteorChunkEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public ThrowableMeteorChunkEntity(LivingEntity shooter, Level level, boolean hot) {
        super(GCEntityTypes.THROWABLE_METEOR_CHUNK, shooter, level);
        this.entityData.set(HOT, hot);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder compositeStateBuilder) {
        super.defineSynchedData(compositeStateBuilder);
        compositeStateBuilder.define(HOT, false);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        if (compound.contains("Hot")) {
            this.entityData.set(HOT, compound.getBoolean("Hot"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Hot", this.entityData.get(HOT));
    }

    @Override
    protected Item getDefaultItem() {
        if (this.entityData == null || !this.entityData.get(HOT)) {
            return GCItems.THROWABLE_METEOR_CHUNK;
        } else {
            return GCItems.HOT_THROWABLE_METEOR_CHUNK;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isInWater() && this.entityData.get(HOT)) {
            Level level = this.level();
            this.setItem(new ItemStack(GCItems.THROWABLE_METEOR_CHUNK));
            this.entityData.set(HOT, false);
            this.playEntityOnFireExtinguishedSound();
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.WHITE_SMOKE, true, this.getX() + level.random.nextDouble() - 0.5, Mth.ceil(this.getY()), this.getZ() + level.random.nextDouble() - 0.5, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        Entity owner = this.getOwner();
        // TODO: Find out why the owner isn't getting credited for the damage
        DamageSource damage = new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(GCDamageTypes.METEOR_STRIKE), this, owner != null ? owner : this);
        if (this.entityData.get(HOT)) {
            entity.hurt(damage, 4.0F);
            entity.igniteForSeconds(3);
        } else {
            entity.hurt(damage, 2.0F);
        }
        if (owner instanceof LivingEntity livingEntity) {
            livingEntity.setLastHurtMob(entity);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide && !(this.getOwner() instanceof Player player && player.getAbilities().instabuild)) {
            this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), this.getItem()));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }
}
