package appeng.util.inv;


import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEUnivInventory;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;


public class UnivSubInventoryDelegate<T extends IAEStack<T>> implements IMEInventory<T> {

    private final IMEUnivInventory inv;
    private final IStorageChannel<T> channel;

    public UnivSubInventoryDelegate(final IMEUnivInventory inv, final IStorageChannel<T> channel) {
        this.inv = inv;
        this.channel = channel;
    }

    @Override
    public T injectItems(final T input, final Actionable type, final IActionSource src) {
        return this.inv.injectItems(input, type, src);
    }

    @Override
    public T extractItems(final T request, final Actionable mode, final IActionSource src) {
        return this.inv.extractItems(request, mode, src);
    }

    @Override
    public IItemList<T> getAvailableItems(final IItemList<T> out) {
        return this.inv.getAvailableItems(channel, out);
    }

    @Override
    public IStorageChannel<T> getChannel() {
        return this.channel;
    }
}
