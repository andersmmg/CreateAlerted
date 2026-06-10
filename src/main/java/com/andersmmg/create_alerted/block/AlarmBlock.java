package com.andersmmg.create_alerted.block;

import com.andersmmg.create_alerted.Config;
import com.andersmmg.create_alerted.integration.SableCompat;
import com.andersmmg.create_alerted.menu.AlarmMenu;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AlarmBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty CAGE = BooleanProperty.create("cage");

    private static final VoxelShape SHAPE_UP = Block.box(5, 0, 5, 11, 8, 11);
    private static final VoxelShape SHAPE_DOWN = Block.box(5, 8, 5, 11, 16, 11);
    private static final VoxelShape SHAPE_NORTH = Block.box(5, 5, 8, 11, 11, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(5, 5, 0, 11, 11, 8);
    private static final VoxelShape SHAPE_WEST = Block.box(8, 5, 5, 16, 11, 11);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 5, 5, 8, 11, 11);

    public AlarmBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(POWERED, false)
                .setValue(CAGE, false));
    }

    public abstract SoundEvent getAlarmSound();

    public abstract int getSoundInterval();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, CAGE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction dir : context.getNearestLookingDirections()) {
            BlockState state = defaultBlockState().setValue(FACING, dir.getOpposite());
            if (state.canSurvive(level, pos)) {
                boolean powered = level.hasNeighborSignal(pos);
                return state.setValue(POWERED, powered);
            }
        }
        return null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            if (!state.canSurvive(level, pos)) {
                level.destroyBlock(pos, true);
                return;
            }
            boolean powered = shouldBePowered(level, pos);
            if (powered != state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(POWERED, powered), 3);
                if (powered) {
                    scheduleSoundTick(level, pos);
                }
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof AlarmBlockEntity be) {
                be.registerNetwork();
            }
            if (state.getValue(POWERED)) {
                scheduleSoundTick(level, pos);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(POWERED)) {
            Vec3 soundPos = SableCompat.getGlobalPos(level, Vec3.atCenterOf(pos));
            level.playSound(null, soundPos.x, soundPos.y, soundPos.z, getAlarmSound(), SoundSource.BLOCKS, (float) Config.alarmVolume, 1.0f);
            scheduleSoundTick(level, pos);
        }
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_UP;
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof WrenchItem && !player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.cycle(CAGE), 3);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (openMenu(state, level, pos, player)) return ItemInteractionResult.SUCCESS;
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (openMenu(state, level, pos, player)) return InteractionResult.SUCCESS;
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlarmBlockEntity(pos, state);
    }

    private boolean openMenu(BlockState state, Level level, BlockPos pos, Player player) {
        if (!player.isShiftKeyDown()) return false;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AlarmBlockEntity be) {
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return state.getBlock().getName();
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    return new AlarmMenu(containerId, inventory, be);
                }
            }, pos);
        }
        return true;
    }

    private void scheduleSoundTick(Level level, BlockPos pos) {
        level.scheduleTick(pos, this, getSoundInterval());
    }

    private boolean shouldBePowered(Level level, BlockPos pos) {
        if (level.hasNeighborSignal(pos)) return true;
        return level.getBlockEntity(pos) instanceof AlarmBlockEntity be && be.getReceivedSignal() > 0;
    }
}
