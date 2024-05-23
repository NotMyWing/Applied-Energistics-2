package appeng.tile.inventory;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IExAEStack;
import appeng.util.inv.InvOperation;
import appeng.util.item.AEItemStack;
import appeng.util.item.ExAEStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


/**
 * A variant of {@link AppEngInternalAEInventory} that supports heterogeneous contents represented by
 * {@link appeng.api.util.IExAEStack}.
 */
public class AppEngInternalUnivInventory implements Iterable<IExAEStack<?>> {

    private final IListener owner;
    private final List<IExAEStack<?>> stacks;
    private boolean dirtyFlag = false;
    private AsItemHandler asItemHandler = null;

    public AppEngInternalUnivInventory(final IListener owner, final int size) {
        this.owner = owner;
        this.stacks = Arrays.asList(new IExAEStack<?>[size]);
    }

    public int getSlots() {
        return this.stacks.size();
    }

    public IExAEStack<?> getStackInSlot(final int slot) {
        return this.stacks.get(slot);
    }

    public void setStackInSlot(final int slot, final IExAEStack<?> stack) {
        final IExAEStack<?> previousStack = this.stacks.set(slot, stack);
        this.onContentsChanged(slot, previousStack);
    }

    public <T extends IAEStack<T>> void setStackInSlot(final int slot, final T stack) {
        this.setStackInSlot(slot, ExAEStack.of(stack));
    }

    private void onContentsChanged(final int slot, final IExAEStack<?> previousStack) {
        if (this.owner != null && !this.dirtyFlag) {
            this.dirtyFlag = true;
            IExAEStack<?> newStack = this.getStackInSlot(slot);
            IExAEStack<?> oldStack = previousStack != null ? previousStack.copy() : null;
            InvOperation op = InvOperation.SET;

            if (newStack == null) {
                op = InvOperation.EXTRACT;
            } else if (oldStack == null) {
                newStack = newStack.copy();
                op = InvOperation.INSERT;
            } else if (oldStack.getStackSize() != newStack.getStackSize() && newStack.equals(oldStack)) {
                if (newStack.getStackSize() > oldStack.getStackSize()) {
                    newStack = newStack.copy();
                    newStack.setStackSize(newStack.getStackSize() - oldStack.getStackSize());
                    oldStack = null;
                    op = InvOperation.INSERT;
                } else {
                    oldStack.setStackSize(oldStack.getStackSize() - newStack.getStackSize());
                    newStack = null;
                    op = InvOperation.EXTRACT;
                }
            }

            this.owner.onChangeInventory(this, slot, op, oldStack, newStack);
            this.dirtyFlag = false;
        }
    }

    @Nonnull
    @Override
    public Iterator<IExAEStack<?>> iterator() {
        return this.stacks.iterator();
    }

    public IItemHandler asItemHandler() {
        if (this.asItemHandler == null) {
            this.asItemHandler = new AsItemHandler();
        }
        return this.asItemHandler;
    }

    public void writeToNBT(final NBTTagCompound parent, final String key) {
        final NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        parent.setTag(key, nbt);
    }

    public void writeToNBT(final NBTTagCompound nbt) {
        final NBTTagList itemsTag = new NBTTagList();
        for (int i = 0; i < this.stacks.size(); i++) {
            final IExAEStack<?> stack = this.stacks.get(i);
            if (stack == null) {
                continue;
            }

            final NBTTagCompound itemTag = new NBTTagCompound();
            stack.writeToNBT(itemTag);
            itemTag.setInteger("Slot", i);
            itemsTag.appendTag(itemTag);
        }
        nbt.setTag("Items", itemsTag);
    }

    public void readFromNBT(final NBTTagCompound parent, final String key) {
        if (parent.hasKey(key, Constants.NBT.TAG_COMPOUND)) {
            readFromNBT(parent.getCompoundTag(key));
        }
    }

    public void readFromNBT(final NBTTagCompound nbt) { // (mostly) backwards-compatible with AppEngInternalInventory
        final NBTTagList itemsTag = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < itemsTag.tagCount(); i++) {
            final NBTTagCompound itemTag = itemsTag.getCompoundTagAt(i);
            final int slot = itemTag.getInteger("Slot");
            if (slot >= 0 && slot < this.stacks.size()) {
                this.stacks.set(slot, ExAEStack.fromNBT(itemTag));
            }
        }
    }

    public interface IListener {

        void onChangeInventory(AppEngInternalUnivInventory inv, int slot, InvOperation op, IExAEStack<?> oldStack, IExAEStack<?> newStack);

    }

    private class AsItemHandler implements IItemHandlerModifiable {

        @Override
        public void setStackInSlot(final int slot, @Nonnull final ItemStack stack) {
            AppEngInternalUnivInventory.this.setStackInSlot(slot, AEItemStack.fromItemStack(stack));
        }

        @Override
        public int getSlots() {
            return AppEngInternalUnivInventory.this.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(final int slot) {
            final IExAEStack<?> inSlot = AppEngInternalUnivInventory.this.getStackInSlot(slot);
            if (inSlot != null) {
                if (inSlot.unwrap() instanceof final IAEItemStack ais) {
                    return ais.createItemStack();
                } else {
                    return inSlot.asItemStackRepresentation();
                }
            }
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(final int slot, @Nonnull final ItemStack stack, final boolean simulate) { // doesn't respect stack limits!
            final IExAEStack<?> inSlot = AppEngInternalUnivInventory.this.getStackInSlot(slot);
            if (inSlot == null) {
                if (!simulate) {
                    AppEngInternalUnivInventory.this.setStackInSlot(slot, AEItemStack.fromItemStack(stack));
                }
                return ItemStack.EMPTY;
            } else if (inSlot.unwrap() instanceof final IAEItemStack ais && ais.isSameType(stack)) {
                if (!simulate) {
                    AppEngInternalUnivInventory.this.setStackInSlot(slot, ais.copy().setStackSize(ais.getStackSize() + stack.getCount()));
                }
                return ItemStack.EMPTY;
            }
            return stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) { // doesn't respect stack limits!
            final IExAEStack<?> inSlot = AppEngInternalUnivInventory.this.getStackInSlot(slot);
            if (inSlot != null && inSlot.unwrap() instanceof IAEItemStack ais) {
                if (amount >= ais.getStackSize()) {
                    if (!simulate) {
                        AppEngInternalUnivInventory.this.setStackInSlot(slot, (IExAEStack<?>) null);
                    }
                    return ais.createItemStack();
                } else {
                    final ItemStack result = ais.createItemStack();
                    result.setCount(amount);
                    if (!simulate) {
                        AppEngInternalUnivInventory.this.setStackInSlot(slot, ais.copy().setStackSize(ais.getStackSize() - amount));
                    }
                    return result;
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(final int slot) {
            return 64;
        }

    }

}
