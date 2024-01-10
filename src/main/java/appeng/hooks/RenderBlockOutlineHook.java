package appeng.hooks;

import appeng.api.AEApi;
import appeng.api.parts.IFacadePart;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AEPartLocation;
import appeng.core.AEConfig;
import appeng.facade.FacadePart;
import appeng.facade.IFacadeItem;
import appeng.items.parts.ItemFacade;
import appeng.parts.BusCollisionHelper;
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
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class RenderBlockOutlineHook {

    @SubscribeEvent
    public static void onDrawHighlightEvent(DrawBlockHighlightEvent event) {
        if (event.getTarget() == null) return;
        // noinspection ConstantConditions
        if (event.getTarget().getBlockPos() == null) return;
        if (event.getTarget().typeOfHit != RayTraceResult.Type.BLOCK) return;

        EntityPlayer player = event.getPlayer();
        ItemStack stack = player.getHeldItemMainhand();
        RayTraceResult hitResult = event.getTarget();

        if (player.world.getBlockState(hitResult.getBlockPos()).getBlock() == Blocks.AIR) {
            return;
        }

        if (replaceBlockOutline(player, stack, hitResult, event.getPartialTicks())) {
            event.setCanceled(true);
        }
    }

    private static boolean replaceBlockOutline(EntityPlayer player, ItemStack stack, RayTraceResult hitResult, float partialTicks) {
        BlockPos pos = hitResult.getBlockPos();

        // Render the placement preview
        if (AEConfig.instance().showPlacementPreview()) {
            renderPartPlacementPreview(player, hitResult, stack, partialTicks);
        }

        IPartHost host = AEApi.instance().partHelper().getPartHost(player.world, pos);
        if (host != null) {

            // Try to render facade placement preview here, since it's a
            // convenient time to do it due to having the Part Host already.
            if (AEConfig.instance().showPlacementPreview()) {
                renderFacadePlacementPreview(host, player, hitResult, stack, partialTicks);
            }

            // Render the Part Host block outline, which is done differently from default behavior
            SelectedPart selectedPart = host.selectPartGlobal(hitResult.hitVec);
            if (selectedPart.facade != null) {
                renderFacade(selectedPart.facade, host, pos, selectedPart.side.getFacing(), player, partialTicks, false, false);
                return true;
            }
            if (selectedPart.part != null) {
                renderPart(selectedPart.part, pos, selectedPart.side.getFacing(), player, partialTicks, false, false);
                return true;
            }
        }

        return false;
    }

    /** Render a placement preview for a part item, if possible. */
    private static void renderPartPlacementPreview(EntityPlayer player, RayTraceResult hitResult, ItemStack stack, float partialTicks) {
        if (!(stack.getItem() instanceof IPartItem<?> partItem)) return;

        Placement placement = PartPlacement.getPartPlacement(player, player.world, stack, hitResult.getBlockPos(), hitResult.sideHit);
        if (placement == null) return;
        if (!player.world.getWorldBorder().contains(placement.pos())) return;

        IPart part = partItem.createPartFromItemStack(stack);
        if (part == null) return;

        // Render with two depth passes to render behind blocks
        renderPart(part, placement.pos(), placement.side(), player, partialTicks, true, true);
        renderPart(part, placement.pos(), placement.side(), player, partialTicks, true, false);
    }

    /** Render a placement preview for a facade item, if possible. */
    private static void renderFacadePlacementPreview(@Nonnull IPartHost host, EntityPlayer player, RayTraceResult hitResult, ItemStack stack, float partialTicks) {
        if (!(stack.getItem() instanceof IFacadeItem facadeItem)) return;

        Placement placement = PartPlacement.getPartPlacement(player, player.world, stack, hitResult.getBlockPos(), hitResult.sideHit);
        if (placement == null) return;

        FacadePart part = facadeItem.createPartFromItemStack(stack, AEPartLocation.fromFacing(placement.side()));
        if (part == null) return;
        if (!ItemFacade.canPlaceFacade(host, part)) return;

        // Render with two depth passes to render behind blocks
        renderFacade(part, host, placement.pos(), placement.side(), player, partialTicks, true, true);
        renderFacade(part, host, placement.pos(), placement.side(), player, partialTicks, true, false);
    }

    /** Render a part block outline. */
    private static void renderPart(IPart part, BlockPos pos, EnumFacing side, EntityPlayer player, float partialTicks, boolean preview, boolean insideBlock) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        IPartCollisionHelper helper = new BusCollisionHelper(boxes, AEPartLocation.fromFacing(side), player, true);
        part.getBoxes(helper);
        offsetBoxes(boxes, pos, player, partialTicks);
        renderBoxes(boxes, preview, insideBlock);
    }

    /** Render a facade block outline. */
    private static void renderFacade(IFacadePart facade, IPartHost host, BlockPos pos, EnumFacing side, EntityPlayer player, float partialTicks, boolean preview, boolean insideBlock) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        IPartCollisionHelper helper = new BusCollisionHelper(boxes, AEPartLocation.fromFacing(side), player, true);
        facade.getBoxes(helper, player);

        // Render a cable anchor part box as well if there is no part
        // attachment on this side, and if we are in a preview render pass.
        if (host.getPart(side) == null && preview) {
            addAnchorBox(helper);
        }

        offsetBoxes(boxes, pos, player, partialTicks);
        renderBoxes(boxes, preview, insideBlock);
    }

    /**
     * Render the provided list of AABB boxes as a block outline.
     *
     * @param preview     Whether this is a preview placement or a normal block outline. Determines coloration of the outline.
     * @param insideBlock Whether to disable depth test and darken the outline. Will draw behind other blocks.
     */
    private static void renderBoxes(List<AxisAlignedBB> boxes, boolean preview, boolean insideBlock) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        if (insideBlock) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        for (AxisAlignedBB box : boxes) {
            RenderGlobal.drawSelectionBoundingBox(
                    box,
                    preview ? 1 : 0,
                    preview ? 1 : 0,
                    preview ? 1 : 0,
                    insideBlock ? 0.2F : preview ? 0.6F : 0.4F);
        }

        if (insideBlock) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /** Offset each box in the list to the appropriate render position. */
    private static void offsetBoxes(List<AxisAlignedBB> boxes, BlockPos pos, EntityPlayer player, float partialTicks) {
        double dX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double dY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double dZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        boxes.replaceAll(box -> box.offset(pos.getX() - dX, pos.getY() - dY, pos.getZ() - dZ).grow(0.002D));
    }

    /** Adds a cable anchor box to the collision helper. This does NOT offset the box! */
    private static void addAnchorBox(IPartCollisionHelper helper) {
        ItemStack anchorStack = AEApi.instance().definitions().parts().cableAnchor().maybeStack(1).orElse(null);
        if (anchorStack != null && anchorStack.getItem() instanceof IPartItem<?> anchorPartItem) {
            IPart anchorPart = anchorPartItem.createPartFromItemStack(anchorStack);
            if (anchorPart != null) {
                anchorPart.getBoxes(helper);
            }
        }
    }
}
