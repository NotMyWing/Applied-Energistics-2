package appeng.util.inv;


import appeng.api.util.IExAEStack;
import appeng.api.util.IUnivStackIterable;

import javax.annotation.Nonnull;
import java.util.Iterator;


public class UnivStackIterableWrapper implements IUnivStackIterable {

    private final Iterable<IExAEStack<?>> delegate;

    public UnivStackIterableWrapper(final Iterable<IExAEStack<?>> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onEach(final Visitor visitor) {
        IExAEStack.onEach(this.delegate, visitor);
    }

    @Override
    public boolean traverse(final Traversal traversal) {
        return IExAEStack.traverse(this.delegate, traversal);
    }

    @Override
    public <A> A foldL(final A acc, final Accumulator<A> accumulator) {
        return IExAEStack.foldL(this.delegate, acc, accumulator);
    }

    @Nonnull
    @Override
    public Iterator<IExAEStack<?>> iterator() {
        return this.delegate.iterator();
    }

}
