package appeng.util.item;


import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemContainer;
import appeng.api.storage.data.IItemList;
import appeng.api.storage.data.IUnivItemList;
import appeng.api.util.IExAEStack;
import appeng.api.util.IItemListFactory;
import com.google.common.collect.Iterators;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;


public class UnivItemList implements IUnivItemList {

    private final IItemListFactory factory;
    private final Map<IStorageChannel<?>, IItemList<?>> lists = new IdentityHashMap<>();

    public UnivItemList(final IItemListFactory factory) {
        this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IAEStack<T>> IItemList<T> listFor(final IStorageChannel<T> channel) {
        return (IItemList<T>) this.lists.computeIfAbsent(channel, factory::newList);
    }

    @Override
    public boolean isEmpty() {
        return this.lists.values().stream().allMatch(IItemContainer::isEmpty);
    }

    @Override
    public int size() {
        return this.lists.values().stream().mapToInt(IItemList::size).sum();
    }

    @Override
    public Iterator<IExAEStack<?>> iterator() {
        return Iterators.concat(Iterators.transform(this.lists.values().iterator(), UnivItemList::iterateSubList));
    }

    private static <T extends IAEStack<T>> Iterator<IExAEStack<?>> iterateSubList(final IItemList<T> list) {
        return Iterators.transform(list.iterator(), ExAEStack::of);
    }

    @Override
    public void onEach(final Visitor visitor) {
        for (final IItemList<?> list : this.lists.values()) {
            visitList(visitor, list);
        }
    }

    private static <T extends IAEStack<T>> void visitList(final Visitor visitor, final IItemList<T> list) {
        for (final T stack : list) {
            visitor.visit(stack);
        }
    }

    @Override
    public boolean traverse(final Traversal traversal) {
        for (final IItemList<?> list : this.lists.values()) {
            if (!traverseList(traversal, list)) {
                return false;
            }
        }
        return true;
    }

    private static <T extends IAEStack<T>> boolean traverseList(final Traversal traversal, final IItemList<T> list) {
        for (final T stack : list) {
            if (!traversal.traverse(stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public <A> A foldL(A acc, final Accumulator<A> accumulator) {
        for (final IItemList<?> list : this.lists.values()) {
            acc = foldList(acc, accumulator, list);
        }
        return acc;
    }

    private static <A, T extends IAEStack<T>> A foldList(A acc, final Accumulator<A> accumulator, final IItemList<T> list) {
        for (final T stack : list) {
            acc = accumulator.accumulate(acc, stack);
        }
        return acc;
    }

    @Override
    public void resetStatus() {
        for (final IItemList<?> list : this.lists.values()) {
            list.resetStatus();
        }
    }

}
