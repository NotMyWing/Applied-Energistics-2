package appeng.core.sync.packets;

import appeng.block.networking.BlockCableBus;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketCableBusLandingParticle extends AppEngPacket {

    private BlockPos pos;
    private double entityX;
    private double entityY;
    private double entityZ;
    private int numberOfParticles;

    public PacketCableBusLandingParticle(final ByteBuf stream) {
        this.pos = BlockPos.fromLong(stream.readLong());
        this.entityX = stream.readDouble();
        this.entityY = stream.readDouble();
        this.entityZ = stream.readDouble();
        this.numberOfParticles = stream.readInt();
    }

    public PacketCableBusLandingParticle(final BlockPos pos, final Entity entity, final int numberOfParticles) {
        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());
        data.writeLong(pos.toLong());
        data.writeDouble(entity.posX);
        data.writeDouble(entity.posY);
        data.writeDouble(entity.posZ);
        data.writeInt(numberOfParticles);

        this.configureWrite(data);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void clientPacketData(INetworkInfo network, AppEngPacket packet, EntityPlayer player) {
        final World world = Minecraft.getMinecraft().world;
        final IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockCableBus cb) {
            cb.addLandingParticle(pos, entityX, entityY, entityZ, numberOfParticles);
        }
    }
}
