package appeng.api.networking.crafting;

import appeng.api.storage.data.IAEStack;
import appeng.api.util.IExAEStack;
import appeng.api.util.IUnivStackIterable;
import net.minecraft.inventory.InventoryCrafting;

/**
 * Represents a variant of {@link InventoryCrafting} with heterogeneous item slots for use as an autocrafting buffer.
 */
public interface ICraftingInventory extends IUnivStackIterable
{

    /**
     * @return the width of the crafting area
     */
    int getWidth();

    /**
     * @return the height of the crafting area
     */
    int getHeight();

    /**
     * @return the number of slots in the inventory
     */
    int getSlotCount();

    /**
     * Sets the item stack in the given inventory slot.
     *
     * @param slotIndex the index of the slot
     * @param stack     the stack to insert
     */
    <T extends IAEStack<T>> void setStackInSlot( int slotIndex, T stack );

    /**
     * Sets the item stack in the given inventory slot.
     *
     * @param slotIndex the index of the slot
     * @param stack     the stack to insert
     */
    <T extends IAEStack<T>> void setStackInSlot( final int slotIndex, final IExAEStack<T> stack );

    /**
     * Retrieves the item stack in the given inventory slot.
     *
     * @param slotIndex the index of the slot
     * @return the stack in the slot
     */
    IExAEStack<?> getStackInSlot(int slotIndex );

    /**
     * Coerces the contents of this inventory into a vanilla {@link InventoryCrafting}.
     *
     * @return the vanilla crafting inventory, or null if this inventory contains non-item ingredients
     */
    InventoryCrafting asVanilla();
}
