package appeng.helpers;

import appeng.tile.inventory.AppEngInternalUnivInventory;


public interface IContainerPatternPacket {

    /**
     * @return inventory for the crafting grid
     */
    AppEngInternalUnivInventory getCraftingInventory();

    /**
     * @return inventory for the output area
     */
    AppEngInternalUnivInventory getOutputInventory();

}
