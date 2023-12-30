package appeng.hooks;

import appeng.api.AEApi;
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
import org.lwjgl.opengl.GL11;

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

        IPartHost host = PartHelper.getPartHost(world, placement.pos());
        if (stack.getItem() instanceof IPartItem<?> partItem) {
            return renderPart(partItem, stack, placement, player, partialTicks);
        } else if (stack.getItem() instanceof IFacadeItem facadeItem) {
            return renderFacade(facadeItem, stack, host, placement, player, partialTicks);
        }
        return false;
    }

    private static boolean renderPart(IPartItem<?> partItem, ItemStack stack, Placement placement, EntityPlayer player, float partialTicks) {
        IPart part = partItem.createPartFromItemStack(stack);
        if (part == null) {
            return false;
        }

        List<AxisAlignedBB> boxes = new ArrayList<>();
        IPartCollisionHelper helper = new BusCollisionHelper(boxes, AEPartLocation.fromFacing(placement.side()), player, true);
        part.getBoxes(helper);

        renderBoxes(boxes, placement, player, partialTicks);

        return true;
    }

    private static boolean renderFacade(IFacadeItem facadeItem, ItemStack stack, IPartHost host, Placement placement, EntityPlayer player, float partialTicks) {
        FacadePart part = facadeItem.createPartFromItemStack(stack, AEPartLocation.fromFacing(placement.side()));
        if (part == null) {
            return false;
        }
        if (host == null || !ItemFacade.canPlaceFacade(host, part)) {
            return false;
        }
        List<AxisAlignedBB> boxes = new ArrayList<>();
        IPartCollisionHelper helper = new BusCollisionHelper(boxes, AEPartLocation.fromFacing(placement.side()), player, true);
        part.getBoxes(helper, player);

        // Render a cable anchor part box as well if there is no attachments on this side
        if (host.getPart(placement.side()) == null) {
            addAnchorBoxes(helper);
        }

        renderBoxes(boxes, placement, player, partialTicks);

        return true;
    }

    private static void renderBoxes(List<AxisAlignedBB> boxes, Placement placement, EntityPlayer player, float partialTicks) {
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

        // Render boxes "underneath" other parts, blocks, facades etc
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        for (AxisAlignedBB box : boxes) {
            box = offsetBox(box, placement.pos(), player, partialTicks);
            RenderGlobal.drawSelectionBoundingBox(box, 1, 1, 1, 0.2F);
        }
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        // Render normal boxes
        for (AxisAlignedBB box : boxes) {
            box = offsetBox(box, placement.pos(), player, partialTicks);
            RenderGlobal.drawSelectionBoundingBox(box, 1, 1, 1, 0.4F);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static AxisAlignedBB offsetBox(AxisAlignedBB box, BlockPos pos, EntityPlayer player, float partialTicks) {
        double dX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double dY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double dZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        return box.offset(pos.getX() - dX, pos.getY() - dY, pos.getZ() - dZ);
    }

    private static void addAnchorBoxes(IPartCollisionHelper helper) {
        ItemStack anchorStack = AEApi.instance().definitions().parts().cableAnchor().maybeStack(1).orElse(null);
        if (anchorStack != null && anchorStack.getItem() instanceof IPartItem<?> anchorPartItem) {
            IPart anchorPart = anchorPartItem.createPartFromItemStack(anchorStack);
            if (anchorPart != null) {
                anchorPart.getBoxes(helper);
            }
        }
    }
}
