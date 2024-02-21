package appeng.core.sync;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Allow to register custom GuiBridge from third-party
 */
public class GuiWrapper {

    public static final GuiWrapper INSTANCE = new GuiWrapper();

    private GuiWrapper() {
        // NO-OP
    }

    private final Object2ObjectMap<ResourceLocation, Opener> openers = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectMap<IExternalGui, GuiBridge> externalGuis = new Object2ObjectOpenCustomHashMap<>(new Hash.Strategy<IExternalGui>() {
        @Override
        public int hashCode(IExternalGui o) {
            return o.getID().hashCode();
        }

        @Override
        public boolean equals(@Nullable IExternalGui a, @Nullable IExternalGui b) {
            return a == b || (a != null && b != null && a.getID().equals(b.getID()));
        }
    });

    public synchronized void registerExternalGuiHandler(@Nonnull ResourceLocation id, @Nonnull Opener opener) {
        openers.put(id, opener);
    }

    @Nullable
    public Opener getOpener(ResourceLocation id) {
        return openers.get(id);
    }

    public GuiBridge wrap(IExternalGui obj) {
        return externalGuis.computeIfAbsent(obj, this::create);
    }

    private GuiBridge create(IExternalGui obj) {
        return EnumHelper.addEnum(GuiBridge.class, obj.getID().toString(), new Class<?>[] {IExternalGui.class}, obj);
    }

    @FunctionalInterface
    public interface Opener {

        <T extends IExternalGui> void open(T obj, GuiContext context);

    }

    public interface IExternalGui {

        ResourceLocation getID();

    }

    public static class GuiContext {

        public final World world;
        public final EntityPlayer player;
        public final BlockPos pos;
        public final EnumFacing facing;
        public final NBTTagCompound extra;

        public GuiContext(World world, EntityPlayer player, BlockPos pos, EnumFacing face, NBTTagCompound extra) {
            this.world = world;
            this.player = player;
            this.pos = pos;
            this.facing = face;
            this.extra = extra;
        }

    }

}
