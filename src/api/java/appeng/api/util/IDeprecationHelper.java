package appeng.api.util;

import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.api.storage.data.IUnivItemList;
import net.minecraft.inventory.InventoryCrafting;

import javax.annotation.Nonnull;


/**
 * Exposes utilities for bridging the gap between old and new API shapes.
 */
public interface IDeprecationHelper
{

    /**
     * Create a new fake {@link InventoryCrafting}.
     *
     * @param width  the width of the crafting grid
     * @param height the height of the crafting grid
     * @return the new crafting inventory
     */
    @Nonnull
    InventoryCrafting createFakeCraftingInventory( int width, int height );

    /**
     * Wraps an item list in a universal item list that throws errors if it receives any operations of other item types.
     *
     * @param channel the storage channel of the list to wrap
     * @param list    the list to wrap
     * @return the wrapped list
     */
    @Nonnull
    <T extends IAEStack<T>> IUnivItemList wrapAsUniv( @Nonnull IStorageChannel<T> channel, @Nonnull IItemList<T> list );
}
