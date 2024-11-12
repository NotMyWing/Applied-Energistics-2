package appeng.integration.modules.waila.part;

import appeng.api.parts.IPart;
import appeng.core.localization.WailaText;
import appeng.parts.automation.PartAnnihilationPlane;
import appeng.parts.automation.PartIdentityAnnihilationPlane;
import appeng.util.EnchantmentUtil;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;

public class AnnihilationPlaneDataProvider extends BasePartWailaDataProvider {
    @Override
    public List<String> getWailaBody(IPart part, List<String> currentToolTip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        if (part instanceof PartIdentityAnnihilationPlane) {
            currentToolTip.add(WailaText.IdentityDeprecated.getLocal());
        } else if (part instanceof PartAnnihilationPlane plane) {
            NBTTagCompound nbtData = accessor.getNBTData();
            Map<Enchantment, Integer> enchantments = EnchantmentUtil.getEnchantments(nbtData);
            if (!enchantments.isEmpty()) {
                currentToolTip.add(WailaText.EnchantedWith.getLocal());
                for (var enchantment : enchantments.keySet()) {
                    currentToolTip.add(enchantment.getTranslatedName(enchantments.get(enchantment)));
                }
            }
        }

        return currentToolTip;
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, IPart part, TileEntity te, NBTTagCompound tag, World world, BlockPos pos) {
        if (part instanceof PartAnnihilationPlane plane) {
            plane.writeEnchantments(tag);
        }

        return tag;
    }
}
