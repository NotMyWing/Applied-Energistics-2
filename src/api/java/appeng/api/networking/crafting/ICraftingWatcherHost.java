package appeng.api.networking.crafting;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;


/**
 * Represents a device that can subscribe to item autocrafting state updates in an ME grid.
 *
 * @deprecated implement and use {@link IUnivCraftingWatcherHost} instead.
 */
@Deprecated
public interface ICraftingWatcherHost extends IUnivCraftingWatcherHost
{

    /**
     * Called when a crafting status changes.
     *
     * @param craftingGrid current crafting grid
     * @param what change
     */
    void onRequestChange( ICraftingGrid craftingGrid, IAEItemStack what );

    @Override
    default <T extends IAEStack<T>> void onUnivRequstChange( final ICraftingGrid craftingGrid, final T what )
    {
        this.onRequestChange(craftingGrid, (IAEItemStack) what);
    }
}
