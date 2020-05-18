package com.hrznstudio.galacticraft.block.special.rocketlaunchpad;

import com.hrznstudio.galacticraft.block.GalacticraftBlocks;
import io.netty.buffer.Unpooled;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

import javax.annotation.Nullable;

public class RocketLaunchPadBlock extends BlockWithEntity {
    public static final EnumProperty<Part> PART = EnumProperty.of("part", Part.class);
    private static final Direction[] CARDINAL = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    public RocketLaunchPadBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(PART, Part.NONE));
    }

    public static BlockPos partToCenterPos(Part part) {
        if (part == null) return new BlockPos(0, 0, 0);
        switch (part) {
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                return new BlockPos(Direction.valueOf(part.asString().toUpperCase()).getOpposite().getOffsetX(), 0, Direction.valueOf(part.asString().toUpperCase()).getOpposite().getOffsetX());
            case NONE:
            case CENTER:
                return new BlockPos(0, 0, 0);
            default:
                return new BlockPos(Direction.valueOf(part.asString().split("_")[0].toUpperCase()).getOpposite().getOffsetX(), 0, Direction.valueOf(part.asString().split("_")[1].toUpperCase()).getOpposite().getOffsetX());
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(PART);
    }

    @Override
    public BlockRenderType getRenderType(BlockState blockState_1) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelShapes.cuboid(0.0D, 0.0D, 0.0D, 1.0D, state.get(PART) == Part.CENTER ? 0.25D : 0.1875D, 1.0D);
    }

    @Override
    public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
        super.onBroken(world, pos, state);
        switch (state.get(PART)) {
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST: {
                BlockPos center = pos.offset(Direction.valueOf(state.get(PART).asString().toUpperCase()).getOpposite());
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (world.getBlockState(center.add(x, 0, z)).getBlock() == GalacticraftBlocks.ROCKET_LAUNCH_PAD
                                && world.getBlockState(center.add(x, 0, z)).get(PART) != Part.NONE) {
                            world.setBlockState(center.add(x, 0, z), Blocks.AIR.getDefaultState(), 3);
                            createBlockBreakParticles(world, center.add(x, 0, z));
                        }
                    }
                }
                return;
            }
            case NORTH_EAST:
            case NORTH_WEST:
            case SOUTH_EAST:
            case SOUTH_WEST: {
                String[] parts = state.get(PART).asString().split("_");
                BlockPos center = pos.offset(Direction.valueOf(parts[0].toUpperCase()).getOpposite()).offset(Direction.valueOf(parts[1].toUpperCase()).getOpposite());
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (world.getBlockState(center.add(x, 0, z)).getBlock() == GalacticraftBlocks.ROCKET_LAUNCH_PAD
                                && world.getBlockState(center.add(x, 0, z)).get(PART) != Part.NONE) {
                            world.setBlockState(center.add(x, 0, z), Blocks.AIR.getDefaultState(), 3);
                            createBlockBreakParticles(world, center.add(x, 0, z));
                        }
                    }
                }
                return;
            }
            case CENTER: {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (world.getBlockState(pos.add(x, 0, z)).getBlock() == GalacticraftBlocks.ROCKET_LAUNCH_PAD
                                && world.getBlockState(pos.add(x, 0, z)).get(PART) != Part.NONE) {
                            world.setBlockState(pos.add(x, 0, z), Blocks.AIR.getDefaultState(), 3);
                            createBlockBreakParticles(world, pos.add(x, 0, z));
                        }
                    }
                }
                return;
            }
        }
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);
    }

    @Override
    public void onBlockRemoved(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onBlockRemoved(state, world, pos, newState, moved);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moved) {
        super.onBlockAdded(state, world, pos, oldState, moved);
        if (state.get(PART) == Part.NONE) {
            if (world.getBlockEntity(pos) == null) {
                int connections = 0;
                for (Direction direction : CARDINAL) {
                    if (world.getBlockState(pos.offset(direction)).getBlock() == GalacticraftBlocks.ROCKET_LAUNCH_PAD && world.getBlockEntity(pos.offset(direction)) == null) {
                        connections++;
                    }
                }

                if (connections == 4) {
                    boolean allValid = true;
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (world.getBlockState(pos.add(x, 0, z)).getBlock() != GalacticraftBlocks.ROCKET_LAUNCH_PAD || world.getBlockEntity(pos.add(x, 0, z)) != null) {
                                allValid = false;
                                break;
                            }
                        }
                    }
                    if (allValid) {
                        RocketLaunchPadBlockEntity blockEntity = new RocketLaunchPadBlockEntity();
                        blockEntity.setLocation(world, pos);
                        world.setBlockEntity(pos, blockEntity);
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                createBlockBreakParticles(world, pos);
                                world.setBlockState(pos.add(x, 0, z), world.getBlockState(pos.add(x, 0, z)).with(PART, getPartForOffset(x, z)));
                            }
                        }
                        return;
                    } else {
                        connections--;
                    }
                }

                if (connections == 3) {
                    for (Direction direction : CARDINAL) {
                        if (world.getBlockState(pos.offset(direction)).getBlock() == GalacticraftBlocks.ROCKET_LAUNCH_PAD && world.getBlockEntity(pos.offset(direction)) == null) {
                            if (world.getBlockState(pos.offset(direction.getOpposite())).getBlock() == GalacticraftBlocks.ROCKET_LAUNCH_PAD && world.getBlockEntity(pos.offset(direction.getOpposite())) == null) {
                                for (Direction dir : CARDINAL) {
                                    if (dir.getAxis() != direction.getAxis()) {
                                        boolean allValid = true;
                                        for (int x = -1; x <= 1; x++) {
                                            for (int z = -1; z <= 1; z++) {
                                                if (world.getBlockState(pos.offset(dir).add(x, 0, z)).getBlock() != GalacticraftBlocks.ROCKET_LAUNCH_PAD || world.getBlockEntity(pos.offset(dir).add(x, 0, z)) != null) {
                                                    allValid = false;
                                                    break;
                                                }
                                            }
                                        }

                                        if (allValid) {
                                            RocketLaunchPadBlockEntity blockEntity = new RocketLaunchPadBlockEntity();
                                            blockEntity.setLocation(world, pos.offset(dir));
                                            world.setBlockEntity(pos.offset(dir), blockEntity);
                                            for (int x = -1; x <= 1; x++) {
                                                for (int z = -1; z <= 1; z++) {
                                                    createBlockBreakParticles(world, pos.offset(dir));
                                                    world.setBlockState(pos.offset(dir).add(x, 0, z), world.getBlockState(pos.offset(dir).add(x, 0, z)).with(PART, getPartForOffset(x, z)));
                                                }
                                            }
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    connections--;
                }

                if (connections == 2) {
                    for (Direction direction : CARDINAL) {
                        if (world.getBlockState(pos.offset(direction)).getBlock() == GalacticraftBlocks.ROCKET_LAUNCH_PAD
                                && world.getBlockEntity(pos.offset(direction)) == null) {
                            Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH};
                            if (direction.getAxis() == Direction.Axis.Z)
                                dirs = new Direction[]{Direction.EAST, Direction.WEST};

                            for (Direction dir : dirs) {
                                if (world.getBlockState(pos.offset(dir)).getBlock() == GalacticraftBlocks.ROCKET_LAUNCH_PAD
                                        && world.getBlockEntity(pos.offset(dir)) == null) {
                                    if (world.getBlockState(pos.offset(dir).offset(direction)).getBlock() == GalacticraftBlocks.ROCKET_LAUNCH_PAD
                                            && world.getBlockEntity(pos.offset(dir).offset(direction)) == null) {
                                        boolean allValid = true;
                                        for (int x = -1; x <= 1; x++) {
                                            for (int z = -1; z <= 1; z++) {
                                                if (world.getBlockState(pos.offset(dir).offset(direction).add(x, 0, z)).getBlock() != GalacticraftBlocks.ROCKET_LAUNCH_PAD || world.getBlockEntity(pos.offset(dir).offset(direction).add(x, 0, z)) != null) {
                                                    allValid = false;
                                                    break;
                                                }
                                            }
                                        }

                                        if (allValid) {
                                            RocketLaunchPadBlockEntity blockEntity = new RocketLaunchPadBlockEntity();
                                            blockEntity.setLocation(world, pos.offset(dir).offset(direction));
                                            world.setBlockEntity(pos.offset(dir).offset(direction), blockEntity);
                                            for (int x = -1; x <= 1; x++) {
                                                for (int z = -1; z <= 1; z++) {
                                                    createBlockBreakParticles(world, pos.offset(dir).offset(direction));
                                                    world.setBlockState(pos.offset(dir).offset(direction).add(x, 0, z), world.getBlockState(pos.offset(dir).offset(direction).add(x, 0, z)).with(PART, getPartForOffset(x, z)));
                                                }
                                            }
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public Part getPartForOffset(int x, int z) {
        switch (x) {
            case 0:
                switch (z) {
                    case 0:
                        return Part.CENTER;
                    case -1:
                        return Part.NORTH;
                    case 1:
                        return Part.SOUTH;
                }
            case -1:
                switch (z) {
                    case 0:
                        return Part.WEST;
                    case -1:
                        return Part.NORTH_WEST;
                    case 1:
                        return Part.SOUTH_WEST;
                }
            case 1:
                switch (z) {
                    case 0:
                        return Part.EAST;
                    case -1:
                        return Part.NORTH_EAST;
                    case 1:
                        return Part.SOUTH_EAST;
                }
        }
        return Part.NONE;
    }

    public void createBlockBreakParticles(WorldAccess world, BlockPos pos) {
        if (!world.getBlockState(pos).isAir()) {
            VoxelShape voxelShape = world.getBlockState(pos).getOutlineShape(world, pos, ShapeContext.absent());
            double d = 0.25D;
            voxelShape.forEachBox((dx, e, f, g, h, i) -> {
                double j = Math.min(1.0D, g - dx);
                double k = Math.min(1.0D, h - e);
                double l = Math.min(1.0D, i - f);
                int m = Math.max(2, MathHelper.ceil(j / 0.25D));
                int n = Math.max(2, MathHelper.ceil(k / 0.25D));
                int o = Math.max(2, MathHelper.ceil(l / 0.25D));

                for (int p = 0; p < m; ++p) {
                    for (int q = 0; q < n; ++q) {
                        for (int r = 0; r < o; ++r) {
                            double s = ((double) p + 0.5D) / (double) m;
                            double t = ((double) q + 0.5D) / (double) n;
                            double u = ((double) r + 0.5D) / (double) o;
                            double v = s * j + dx;
                            double w = t * k + e;
                            double x = u * l + f;
                            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer()).writeVarInt(Block.STATE_IDS.getId(this.getDefaultState()));
                            world.addParticle(BlockStateParticleEffect.PARAMETERS_FACTORY.read(ParticleTypes.BLOCK, buf), (double) pos.getX() + v, (double) pos.getY() + w, (double) pos.getZ() + x, s - 0.5D, t - 0.5D, u - 0.5D);
                        }
                    }
                }

            });
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return !world.getBlockState(pos.offset(Direction.DOWN)).getMaterial().isReplaceable();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockView view) {
        return null;
    }

    public enum Part implements StringIdentifiable {
        NONE,
        NORTH_WEST,
        NORTH,
        NORTH_EAST,
        WEST,
        CENTER,
        EAST,
        SOUTH_WEST,
        SOUTH,
        SOUTH_EAST;

        @Override
        public String asString() {
            return this.name().toLowerCase();
        }
    }
}
