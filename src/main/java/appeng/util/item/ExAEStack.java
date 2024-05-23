package appeng.util.item;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IExAEStack;
import appeng.core.Api;
import appeng.helpers.ItemStackHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.io.IOException;


public class ExAEStack<T extends IAEStack<T>> implements IExAEStack<T> {

    private static final String NBT_CHANNEL = "StrgChnl";

    public static <T extends IAEStack<T>> IExAEStack<T> of(final T stack) {
        return stack != null ? new ExAEStack<>(stack) : null;
    }

    @Nonnull
    private final T stack;

    private ExAEStack(@Nonnull final T stack) {
        this.stack = stack;
    }

    @Override
    public T unwrap() {
        return this.stack;
    }

    @Override
    public String toString() {
        return this.stack.toString();
    }

    @Override
    public int hashCode() {
        return this.stack.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ExAEStack<?>) {
            return this.stack.equals(((ExAEStack<?>) obj).stack);
        } else {
            return this.stack.equals(obj); // AE stacks like to break object identity contracts
        }
    }

    public void writeToPacket(final ByteBuf data) throws IOException {
        data.writeByte(Api.INSTANCE.storage().getStorageChannelId(this.stack.getChannel()));
        this.stack.writeToPacket(data);
    }

    public static IExAEStack<?> fromPacket(@Nonnull final ByteBuf input) throws IOException {
        return fromPacket(Api.INSTANCE.storage().getStorageChannelById(input.readByte()), input);
    }

    private static <T extends IAEStack<T>> IExAEStack<T> fromPacket(final IStorageChannel<T> channel, final ByteBuf input) throws IOException {
        return ExAEStack.of(channel.readFromPacket(input));
    }

    public void writeToNBT(final NBTTagCompound data) {
        data.setString(NBT_CHANNEL, this.stack.getChannel().getChannelType().getName());
        this.stack.writeToNBT(data);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static IExAEStack<?> fromNBT(final NBTTagCompound nbt) {
        if (nbt == null) {
            return null;
        }

        if (!nbt.hasKey(NBT_CHANNEL, Constants.NBT.TAG_STRING)) { // backwards-compatibility
            final IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);

            // first, try deserializing it as an IAEItemStack
            final IAEItemStack stack = channel.createFromNBT(nbt);
            if (stack != null && stack.getStackSize() != 0) {
                return ExAEStack.of(stack);
            }

            // if that fails, try deserializing it as a vanilla ItemStack
            return ExAEStack.of(channel.createStack(ItemStackHelper.stackFromNBT(nbt)));
        }

        final Class<? extends IStorageChannel> channelClass;
        final String channelClassName = nbt.getString(NBT_CHANNEL);
        try {
            channelClass = Class.forName(channelClassName).asSubclass(IStorageChannel.class);
        } catch (ClassNotFoundException | ClassCastException e) {
            return null;
        }

        return ExAEStack.of(AEApi.instance().storage().getStorageChannel(channelClass).createFromNBT(nbt));
    }

}
