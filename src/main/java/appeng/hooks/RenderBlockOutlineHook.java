package appeng.hooks;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.util.AEPartLocation;
import appeng.facade.FacadePart;
import appeng.facade.IFacadeItem;
import appeng.items.parts.ItemFacade;
import appeng.parts.BusCollisionHelper;
import appeng.parts.PartHelper;
import appeng.parts.PartPlacement;
import appeng.parts.PartPlacement.Placement;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class RenderBlockOutlineHook {

    @SubscribeEvent
    public static void onDrawHighlightEvent(DrawBlockHighlightEvent event) {
        // noinspection ConstantConditions
        if (event.getTarget().getBlockPos() == null) return;

        if (event.getTarget().typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }

        EntityPlayer player = event.getPlayer();
        ItemStack stack = player.getHeldItemMainhand();
        BlockPos hitPos = event.getTarget().getBlockPos();
        EnumFacing hitSide = event.getTarget().sideHit;
        if (replaceBlockOutline(player, player.world, stack, hitPos, hitSide, event.getPartialTicks())) {
            event.setCanceled(true);
        }
    }

    private static boolean replaceBlockOutline(EntityPlayer player, World world, ItemStack stack, BlockPos hitPos, EnumFacing side, float partialTicks) {
        if (world.getBlockState(hitPos).getBlock() == Blocks.AIR) {
            return false;
        }
        Placement placement = PartPlacement.getPartPlacement(player, world, stack, hitPos, side);
        if (placement == null) {
            return false;
        }
        if (!world.getWorldBorder().contains(placement.pos())) {
            return false;
        }

        if (stack.getItem() instanceof IPartItem<?> partItem) {
            return renderPart(partItem, stack, placement, player, partialTicks);
        } else if (stack.getItem() instanceof IFacadeItem facadeItem) {
            return renderFacade(facadeItem, stack, placement, player, world, partialTicks);
        }
        return false;
    }

    private static boolean renderPart(IPartItem<?> partItem, ItemStack stack, Placement placement, EntityPlayer player, float partialTicks) {
        IPart part = partItem.createPartFromItemStack(stack);
        if (part == null) {
            return false;
        }

        setup();
        List<AxisAlignedBB> boxes = new ArrayList<>();
        IPartCollisionHelper helper = new BusCollisionHelper(boxes, AEPartLocation.fromFacing(placement.side()), player, true);
        part.getBoxes(helper);
        for (AxisAlignedBB box : boxes) {
            box = offsetBox(box, placement.pos(), player, partialTicks);
            RenderGlobal.drawSelectionBoundingBox(box, 1, 1, 1, 0.4F);
        }
        cleanup();

        return true;
    }

    private static boolean renderFacade(IFacadeItem facadeItem, ItemStack stack, Placement placement, EntityPlayer player, World world, float partialTicks) {
        FacadePart part = facadeItem.createPartFromItemStack(stack, AEPartLocation.fromFacing(placement.side()));
        if (part == null) {
            return false;
        }
        IPartHost host = PartHelper.getPartHost(world, placement.pos());
        if (host == null || !ItemFacade.canPlaceFacade(host, part)) {
            return false;
        }

        setup();
        List<AxisAlignedBB> boxes = new ArrayList<>();
        IPartCollisionHelper helper = new BusCollisionHelper(boxes, AEPartLocation.fromFacing(placement.side()), player, true);
        part.getBoxes(helper, player);
        for (AxisAlignedBB box : boxes) {
            box = offsetBox(box, placement.pos(), player, partialTicks);
            RenderGlobal.drawSelectionBoundingBox(box, 1, 1, 1, 0.4F);
        }
        cleanup();

        return true;
    }

    private static void setup() {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
    }

    private static void cleanup() {
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static AxisAlignedBB offsetBox(AxisAlignedBB box, BlockPos pos, EntityPlayer player, float partialTicks) {
        double dX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double dY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double dZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        return box.offset(pos.getX() - dX, pos.getY() - dY, pos.getZ() - dZ).shrink(0.002D);
    }
}
