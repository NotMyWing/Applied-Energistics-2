package appeng.core.sync.packets;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.core.Api;
import appeng.core.AppEng;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;

import java.io.IOException;

public class PacketInformPlayer<T extends IAEStack<T>> extends AppEngPacket {
    private T actualItem = null;
    private T reportedItem = null;
    private final InfoType type;

    @SuppressWarnings("unchecked")
    public PacketInformPlayer(ByteBuf stream) throws IOException {
        this.type = InfoType.values()[stream.readInt()];
        final IStorageChannel<T> channel = (IStorageChannel<T>) Api.INSTANCE.storage().getStorageChannelById(stream.readByte());
        switch (type) {
            case PARTIAL_ITEM_EXTRACTION:
                this.reportedItem = channel.readFromPacket(stream);
                this.actualItem = channel.readFromPacket(stream);
                break;
            case NO_ITEMS_EXTRACTED:
                this.reportedItem = channel.readFromPacket(stream);
                break;
        }
    }

    public PacketInformPlayer(T expected, T actual, InfoType type) throws IOException {
        this.reportedItem = expected;
        this.actualItem = actual;
        this.type = type;

        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());

        data.writeInt(type.ordinal());

        data.writeByte(Api.INSTANCE.storage().getStorageChannelId(reportedItem.getChannel()));
        reportedItem.writeToPacket(data);
        if (actualItem != null) {
            actualItem.writeToPacket(data);
        }

        this.configureWrite(data);
    }

    @Override
    public void clientPacketData(final INetworkInfo network, final AppEngPacket packet, final EntityPlayer player) {
        if (this.type == InfoType.PARTIAL_ITEM_EXTRACTION) {
            AppEng.proxy.getPlayers().get(0).sendStatusMessage(new TextComponentString("System reported " + reportedItem.getStackSize() + " " + reportedItem.asItemStackRepresentation().getDisplayName() + " available but could only extract " + actualItem.getStackSize()), false);
        } else if (this.type == InfoType.NO_ITEMS_EXTRACTED) {
            AppEng.proxy.getPlayers().get(0).sendStatusMessage(new TextComponentString("System reported " + reportedItem.getStackSize() + " " + reportedItem.asItemStackRepresentation().getDisplayName() + " available but could not extract anything"), false);
        }
    }

    public enum InfoType {
        PARTIAL_ITEM_EXTRACTION, NO_ITEMS_EXTRACTED
    }
}
