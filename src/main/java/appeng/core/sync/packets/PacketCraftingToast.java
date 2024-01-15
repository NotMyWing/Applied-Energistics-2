package appeng.core.sync.packets;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.toasts.CraftingStatusToast;
import appeng.core.AEConfig;
import appeng.core.features.AEFeature;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

public class PacketCraftingToast extends AppEngPacket {
	private final IAEItemStack stack;
	private final boolean cancelled;

	public PacketCraftingToast(final ByteBuf stream) {
		this.stack = AEItemStack.fromPacket(stream);
		this.cancelled = stream.readBoolean();
	}

	public PacketCraftingToast(IAEItemStack stack, boolean cancelled) throws IOException {
		this.stack = stack;
		this.cancelled = cancelled;

		final ByteBuf data = Unpooled.buffer();

		data.writeInt(this.getPacketID());
		stack.writeToPacket(data);
		data.writeBoolean(cancelled);

		this.configureWrite(data);
	}

	@Override
	public void clientPacketData(INetworkInfo network, AppEngPacket packet, EntityPlayer player) {
		if (AEConfig.instance().isFeatureEnabled(AEFeature.CRAFTING_TOASTS)) {
			doCraftingToast();
		}
	}

	@SideOnly(Side.CLIENT)
	private void doCraftingToast() {
		Minecraft.getMinecraft().getToastGui().add(new CraftingStatusToast(stack.asItemStackRepresentation(), cancelled));
	}

	@Override
	public void serverPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player) {}
}
