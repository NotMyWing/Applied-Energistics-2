package appeng.hooks;

import appeng.api.AEApi;
import appeng.api.definitions.IItems;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartItemStack;
import appeng.api.parts.SelectedPart;
import appeng.api.util.DimensionalCoord;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketClick;
import appeng.parts.PartPlacement;
import appeng.util.LookDirection;
import appeng.util.Platform;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

// todo
public class PlayerInteractHook {

    @SubscribeEvent
    public void playerInteract(final PlayerInteractEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        EnumHand hand = event.getHand();
        BlockPos pos = event.getPos();
        World world = event.getWorld();
        ItemStack held = event.getItemStack();

        if (event instanceof PlayerInteractEvent.RightClickBlock && !event.getEntityPlayer().world.isRemote) {
            if (!held.isEmpty() && Platform.isWrench(player, held, pos) && player.isSneaking()) {
                if (!Platform.hasPermissions(new DimensionalCoord(world, pos), player)) {
                    return;
                }

                final Block block = world.getBlockState(pos).getBlock();
                final TileEntity tile = world.getTileEntity(pos);
                IPartHost host = null;

                if (tile instanceof IPartHost) {
                    host = (IPartHost) tile;
                }

                if (host != null) {
                    if (!world.isRemote) {
                        final LookDirection dir = Platform.getPlayerRay(player, player.getEyeHeight());
                        final RayTraceResult mop = block.collisionRayTrace(world.getBlockState(pos), world, pos, dir.getA(), dir.getB());

                        if (mop != null) {
                            final List<ItemStack> is = new ArrayList<>();
                            final SelectedPart sp = host.selectPartGlobal(mop.hitVec);

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
                        }
                    } else {
                        player.swingArm(hand);
                    }
                }
            }
        }
    }

/*
    @SubscribeEvent
    public void playerInteract(final PlayerInteractEvent event) {
        // Only handle the main hand event
        if (event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }

        if (event instanceof PlayerInteractEvent.RightClickEmpty && event.getEntityPlayer().world.isRemote) {
            // re-check to see if this event was already channeled, cause these two events are really stupid...
            final RayTraceResult mop = Platform.rayTrace(event.getEntityPlayer(), true, false);
            final Minecraft mc = Minecraft.getMinecraft();

            final float f = 1.0F;
            final double d0 = mc.playerController.getBlockReachDistance();
            final Vec3d vec3 = mc.getRenderViewEntity().getPositionEyes(f);

            if (mop == null || mop.hitVec.distanceTo(vec3) >= d0) {
                final ItemStack held = event.getEntityPlayer().getHeldItem(event.getHand());
                final IItems items = AEApi.instance().definitions().items();

                boolean supportedItem = items.memoryCard().isSameAs(held);
                supportedItem |= items.colorApplicator().isSameAs(held);

                if (event.getEntityPlayer().isSneaking() && !held.isEmpty() && supportedItem) {
                    NetworkHandler.instance().sendToServer(new PacketClick(event.getPos(), event.getFace(), 0, 0, 0, event.getHand()));
                }
            }
        } else if (event instanceof PlayerInteractEvent.RightClickBlock && !event.getEntityPlayer().world.isRemote) {
            if (!held.isEmpty() && Platform.isWrench(player, held, pos) && player.isSneaking()) {
                if (!Platform.hasPermissions(new DimensionalCoord(world, pos), player)) {
                    return EnumActionResult.FAIL;
                }

                final Block block = world.getBlockState(pos).getBlock();
                final TileEntity tile = world.getTileEntity(pos);
                IPartHost host = null;

                if (tile instanceof IPartHost) {
                    host = (IPartHost) tile;
                }

                if (host != null) {
                    if (!world.isRemote) {
                        final LookDirection dir = Platform.getPlayerRay(player, getEyeOffset(player));
                        final RayTraceResult mop = block.collisionRayTrace(world.getBlockState(pos), world, pos, dir.getA(), dir.getB());

                        if (mop != null) {
                            final List<ItemStack> is = new ArrayList<>();
                            final SelectedPart sp = selectPart(player, host,
                                    mop.hitVec.add(-mop.getBlockPos().getX(), -mop.getBlockPos().getY(), -mop.getBlockPos().getZ()));

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
                        }
                    } else {
                        player.swingArm(hand);
                        NetworkHandler.instance().sendToServer(new PacketPartPlacement(pos, side, getEyeOffset(player), hand));
                    }
                    return EnumActionResult.SUCCESS;
                }

                return EnumActionResult.PASS;
            }

            if (held.isEmpty()) {
                final Block block = world.getBlockState(pos).getBlock();
                if (host != null && player.isSneaking() && block != null) {
                    final LookDirection dir = Platform.getPlayerRay(player, getEyeOffset(player));
                    final RayTraceResult mop = block.collisionRayTrace(world.getBlockState(pos), world, pos, dir.getA(), dir.getB());

                    if (mop != null) {
                        mop.hitVec = mop.hitVec.add(-mop.getBlockPos().getX(), -mop.getBlockPos().getY(), -mop.getBlockPos().getZ());
                        final SelectedPart sPart = selectPart(player, host, mop.hitVec);
                        if (sPart != null && sPart.part != null) {
                            if (sPart.part.onShiftActivate(player, hand, mop.hitVec)) {
                                if (world.isRemote) {
                                    NetworkHandler.instance().sendToServer(new PacketPartPlacement(pos, side, getEyeOffset(player), hand));
                                }
                                return EnumActionResult.SUCCESS;
                            }
                        }
                    }
                }
            }
        }
    }*/
}
