package appeng.parts;

import appeng.api.AEApi;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.util.Platform;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Objects;

public class PartHelper {

    private PartHelper() {/**/}

    @Nullable
    public static IPart getPart(World world, BlockPos pos, @Nullable EnumFacing side) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof IPartHost partHost) {
            return partHost.getPart(AEPartLocation.fromFacing(side));
        }
        return null;
    }

    @Nullable
    public static IPartHost getOrPlacePartHost(World world, BlockPos pos, boolean force, @Nullable EntityPlayer player) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof IPartHost partHost) {
            return partHost;
        } else {
            if (!force && !canPlacePartHost(player, world, pos)) {
                return null;
            }

            IBlockState state = AEApi.instance().definitions().blocks().multiPart().maybeBlock().get().getDefaultState();
            world.setBlockState(pos, state, 3);
            return world.getTileEntity(pos) instanceof IPartHost host ? host : null;
        }
    }

    @Nullable
    public static IPartHost getPartHost(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof IPartHost partHost) {
            return partHost;
        }
        return null;
    }

    public static boolean canPlacePartHost(@Nullable EntityPlayer player, World world, BlockPos pos) {
        if (player != null && !Platform.hasPermissions(world, pos, player)) {
            return false;
        }

        Block blk = world.getBlockState(pos).getBlock();
        return blk == null || blk.isReplaceable(world, pos);
    }
}
