package appeng.api.util;


import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.IOException;


/**
 * Wraps an {@link IAEStack} to avoid losing the type parameter's bounds in covariant positions. The wildcarded type
 * {@code iExAEStack<?>} can be thought of as an AE stack with its type parameter existentially quantified out.
 *
 * DO NOT IMPLEMENT! Use {@link appeng.api.storage.IStorageHelper#createExStack(IAEStack)} to get an instance.
 */
public interface IExAEStack<T extends IAEStack<T>>
{

    /**
     * @return the underlying stack
     */
    T unwrap();

    /**
     * number of items in the stack.
     *
     * @return basically ItemStack.stackSize
     */
    default long getStackSize()
    {
        return this.unwrap().getStackSize();
    }

    /**
     * changes the number of items in the stack.
     *
     * @param stackSize , ItemStack.stackSize = N
     * @return the same stack
     */
    default IExAEStack<T> setStackSize( final long stackSize )
    {
        this.unwrap().setStackSize(stackSize);
        return this;
    }

    /**
     * Same as getStackSize, but for requestable items. ( LP )
     *
     * @return the requestable size of the stack
     */
    default long getCountRequestable()
    {
        return this.unwrap().getCountRequestable();
    }

    /**
     * Same as setStackSize, but for requestable items. ( LP )
     *
     * @return the same stack
     */
    default IExAEStack<T> setCountRequestable( final long countRequestable )
    {
        this.unwrap().setCountRequestable(countRequestable);
        return this;
    }

    /**
     * true, if the item can be crafted.
     *
     * @return true, if it can be crafted.
     */
    default boolean isCraftable()
    {
        return this.unwrap().isCraftable();
    }

    /**
     * change weather the item can be crafted.
     *
     * @param isCraftable can item be crafted
     * @return the same stack
     */
    default IExAEStack<T> setCraftable( final boolean isCraftable )
    {
        this.unwrap().setCraftable(isCraftable);
        return this;
    }

    /**
     * clears, requestable, craftable, and stack sizes.
     *
     * @return the same stack
     */
    default IExAEStack<T> reset()
    {
        this.unwrap().reset();
        return this;
    }

    /**
     * returns true, if the item can be crafted, requested, or extracted.
     *
     * @return isThisRecordMeaningful
     */
    default boolean isMeaningful()
    {
        return this.unwrap().isMeaningful();
    }

    /**
     * Adds more to the stack size...
     *
     * @param i additional stack size
     */
    default void incStackSize( final long i )
    {
        this.unwrap().incStackSize(i);
    }

    /**
     * removes some from the stack size.
     */
    default void decStackSize( final long i )
    {
        this.unwrap().decStackSize(i);
    }

    /**
     * adds items to the requestable
     *
     * @param i increased amount of requested items
     */
    default void incCountRequestable( final long i )
    {
        this.unwrap().incCountRequestable(i);
    }

    /**
     * removes items from the requestable
     *
     * @param i decreased amount of requested items
     */
    default void decCountRequestable( final long i )
    {
        this.unwrap().decCountRequestable(i);
    }

    /**
     * Clone the Item / Fluid Stack
     *
     * @return a new Stack, which is copied from the original.
     */
    default IExAEStack<T> copy()
    {
        return AEApi.instance().storage().createExStack(this.unwrap().copy());
    }

    /**
     * create an empty stack.
     *
     * @return a new stack, which represents an empty copy of the original.
     */
    default IExAEStack<T> empty()
    {
        return AEApi.instance().storage().createExStack(this.unwrap().empty());
    }

    /**
     * @return the underlying stack's storage channel
     */
    default IStorageChannel<T> getChannel()
    {
        return this.unwrap().getChannel();
    }

    /**
     * Returns itemstack for display and similar purposes. Always has a count of 1.
     *
     * @return itemstack
     */
    default ItemStack asItemStackRepresentation()
    {
        return this.unwrap().asItemStackRepresentation();
    }

    /**
     * Computes the hash code of the underlying stack so as to maintain the (non-compliant!) object identity behaviour
     * of AE stacks.
     *
     * @return the hash code of the underlying stack
     */
    @Override
    int hashCode();

    /**
     * Computes equality comparison relative to the underlying stacks so as to maintain the (non-compliant!) object
     * identity behaviour of AE stacks.
     *
     * @param obj the object to compare against
     * @return whether this stack is identical with the object, modulo AE stack identity
     */
    @Override
    boolean equals( Object obj );

    /**
     * Serializes the stack to a byte buffer. Use
     * {@link appeng.api.storage.IStorageHelper#readExStackFromPacket(ByteBuf)} for deserialization.
     *
     * @param data the byte buffer to write to
     */
    void writeToPacket(final ByteBuf data) throws IOException;

    /**
     * Serializes the stack to a compound tag. Use
     * {@link appeng.api.storage.IStorageHelper#createExStackFromNBT(NBTTagCompound)} for deserialization.
     *
     * @param data the tag to write to
     */
    void writeToNBT(final NBTTagCompound data);

    /**
     * Iterates over a collection of heterogeneous AE stacks.
     *
     * @param iterable the collection to iterate over
     * @param visitor  the visitor function to apply to each element
     */
    static void onEach( final Iterable<IExAEStack<?>> iterable, final IUnivStackIterable.Visitor visitor )
    {
        for ( final IExAEStack<?> stack : iterable )
        {
            visit(stack, visitor);
        }
    }

    /**
     * Iterates over an array of heterogeneous AE stacks.
     *
     * @param arr     the array to iterate over
     * @param visitor the visitor function to apply to each element
     */
    static void onEach( final IExAEStack<?>[] arr, final IUnivStackIterable.Visitor visitor )
    {
        for ( final IExAEStack<?> stack : arr )
        {
            visit(stack, visitor);
        }
    }

    /**
     * Applies a visitor function to a heterogeneous AE stack.
     * @param stack   the stack to visit.
     * @param visitor the visitor function.
     */
    static <T extends IAEStack<T>> void visit( final IExAEStack<T> stack, final IUnivStackIterable.Visitor visitor )
    {
        visitor.visit(stack != null ? stack.unwrap() : null);
    }

    /**
     * Iterates over a collection of heterogeneous AE stacks, possibly terminating early.
     *
     * @param iterable  the iterable to iterate over
     * @param traversal the traversal function to apply to each element
     * @return true if iteration finished completely; false if terminated early
     */
    static boolean traverse( final Iterable<IExAEStack<?>> iterable, final IUnivStackIterable.Traversal traversal )
    {
        for ( final IExAEStack<?> stack : iterable )
        {
            if ( !traverse(stack, traversal) )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Iterates over an array of heterogeneous AE stacks, possibly terminating early.
     *
     * @param arr       the array to iterate over
     * @param traversal the traversal function to apply to each element
     * @return true if iteration finished completely; false if terminated early
     */
    static boolean traverse( final IExAEStack<?>[] arr, final IUnivStackIterable.Traversal traversal )
    {
        for ( final IExAEStack<?> stack : arr )
        {
            if ( !traverse(stack, traversal) )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies a traversal function to a heterogeneous AE stack.
     *
     * @param stack     the stack to traverse.
     * @param traversal the traversal function.
     * @return the result of the traversal
     */
    static <T extends IAEStack<T>> boolean traverse( final IExAEStack<T> stack, final IUnivStackIterable.Traversal traversal )
    {
        return traversal.traverse(stack != null ? stack.unwrap() : null);
    }

    /**
     * Folds over a collection of heterogeneous AE stacks.
     *
     * @param iterable    the collection to fold over
     * @param acc         the initial accumulator
     * @param accumulator the folding function
     * @return the accumulated result
     */
    static <A> A foldL( final Iterable<IExAEStack<?>> iterable, A acc, final IUnivStackIterable.Accumulator<A> accumulator )
    {
        for ( final IExAEStack<?> stack : iterable )
        {
            acc = accumulate(stack, acc, accumulator);
        }
        return acc;
    }

    /**
     * Folds over an array of heterogeneous AE stacks.
     *
     * @param arr         the array to fold over
     * @param acc         the initial accumulator
     * @param accumulator the folding function
     * @return the accumulated result
     */
    static <A> A foldL( final IExAEStack<?>[] arr, A acc, final IUnivStackIterable.Accumulator<A> accumulator )
    {
        for ( final IExAEStack<?> stack : arr )
        {
            acc = accumulate(stack, acc, accumulator);
        }
        return acc;
    }

    /**
     * Applies an accumulation function to a hetergeneous AE stack.
     * @param stack       the stack to accumulate
     * @param acc         the initial accumulator
     * @param accumulator the accumulation function
     * @return the updated accumulator
     */
    static <A, T extends IAEStack<T>> A accumulate( final IExAEStack<T> stack, final A acc, final IUnivStackIterable.Accumulator<A> accumulator )
    {
        return accumulator.accumulate(acc, stack != null ? stack.unwrap() : null);
    }
}
