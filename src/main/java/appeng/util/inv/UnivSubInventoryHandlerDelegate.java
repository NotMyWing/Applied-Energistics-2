package appeng.util.inv;


import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEUnivInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;


public class UnivSubInventoryHandlerDelegate<T extends IAEStack<T>> implements IMEInventoryHandler<T> {

    private final IMEUnivInventoryHandler delegate;
    private final UnivSubInventoryDelegate<T> inv;

    public UnivSubInventoryHandlerDelegate(IMEUnivInventoryHandler delegate, IStorageChannel<T> channel) {
        this.delegate = delegate;
        this.inv = new UnivSubInventoryDelegate<>(delegate, channel);
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
        return this.inv.getAvailableItems(out);
    }

    @Override
    public IStorageChannel<T> getChannel() {
        return this.inv.getChannel();
    }

    @Override
    public AccessRestriction getAccess() {
        return this.delegate.getAccess();
    }

    @Override
    public boolean isPrioritized(final T input) {
        return this.delegate.isPrioritized(input);
    }

    @Override
    public boolean canAccept(final T input) {
        return this.delegate.canAccept(input);
    }

    @Override
    public int getPriority() {
        return this.delegate.getPriority();
    }

    @Override
    public int getSlot() {
        return this.delegate.getSlot();
    }

    @Override
    public boolean validForPass(final int i) {
        return this.delegate.validForPass(i);
    }

    @Override
    public boolean isSticky() {
        return this.delegate.isSticky();
    }

}
