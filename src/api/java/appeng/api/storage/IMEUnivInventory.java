package appeng.api.storage;


import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.api.storage.data.IUnivItemList;


/**
 * Represents a heterogeneous {@link IMEInventory}, which stores items of any arbitrary item type.
 */
public interface IMEUnivInventory
{
    /**
     * Gets the subinventory for a particular item type.
     * @param channel the storage channel for the item type of interest
     * @return the subinventory for the given channel
     */
    <T extends IAEStack<T>> IMEInventory<T> inventoryFor( IStorageChannel<T> channel );

    /**
     * Store new items, or simulate the addition of new items into the ME Inventory.
     *
     * @param input item to add.
     * @param type action type
     * @param src action source
     *
     * @return returns the number of items not added.
     */
    <T extends IAEStack<T>> T injectItems( T input, Actionable type, IActionSource src );

    /**
     * Extract the specified item from the ME Inventory
     *
     * @param request item to request ( with stack size. )
     * @param mode simulate, or perform action?
     *
     * @return the number of items extracted, or null if none
     */
    <T extends IAEStack<T>> T extractItems( T request, Actionable mode, IActionSource src );

    /**
     * request a full report of all available items, storage.
     *
     * @param out the IItemList the results will be written to
     *
     * @return the same list that was passed in
     */
    IUnivItemList getAvailableItems( IUnivItemList out );

    /**
     * request a report of all available items and storage for a particular item type.
     *
     * @param channel the storage channel for the item type
     * @param out the IItemList the results will be written to
     *
     * @return the same list that was passed in
     */
    <T extends IAEStack<T>> IItemList<T> getAvailableItems( IStorageChannel<T> channel, IItemList<T> out );
}
