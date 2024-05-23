/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.implementations.tiles;


import appeng.api.AEApi;
import appeng.api.networking.crafting.ICraftingInventory;
import appeng.api.storage.channels.IItemStorageChannel;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import appeng.api.networking.crafting.ICraftingPatternDetails;


/**
 * A machine capable of receiving item autocrafting jobs from an interface.
 *
 * @deprecated implement and use {@link IUnivCraftingMachine} instead.
 */
@Deprecated
public interface ICraftingMachine extends IUnivCraftingMachine
{

	/**
	 * inserts a crafting plan, and the necessary items into the crafting machine.
	 *
	 * @param patternDetails details of pattern
	 * @param table crafting table
	 * @param ejectionDirection ejection direction
	 *
	 * @return if it was accepted, all or nothing.
	 */
	boolean pushPattern( ICraftingPatternDetails patternDetails, InventoryCrafting table, EnumFacing ejectionDirection );

	@Override
	default boolean pushPattern( final ICraftingPatternDetails patternDetails, final ICraftingInventory table, final EnumFacing ejectionDirection )
	{
		final InventoryCrafting ic = AEApi.instance().deprecation().createFakeCraftingInventory(table.getWidth(), table.getHeight());
		if ( !this.pushPattern(patternDetails, ic, ejectionDirection) )
		{
			return false;
		}

		final IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
		for ( int i = 0; i < ic.getSizeInventory(); i++ )
		{
			final ItemStack stack = ic.getStackInSlot(i);
			if ( !stack.isEmpty() )
			{
				table.setStackInSlot(i, channel.createStack(stack));
			}
		}
		return true;
	}
}
