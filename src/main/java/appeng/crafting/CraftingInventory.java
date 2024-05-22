package appeng.crafting;


import appeng.api.networking.crafting.ICraftingInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IExAEStack;
import appeng.container.ContainerNull;
import appeng.util.item.ExAEStack;
import net.minecraft.inventory.InventoryCrafting;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class CraftingInventory implements ICraftingInventory {

    private final int inventoryWidth, inventoryHeight;
    private final List<IExAEStack<?>> stacks;

    public CraftingInventory(final int inventoryWidth, final int inventoryHeight) {
        this.inventoryWidth = inventoryWidth;
        this.inventoryHeight = inventoryHeight;
        this.stacks = Arrays.asList(new IExAEStack<?>[inventoryWidth * inventoryHeight]);
    }

    @Override
    public int getWidth() {
        return this.inventoryWidth;
    }

    @Override
    public int getHeight() {
        return this.inventoryHeight;
    }

    @Override
    public int getSlotCount() {
        return this.stacks.size();
    }

    @Override
    public <T extends IAEStack<T>> void setStackInSlot(final int slotIndex, final T stack) {
        this.setStackInSlot(slotIndex, ExAEStack.of(stack));
    }

    @Override
    public <T extends IAEStack<T>> void setStackInSlot(final int slotIndex, final IExAEStack<T> stack) {
        this.stacks.set(slotIndex, stack);
    }

    @Override
    public IExAEStack<?> getStackInSlot(final int slotIndex) {
        return this.stacks.get(slotIndex);
    }

    @Override
    public void onEach(final Visitor visitor) {
        IExAEStack.onEach(this.stacks, visitor);
    }

    @Override
    public boolean traverse(final Traversal traversal) {
        return IExAEStack.traverse(this.stacks, traversal);
    }

    @Override
    public <A> A foldL(final A acc, final Accumulator<A> accumulator) {
        return IExAEStack.foldL(this.stacks, acc, accumulator);
    }

    @Nonnull
    @Override
    public Iterator<IExAEStack<?>> iterator() {
        return this.stacks.iterator();
    }

    @Override
    public InventoryCrafting asVanilla() {
        final InventoryCrafting ic = new InventoryCrafting(new ContainerNull(), this.inventoryWidth, this.inventoryHeight);
        for (int i = 0; i < this.getSlotCount(); i++) {
            IExAEStack<?> stack = this.getStackInSlot(i);
            if (stack != null) {
                if (stack.unwrap() instanceof IAEItemStack is) {
                    ic.setInventorySlotContents(i, is.createItemStack());
                } else {
                    return null;
                }
            }
        }
        return ic;
    }

}
