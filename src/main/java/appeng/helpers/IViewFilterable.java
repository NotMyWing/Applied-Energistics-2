package appeng.helpers;

import net.minecraft.item.ItemStack;


public interface IViewFilterable {

    /**
     * @return array of view cells
     */
    ItemStack[] getViewCells();

}
