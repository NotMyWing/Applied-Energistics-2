package appeng.hooks;

import appeng.api.parts.IPartHost;
import appeng.api.parts.PartItemStack;
import appeng.api.parts.SelectedPart;
import appeng.api.util.DimensionalCoord;
import appeng.util.LookDirection;
import appeng.util.Platform;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrench action, handled in event rather than item to support implementors of our wrench api
 */
public class WrenchClickHook {

    @SubscribeEvent
    public void playerInteract(final PlayerInteractEvent event) {
        // Only handle the main hand event
        if (event.getHand() != EnumHand.MAIN_HAND) return;

        if (event instanceof PlayerInteractEvent.RightClickBlock && !event.getEntityPlayer().world.isRemote) {
            EntityPlayer player = event.getEntityPlayer();
            EnumHand hand = event.getHand();
            BlockPos pos = event.getPos();
            World world = event.getWorld();
            ItemStack held = event.getItemStack();

            if (!Platform.hasPermissions(new DimensionalCoord(world, pos), player)) {
                return;
            }

            if (player.isSneaking() && Platform.isWrench(player, held, pos)) {
                Block block = world.getBlockState(pos).getBlock();
                TileEntity tile = world.getTileEntity(pos);
                if (!(tile instanceof IPartHost host)) {
                    return;
                }

                final LookDirection dir = Platform.getPlayerRay(player, player.getEyeHeight());
                final RayTraceResult mop = block.collisionRayTrace(world.getBlockState(pos), world, pos, dir.getA(), dir.getB());
                if (mop != null) {
                    final SelectedPart sp = host.selectPartGlobal(mop.hitVec);
                    if (sp == null) {
                        return;
                    }

                    final List<ItemStack> is = new ArrayList<>();

                    if (sp.part != null) {
                        is.add(sp.part.getItemStack(PartItemStack.WRENCH));
                        sp.part.getDrops(is, true);
                        host.removePart(sp.side, false);
                    }

                    if (sp.facade != null) {
                        is.add(sp.facade.getItemStack());
                        host.getFacadeContainer().removeFacade(host, sp.side);
                        Platform.notifyBlocksOfNeighbors(world, pos);
                    }

                    if (host.isEmpty()) {
                        host.cleanup();
                    }

                    if (!is.isEmpty()) {
                        Platform.spawnDrops(world, pos, is);
                    }
                } else {
                    player.swingArm(hand);
                }
            }
        }
    }
}
