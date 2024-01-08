/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.parts;

import appeng.api.AEApi;
import appeng.api.parts.*;
import appeng.api.util.AEPartLocation;
import com.github.bsideup.jabel.Desugar;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;

import javax.annotation.Nullable;

public class PartPlacement {

    public static EnumActionResult place(final ItemStack held, final BlockPos pos, EnumFacing side, final EntityPlayer player, final EnumHand hand, final World world) {
        if (!(held.getItem() instanceof IPartItem<?>)) {
            return EnumActionResult.PASS;
        }

        // determine where the part would be placed
        Placement placement = getPartPlacement(player, world, held, pos, side);
        if (placement == null) {
            return EnumActionResult.FAIL;
        }

        // then try to place it
        IPart part = placePart(player, world, held, placement.pos(), placement.side(), hand);
        if (part == null) {
            return EnumActionResult.FAIL;
        }

        // handle placement logic with the stack
        if (!world.isRemote) {
            if (player != null && !player.isCreative()) {
                held.shrink(1);
                if (held.getCount() == 0) {
                    player.setHeldItem(hand, ItemStack.EMPTY);
                    MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, held, hand));
                }
            }
            return EnumActionResult.SUCCESS;
        } else {
            player.swingArm(hand);
            return EnumActionResult.PASS;
        }
    }

    public static IPart placePart(@Nullable EntityPlayer player, World world, ItemStack partItem, BlockPos pos, EnumFacing side, EnumHand hand) {
        IPartHost host = AEApi.instance().partHelper().getOrPlacePartHost(world, pos, false, player);
        if (host == null) {
            return null;
        }

        AEPartLocation location = host.addPart(partItem, AEPartLocation.fromFacing(side), player, hand);
        IPart part = host.getPart(location);
        if (part == null) {
            if (host.isEmpty()) {
                host.cleanup();
            }
            return null;
        }

        IBlockState multiPartState = AEApi.instance().definitions().blocks().multiPart().maybeBlock().get().getDefaultState();
        SoundType soundType = multiPartState.getBlock().getSoundType(multiPartState, world, pos, null);
        world.playSound(null, pos, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
        return part;
    }

    @Nullable
    public static Placement getPartPlacement(@Nullable EntityPlayer player, World world, ItemStack partStack, BlockPos pos, EnumFacing side) {
        if (canPlacePartOnBlock(player, world, partStack, pos, side)) {
            return new Placement(pos, side);
        }

        // If the part cannot be placed directly in the block, try the opposite side of
        // the adjacent block. This is somewhat similar to how torches are placed.
        pos = pos.offset(side);
        side = side.getOpposite();
        if (canPlacePartOnBlock(player, world, partStack, pos, side)) {
            return new Placement(pos, side);
        }

        // can't place the part
        return null;
    }

    public static boolean canPlacePartOnBlock(@Nullable EntityPlayer player, World world, ItemStack partStack, BlockPos pos, EnumFacing side) {
        IPartHost host = AEApi.instance().partHelper().getPartHost(world, pos);

        // There is no host at the location, we also cannot place one
        if (host == null && !AEApi.instance().partHelper().canPlacePartHost(world, pos, player)) {
            return false;
        }

        // Either there is no host, then we assume a freshly placed host will always accept our part,
        // or there is a host, and it has a free side.
        return host == null || host.canAddPart(partStack, AEPartLocation.fromFacing(side));
    }

    @Desugar
    public record Placement(BlockPos pos, EnumFacing side) {
    }
}
