package appeng.core.sync.packets;

import appeng.api.util.AEColor;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.items.tools.powered.ToolColorApplicator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class PacketColorApplicatorSelectColor extends AppEngPacket {

    @Nullable
    private AEColor color = null;

    @SuppressWarnings("unused")
    public PacketColorApplicatorSelectColor(final ByteBuf stream) {
        if (stream.readBoolean()) {
            byte colorIdx = stream.readByte();
            AEColor[] values = AEColor.values();
            if (colorIdx >= 0 && colorIdx < values.length) {
                this.color = values[colorIdx];
            }
        }
    }

    public PacketColorApplicatorSelectColor(@Nullable final AEColor color) {
        final ByteBuf data = Unpooled.buffer();
        data.writeInt(this.getPacketID());
        if (color != null) {
            data.writeBoolean(true);
            data.writeByte(color.ordinal());
        } else {
            data.writeBoolean(false);
        }
        this.configureWrite(data);
    }

    @Override
    public void serverPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player) {
        switchColor(player.getHeldItemMainhand(), color);
        switchColor(player.getHeldItemOffhand(), color);
    }

    private static void switchColor(ItemStack stack, AEColor color) {
        if (!stack.isEmpty() && stack.getItem() instanceof ToolColorApplicator colorApp) {
            colorApp.setActiveColor(stack, color);
        }
    }
}
