package appeng.hooks;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.util.AEPartLocation;
import appeng.parts.BusCollisionHelper;
import appeng.parts.PartPlacement;
import appeng.parts.PartPlacement.Placement;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
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

        EntityPlayer player = event.getPlayer();
        ItemStack stack = player.getHeldItemMainhand();
        BlockPos hitPos = event.getTarget().getBlockPos();
        EnumFacing hitSide = event.getTarget().sideHit;
        if (replaceBlockOutline(player, player.world, stack, hitPos, hitSide, event.getPartialTicks())) {
            event.setCanceled(true);
        }
    }

    private static boolean replaceBlockOutline(EntityPlayer player, World world, ItemStack stack, BlockPos hitPos, EnumFacing side, float partialTicks) {
        if (!(stack.getItem() instanceof IPartItem<?> partItem)) {
            return false;
        }
        Placement placement = PartPlacement.getPartPlacement(player, world, stack, hitPos, side);
        if (placement == null) {
            return false;
        }
        if (!world.getWorldBorder().contains(placement.pos())) {
            return false;
        }

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

        IPart part = partItem.createPartFromItemStack(stack);
        if (part == null) {
            return false;
        }
        List<AxisAlignedBB> boxes = new ArrayList<>();
        IPartCollisionHelper helper = new BusCollisionHelper(boxes, AEPartLocation.fromFacing(placement.side()), player, true);
        part.getBoxes(helper);
        for (AxisAlignedBB box : boxes) {
            box = offsetBox(box, hitPos, player, partialTicks);
            RenderGlobal.drawSelectionBoundingBox(box, 1, 1, 1, 0.4F);
        }
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        return true;
    }

    private static AxisAlignedBB offsetBox(AxisAlignedBB box, BlockPos pos, EntityPlayer player, float partialTicks) {
        double dX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double dY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double dZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        return box.offset(pos.getX() - dX, pos.getY() - dY, pos.getZ() - dZ);
    }
}
