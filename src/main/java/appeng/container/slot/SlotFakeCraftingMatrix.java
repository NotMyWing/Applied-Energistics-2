/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.container.slot;


import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IExAEStack;
import appeng.tile.inventory.AppEngInternalUnivInventory;
import net.minecraft.item.ItemStack;


public class SlotFakeCraftingMatrix extends SlotFake {

    private final AppEngInternalUnivInventory inv;

    public SlotFakeCraftingMatrix(final AppEngInternalUnivInventory inv, final int idx, final int x, final int y) {
        super(inv.asItemHandler(), idx, x, y);
        this.inv = inv;
    }

    @Override
    public int getSlotStackLimit() {
        return Integer.MAX_VALUE;
    }

    @Override
    public ItemStack getDisplayStack() {
        final IExAEStack<?> stack = this.inv.getStackInSlot(this.getSlotIndex());
        if (stack == null) {
            return ItemStack.EMPTY;
        }

        final ItemStack displayStack = stack.asItemStackRepresentation();
        displayStack.setCount((int) Math.min(stack.getStackSize(), Integer.MAX_VALUE));
        return displayStack;
    }

    @Override
    public long getDisplayStackSize() {
        final IExAEStack<?> stack = this.inv.getStackInSlot(this.getSlotIndex());
        if (stack == null || !(stack.unwrap() instanceof IAEItemStack ais)) {
            return 0L;
        }
        final ItemStack reprStack = ais.createItemStack();

        for (final IStorageChannel<? extends IAEStack<?>> channel : AEApi.instance().storage().storageChannels()) {
            if (channel instanceof IItemStorageChannel) {
                continue;
            }
            final IAEStack<?> realStack = channel.createStack(reprStack);
            if (realStack != null) {
                return realStack.getStackSize();
            }
        }
        return 0L;
    }

}
