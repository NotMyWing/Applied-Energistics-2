package appeng.api.util;


import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;


/**
 * A factory function for creating item lists for arbitrary storage channels
 */
public interface IItemListFactory // can't be a functional interface because it's parametric in the stack type
{

    /**
     * @param channel the channel to create an item list for
     * @return the new item list
     */
    <T extends IAEStack<T>> IItemList<T> newList( IStorageChannel<T> channel );

    /**
     * an item list factory that simply calls {@link IStorageChannel#createList()}
     */
    IItemListFactory DEFAULT = new IItemListFactory()
    {

        @Override
        public <T extends IAEStack<T>> IItemList<T> newList( final IStorageChannel<T> channel )
        {
            return channel.createList();
        }

    };

}
