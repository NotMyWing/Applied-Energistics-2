package appeng.util.item;

import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.api.storage.data.IUnivItemList;
import appeng.api.util.IExAEStack;
import com.google.common.collect.Iterators;

import java.util.Iterator;


public class StorageListWrapper implements IUnivItemList {

    private final IStorageMonitorable storage;

    public StorageListWrapper(final IStorageMonitorable storage) {
        this.storage = storage;
    }

    @Override
    public <T extends IAEStack<T>> IItemList<T> listFor(final IStorageChannel<T> channel) {
        return this.storage.getInventory(channel).getStorageList();
    }

    @Override
    public boolean isEmpty() {
        for (final IStorageChannel<? extends IAEStack<?>> channel : AEApi.instance().storage().storageChannels()) {
            if (!this.listFor(channel).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int size() {
        int size = 0;
        for (final IStorageChannel<? extends IAEStack<?>> channel : AEApi.instance().storage().storageChannels()) {
            size += this.listFor(channel).size();
        }
        return size;
    }

    @Override
    public Iterator<IExAEStack<?>> iterator() {
        return Iterators.concat(Iterators.transform(AEApi.instance().storage().storageChannels().iterator(), this::iterateChannel));
    }

    private <T extends IAEStack<T>> Iterator<IExAEStack<?>> iterateChannel(IStorageChannel<T> channel) {
        return Iterators.transform(this.listFor(channel).iterator(), ExAEStack::of);
    }

    @Override
    public void resetStatus() {
        for (final IStorageChannel<? extends IAEStack<?>> channel : AEApi.instance().storage().storageChannels()) {
            this.listFor(channel).resetStatus();
        }
    }

    @Override
    public void onEach(final Visitor visitor) {
        for (final IStorageChannel<? extends IAEStack<?>> channel : AEApi.instance().storage().storageChannels()) {
            this.onEach(channel, visitor);
        }
    }

    private <T extends IAEStack<T>> void onEach(final IStorageChannel<T> channel, final Visitor visitor) {
        for (final T stack : this.listFor(channel)) {
            visitor.visit(stack);
        }
    }

    @Override
    public boolean traverse(final Traversal traversal) {
        for (final IStorageChannel<? extends IAEStack<?>> channel : AEApi.instance().storage().storageChannels()) {
            if (!this.traverse(channel, traversal)) {
                return false;
            }
        }
        return true;
    }

    private <T extends IAEStack<T>> boolean traverse(final IStorageChannel<T> channel, final Traversal traversal) {
        for (final T stack : this.listFor(channel)) {
            if (!traversal.traverse(stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public <A> A foldL(A acc, final Accumulator<A> accumulator) {
        for (final IStorageChannel<? extends IAEStack<?>> channel : AEApi.instance().storage().storageChannels()) {
            acc = this.foldL(channel, acc, accumulator);
        }
        return acc;
    }

    private <A, T extends IAEStack<T>> A foldL(final IStorageChannel<T> channel, A acc, final Accumulator<A> accumulator) {
        for (final T stack : this.listFor(channel)) {
            acc = accumulator.accumulate(acc, stack);
        }
        return acc;
    }
}
