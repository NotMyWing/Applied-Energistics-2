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

package appeng.core.api;


import appeng.api.AEApi;
import appeng.api.definitions.ITileDefinition;
import appeng.api.parts.CableRenderMode;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHelper;
import appeng.core.AppEng;
import appeng.parts.PartPlacement;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.util.Platform;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;


public class ApiPart implements IPartHelper {

    @Override
    public EnumActionResult placeBus(final ItemStack is, final BlockPos pos, final EnumFacing side, final EntityPlayer player, final EnumHand hand, final World w) {
        return PartPlacement.place(is, pos, side, player, hand, w);
    }

    @Override
    public CableRenderMode getCableRenderMode() {
        return AppEng.proxy.getRenderMode();
    }

    @Nullable
    @Override
    public IPart getPart(World w, BlockPos pos, AEPartLocation side) {
        final TileEntity tile = w.getTileEntity(pos);
        if (tile instanceof IPartHost partHost) {
            return partHost.getPart(side);
        }
        return null;
    }

    @Nullable
    @Override
    public IPartHost getPartHost(World w, BlockPos pos) {
        final TileEntity tile = w.getTileEntity(pos);
        if (tile instanceof IPartHost partHost) {
            return partHost;
        }
        return null;
    }

    @Nullable
    @Override
    public IPartHost getOrPlacePartHost(World w, BlockPos pos, boolean force, @Nullable EntityPlayer p) {
        final TileEntity tile = w.getTileEntity(pos);
        if (tile instanceof IPartHost partHost) {
            return partHost;
        } else {
            if (!force && !canPlacePartHost(w, pos, p)) {
                return null;
            }

            final ITileDefinition multiPart = AEApi.instance().definitions().blocks().multiPart();
            if (!multiPart.isEnabled()) return null;
            Block blk = multiPart.maybeBlock().orElse(null);
            if (blk == null) return null;

            final IBlockState state = blk.getDefaultState();
            w.setBlockState(pos, state, 3);
            return w.getTileEntity(pos) instanceof IPartHost host ? host : null;
        }
    }

    @Override
    public boolean canPlacePartHost(World w, BlockPos pos, @Nullable EntityPlayer p) {
        if (p != null && !Platform.hasPermissions(w, pos, p)) {
            return false;
        }

        final Block blk = w.getBlockState(pos).getBlock();
        return blk == null || blk.isReplaceable(w, pos);
    }
}
