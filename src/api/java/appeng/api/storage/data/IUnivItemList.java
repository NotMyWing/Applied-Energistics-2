package appeng.api.storage.data;


import appeng.api.config.FuzzyMode;
import appeng.api.storage.IStorageChannel;
import appeng.api.util.IExAEStack;
import appeng.api.util.IUnivStackIterable;

import java.util.Collection;
import java.util.Iterator;


/**
 * Represents a heterogeneous list of items in AE, which may admit a sublist for any arbitrary item type represented by
 * an AE storage channel.
 * <p>
 * Don't Implement.
 * <p>
 * Construct with {@code AEApi.instance().storage().createUnivList()}
 */
public interface IUnivItemList extends IUnivStackIterable
{

    /**
     * Gets the sublist for a particular item type.
     *
     * @param channel the storage channel for the item type of interest
     * @return the sublist for the given channel
     */
    <T extends IAEStack<T>> IItemList<T> listFor( final IStorageChannel<T> channel );

    /**
     * add a stack to the list, this will merge the stack with an item already in the list if found.
     *
     * @param option added stack
     */
    default <T extends IAEStack<T>> void add( final T option )
    {
        this.listFor(option.getChannel()).add(option);
    }

    /**
     * @param i compared item
     * @return a stack equivalent to the stack passed in, but with the correct stack size information, or null if its
     * not present
     */
    default <T extends IAEStack<T>> T findPrecise( final T i )
    {
        return this.listFor(i.getChannel()).findPrecise(i);
    }

    /**
     * @param input compared item
     * @return a list of relevant fuzzy matched stacks
     */
    default <T extends IAEStack<T>> Collection<T> findFuzzy( final T input, final FuzzyMode fuzzy )
    {
        return this.listFor(input.getChannel()).findFuzzy(input, fuzzy);
    }

    /**
     * @return true if there are no items in the list
     */
    boolean isEmpty();

    /**
     * add a stack to the list stackSize is used to add to stackSize, this will merge the stack with an item already in
     * the list if found.
     *
     * @param option stacktype option
     */
    default <T extends IAEStack<T>> void addStorage( final T option )
    {
        this.listFor(option.getChannel()).addStorage(option);
    }

    /**
     * add a stack to the list as craftable, this will merge the stack with an item already in the list if found.
     *
     * @param option stacktype option
     */
    default <T extends IAEStack<T>> void addCrafting( final T option )
    {
        this.listFor(option.getChannel()).addCrafting(option);
    }

    /**
     * add a stack to the list, stack size is used to add to requestable, this will merge the stack with an item already
     * in the list if found.
     *
     * @param option stacktype option
     */
    default <T extends IAEStack<T>> void addRequestable( final T option )
    {
        this.listFor(option.getChannel()).addRequestable(option);
    }

    /**
     * @return the number of items in the list
     */
    int size();

    /**
     * allows you to iterate the list.
     */
    @Override
    Iterator<IExAEStack<?>> iterator();

    /**
     * resets stack sizes to 0.
     */
    void resetStatus();
}
