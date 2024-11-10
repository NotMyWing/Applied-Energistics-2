package appeng.integration.modules.waila.part;

import appeng.api.parts.IPart;
import appeng.core.localization.GuiText;
import appeng.parts.automation.PartAnnihilationPlane;
import appeng.parts.automation.PartIdentityAnnihilationPlane;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.enchantment.EnchantmentHelper;

import java.util.List;

public class AnnihilationPlaneDataProvider extends BasePartWailaDataProvider {
    @Override
    public List<String> getWailaBody(IPart part, List<String> currentToolTip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        if (part instanceof PartIdentityAnnihilationPlane) {
            currentToolTip.add(GuiText.IdentityDeprecated.getLocal());
        } else if (part instanceof PartAnnihilationPlane plane) {
            var enchantments = EnchantmentHelper.getEnchantments(plane.getItemStack());
            if (!enchantments.isEmpty()) {
                currentToolTip.add(GuiText.EnchantedWith.getLocal());
                for (var enchantment : enchantments.keySet()) {
                    currentToolTip.add(enchantment.getTranslatedName(enchantments.get(enchantment)));
                }
            }
        }

        return currentToolTip;
    }
}
