package appeng.core.transformer;

import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;
import appeng.api.implementations.tiles.IColorableTile;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketColorApplicatorSelectColor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PickBlockPatch extends ClassVisitor {

    public PickBlockPatch(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @SuppressWarnings("unused")
    public static boolean testColorApplicatorPickBlock(RayTraceResult result, EntityPlayer player, World world) {
        if (player == null || player.world == null || result == null || result.typeOfHit != RayTraceResult.Type.BLOCK) {
            return false;
        }

        IItemDefinition applicator = AEApi.instance().definitions().items().colorApplicator();
        if (!applicator.isSameAs(player.getHeldItemMainhand()) && !applicator.isSameAs(player.getHeldItemOffhand())) {
            return false;
        }

        TileEntity tile = player.world.getTileEntity(result.getBlockPos());
        if (tile instanceof IColorableTile colorableTile) {
            NetworkHandler.instance().sendToServer(new PacketColorApplicatorSelectColor(colorableTile.getColor()));
            return true;
        }
        return false;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("onPickBlock".equals(name)) {
            return new OnPickBlockVisitor(mv);
        }
        return mv;
    }

    private static class OnPickBlockVisitor extends MethodVisitor implements Opcodes {

        public OnPickBlockVisitor(MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
        }

        @Override
        public void visitCode() {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    "appeng/core/transformer/PickBlockPatch",
                    "testColorApplicatorPickBlock",
                    "(Lnet/minecraft/util/math/RayTraceResult;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;)Z",
                    false);
            mv.visitInsn(DUP);
            Label exitLabel = new Label();
            mv.visitJumpInsn(IFEQ, exitLabel);

            mv.visitInsn(IRETURN);
            mv.visitLabel(exitLabel);
        }
    }
}
