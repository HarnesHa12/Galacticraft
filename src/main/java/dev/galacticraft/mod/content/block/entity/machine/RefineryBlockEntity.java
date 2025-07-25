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

package dev.galacticraft.mod.content.block.entity.machine;

import com.mojang.datafixers.util.Pair;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.filter.ResourceFilters;
import dev.galacticraft.machinelib.api.machine.MachineStatus;
import dev.galacticraft.machinelib.api.machine.MachineStatuses;
import dev.galacticraft.machinelib.api.menu.MachineMenu;
import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import dev.galacticraft.machinelib.api.storage.MachineFluidStorage;
import dev.galacticraft.machinelib.api.storage.MachineItemStorage;
import dev.galacticraft.machinelib.api.storage.StorageSpec;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.TransferType;
import dev.galacticraft.machinelib.api.util.FluidSource;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.Galacticraft;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.screen.GCMenuTypes;
import dev.galacticraft.mod.util.FluidUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public class RefineryBlockEntity extends MachineBlockEntity {
    public static final int CHARGE_SLOT = 0;
    public static final int OIL_INPUT_SLOT = 1;
    public static final int FUEL_OUTPUT_SLOT = 2;
    public static final int OIL_TANK = 0;
    public static final int FUEL_TANK = 1;

    @VisibleForTesting
    public static final long MAX_CAPACITY = FluidUtil.bucketsToDroplets(8);

    private static final StorageSpec SPEC = StorageSpec.of(
            MachineItemStorage.spec(
                    ItemResourceSlot.builder(TransferType.TRANSFER)
                            .pos(8, 62)
                            .capacity(1)
                            .filter(ResourceFilters.CAN_EXTRACT_ENERGY)
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.ENERGY)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(125, 62)
                            .capacity(1)
                            .filter(ResourceFilters.canExtractFluid(GCFluids.CRUDE_OIL)) // fixme: tag?,
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET)),
                    ItemResourceSlot.builder(TransferType.PROCESSING)
                            .pos(152, 62)
                            .capacity(1)
                            .filter(ResourceFilters.canInsertFluid(GCFluids.FUEL)) // fixme: tag?
                            .icon(Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.SlotSprite.BUCKET))
            ),
            MachineEnergyStorage.spec(
                    Galacticraft.CONFIG.machineEnergyStorageSize(),
                    Galacticraft.CONFIG.refineryEnergyConsumptionRate() * 2,
                    0
            ),
            MachineFluidStorage.spec(
                    FluidResourceSlot.builder(TransferType.INPUT)
                            .pos(125, 8)
                            .capacity(RefineryBlockEntity.MAX_CAPACITY)
                            .filter(ResourceFilters.ofResource(GCFluids.CRUDE_OIL)),
                    FluidResourceSlot.builder(TransferType.OUTPUT)
                            .pos(152, 8)
                            .capacity(RefineryBlockEntity.MAX_CAPACITY)
                            .filter(ResourceFilters.ofResource(GCFluids.FUEL))
            )
    );

    private final FluidSource fluidSource = new FluidSource(this);

    public RefineryBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.REFINERY, pos, state, SPEC);
    }

    @Override
    protected void tickConstant(@NotNull ServerLevel world, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        super.tickConstant(world, pos, state, profiler);
        this.chargeFromSlot(CHARGE_SLOT);

        this.takeFluidFromSlot(OIL_INPUT_SLOT, OIL_TANK, GCFluids.CRUDE_OIL);
        this.drainFluidToSlot(FUEL_OUTPUT_SLOT, FUEL_TANK);
    }

    @Override
    protected @NotNull MachineStatus tick(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ProfilerFiller profiler) {
        profiler.push("transfer");
        this.fluidSource.trySpreadFluids(level, pos, state);

        FluidResourceSlot oilTank = this.fluidStorage().slot(OIL_TANK);
        if (oilTank.isEmpty()) return GCMachineStatuses.MISSING_OIL;
        FluidResourceSlot fuelTank = this.fluidStorage().slot(FUEL_TANK);
        if (fuelTank.isFull()) return GCMachineStatuses.FUEL_TANK_FULL;
        profiler.popPush("transaction");
        try {
            if (this.energyStorage().canExtract(Galacticraft.CONFIG.refineryEnergyConsumptionRate())) {
                long space = fuelTank.tryInsert(GCFluids.FUEL, FluidConstants.BUCKET / 20 / 5);
                if (space > 0) {
                    this.energyStorage().extract(Galacticraft.CONFIG.refineryEnergyConsumptionRate());
                    fuelTank.insert(GCFluids.FUEL, oilTank.extract(GCFluids.CRUDE_OIL, space));
                }
                return GCMachineStatuses.REFINING;
            } else {
                return MachineStatuses.NOT_ENOUGH_ENERGY;
            }
        } finally {
            profiler.pop();
        }
    }

    @Nullable
    @Override
    public MachineMenu<? extends MachineBlockEntity> createMenu(int syncId, Inventory inv, Player player) {
        return new MachineMenu<>(
                GCMenuTypes.REFINERY,
                syncId,
                player,
                this
        );
    }
}