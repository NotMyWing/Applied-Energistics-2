package appeng.api.util;


import appeng.api.storage.data.IAEStack;


/**
 * An iterable type containing heterogeneous AE stacks.
 */
public interface IUnivStackIterable extends Iterable<IExAEStack<?>>
{

    /**
     * Iterates over the stacks in the iterable.
     *
     * @param visitor the visitor function to apply to each item
     */
    void onEach( Visitor visitor );

    /**
     * Iterates over the stacks in the iterable, possibly terminating early.
     *
     * @param traversal the visitor function to apply to each item
     * @return true if iteration finished completely; false if terminated early
     */
    boolean traverse( Traversal traversal );

    /**
     * Folds over the stacks in the iterable.
     *
     * @param acc         the initial accumulator
     * @param accumulator the folding function
     * @return the result of the fold
     */
    <A> A foldL( A acc, Accumulator<A> accumulator );

    /**
     * Represents a universal visitor function for AE stacks.
     */
    interface Visitor
    {

        /**
         * Visits an AE stack.
         *
         * @param stack the stack being visited
         */
        <T extends IAEStack<T>> void visit( T stack );
    }

    /**
     * Represents a universal traversal function for AE stacks, which is a visitor that may terminate the iteration
     * early.
     */
    interface Traversal
    {

        /**
         * Visits an AE stack, optionally terminating the iteration early.
         *
         * @param stack the stack being visited
         * @return whether to continue iteration or not
         */
        <T extends IAEStack<T>> boolean traverse( T stack );
    }

    /**
     * Represents an accumulation function for AE stacks, which accumulates data extracted from AE stacks into an
     * accumulator.
     */
    interface Accumulator<A>
    {

        /**
         * Folds an AE stack into an accumulator.
         *
         * @param acc   the running accumulator
         * @param stack the stack to fold
         * @return the updated accumulator
         */
        <T extends IAEStack<T>> A accumulate( A acc, T stack );
    }
}
