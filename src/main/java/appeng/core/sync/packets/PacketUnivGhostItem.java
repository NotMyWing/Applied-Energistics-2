package appeng.core.sync.packets;


import appeng.api.util.IExAEStack;
import appeng.core.AELog;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.network.NetworkHandler;
import appeng.helpers.IContainerPatternPacket;
import appeng.helpers.InventoryAction;
import appeng.tile.inventory.AppEngInternalUnivInventory;
import appeng.util.item.AEItemStack;
import appeng.util.item.ExAEStack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.io.IOException;


public class PacketUnivGhostItem extends AppEngPacket {

    private final int slot;
    private final IExAEStack<?> stack;

    // automatic.
    public PacketUnivGhostItem(final ByteBuf stream) throws IOException {
        this.slot = stream.readInt();
        this.stack = ExAEStack.fromPacket(stream);
    }

    // api.
    public PacketUnivGhostItem(final int slot, final @Nonnull IExAEStack<?> stack) throws IOException {
        this.slot = slot;
        this.stack = stack;

        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());
        data.writeInt(slot);
        stack.writeToPacket(data);

        this.configureWrite(data);
    }

    @Override
    public void serverPacketData(final INetworkInfo manager, final AppEngPacket packet, final EntityPlayer player) {
        if (player.openContainer instanceof final IContainerPatternPacket cont) {
            final AppEngInternalUnivInventory crafting = cont.getCraftingInventory();
            final AppEngInternalUnivInventory output = cont.getOutputInventory();

            final int craftingSlots = crafting.getSlots();
            if (this.slot < 0 || this.slot >= craftingSlots + output.getSlots()) {
                return; // slot out of bounds; drop the packet
            } else if (this.slot < craftingSlots) {
                crafting.setStackInSlot(this.slot, this.stack);
            } else {
                output.setStackInSlot(this.slot - craftingSlots, this.stack);
            }

            if (player instanceof final EntityPlayerMP sender) {
                try {
                    NetworkHandler.instance().sendTo(new PacketInventoryAction(InventoryAction.UPDATE_HAND, 0, AEItemStack.fromItemStack(ItemStack.EMPTY)), sender);
                } catch (final IOException e) {
                    AELog.debug(e);
                }
            }
        }
    }

}
