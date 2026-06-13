package com.andersmmg.create_alerted.item;

import com.andersmmg.create_alerted.block.AlarmBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AlarmBlockItem extends BlockItem {
    public AlarmBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.setBlock(pos, state, 1 | 8)) {
            return false;
        }
        ItemStack stack = context.getItemInHand();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AlarmBlockEntity alarmBE) {
            int color = 0;
            var dyedItemColor = stack.get(DataComponents.DYED_COLOR);
            if (dyedItemColor != null) {
                color = dyedItemColor.rgb() & 0xFFFFFF;
            } else {
                var blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
                if (blockEntityData != null && !blockEntityData.isEmpty()) {
                    DyeColor dc = DyeColor.CODEC.byName(blockEntityData.copyTag().getString("DyeColor"));
                    if (dc != null) {
                        color = dc.getTextureDiffuseColor() & 0xFFFFFF;
                    }
                }
            }
            if (color != 0) {
                alarmBE.setColor(color);
            }
        }
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        return true;
    }
}
