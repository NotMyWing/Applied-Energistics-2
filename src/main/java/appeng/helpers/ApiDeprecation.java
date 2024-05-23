package appeng.helpers;

import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.api.storage.data.IUnivItemList;
import appeng.api.util.IDeprecationHelper;
import appeng.api.util.IExAEStack;
import appeng.container.ContainerNull;
import appeng.util.item.ExAEStack;
import com.google.common.collect.Iterators;
import net.minecraft.inventory.InventoryCrafting;

import javax.annotation.Nonnull;
import java.util.Iterator;


public class ApiDeprecation implements IDeprecationHelper {

    @Nonnull
    @Override
    public InventoryCrafting createFakeCraftingInventory(final int width, final int height) {
        return new InventoryCrafting(new ContainerNull(), width, height);
    }

    @Nonnull
    @Override
    public <T extends IAEStack<T>> IUnivItemList wrapAsUniv(@Nonnull final IStorageChannel<T> channel, @Nonnull final IItemList<T> list) {
        return new ItemListWrapper<>(channel, list);
    }

    private record ItemListWrapper<T extends IAEStack<T>>(IStorageChannel<T> channel,
                                                          IItemList<T> backing) implements IUnivItemList {
        @SuppressWarnings("unchecked")
        @Override
        public <U extends IAEStack<U>> IItemList<U> listFor(final IStorageChannel<U> channel) {
            if (channel != this.channel) {
                throw new UnsupportedOperationException(String.format("%s operation on %s list!",
                        channel.getUnderlyingStackType().getSimpleName(), this.channel.getUnderlyingStackType().getSimpleName()));
            }
            return (IItemList<U>) this.backing;
        }

        @Override
        public boolean isEmpty() {
            return this.backing.isEmpty();
        }

        @Override
        public int size() {
            return this.backing.size();
        }

        @Override
        public Iterator<IExAEStack<?>> iterator() {
            return Iterators.transform(this.backing.iterator(), ExAEStack::of);
        }

        @Override
        public void resetStatus() {
            this.backing.resetStatus();
        }

        @Override
        public void onEach(final Visitor visitor) {
            for (final T stack : this.backing) {
                visitor.visit(stack);
            }
        }

        @Override
        public boolean traverse(final Traversal traversal) {
            for (final T stack : this.backing) {
                if (!traversal.traverse(stack)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public <A> A foldL(A acc, final Accumulator<A> accumulator) {
            for (final T stack : this.backing) {
                acc = accumulator.accumulate(acc, stack);
            }
            return acc;
        }
    }

}
