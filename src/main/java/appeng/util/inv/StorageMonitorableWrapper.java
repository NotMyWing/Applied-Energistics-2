package appeng.util.inv;


import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEUnivInventory;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.api.storage.data.IUnivItemList;


public class StorageMonitorableWrapper implements IMEUnivInventory {

    private final IStorageMonitorable delegate;

    public StorageMonitorableWrapper(final IStorageMonitorable delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T extends IAEStack<T>> IMEInventory<T> inventoryFor(final IStorageChannel<T> channel) {
        return this.delegate.getInventory(channel);
    }

    @Override
    public <T extends IAEStack<T>> T injectItems(final T input, final Actionable type, final IActionSource src) {
        return this.inventoryFor(input.getChannel()).injectItems(input, type, src);
    }

    @Override
    public <T extends IAEStack<T>> T extractItems(final T request, final Actionable mode, final IActionSource src) {
        return this.inventoryFor(request.getChannel()).extractItems(request, mode, src);
    }

    @Override
    public IUnivItemList getAvailableItems(final IUnivItemList out) {
        for (final IStorageChannel<?> channel : AEApi.instance().storage().storageChannels()) {
            this.getAvailableItems(channel, out);
        }

        return out;
    }

    private <T extends IAEStack<T>> void getAvailableItems(final IStorageChannel<T> channel, final IUnivItemList out) {
        this.getAvailableItems(channel, out.listFor(channel));
    }

    @Override
    public <T extends IAEStack<T>> IItemList<T> getAvailableItems(final IStorageChannel<T> channel, final IItemList<T> out) {
        return this.inventoryFor(channel).getAvailableItems(out);
    }
}
