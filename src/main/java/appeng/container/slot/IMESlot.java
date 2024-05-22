package appeng.container.slot;

import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;

import javax.annotation.Nonnull;


public interface IMESlot<T extends IAEStack<T>> {

    T getAEStack();

    @Nonnull
    IStorageChannel<T> getSlotChannel();

}
