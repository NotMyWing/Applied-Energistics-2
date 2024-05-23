package appeng.helpers;

import appeng.core.sync.GuiBridge;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


public interface IGuiHost {

    GuiBridge getGui(EntityPlayer player);

    ItemStack getItemStackRepresentation();

}
