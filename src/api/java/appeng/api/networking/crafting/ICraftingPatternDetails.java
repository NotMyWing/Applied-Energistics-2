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

package appeng.api.networking.crafting;


import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IExAEStack;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;


/**
 * do not implement provided by {@link ICraftingPatternItem}
 *
 * caching this INSTANCE will increase performance of validation and checks.
 */
public interface ICraftingPatternDetails
{

	/**
	 * @return source item.
	 */
	ItemStack getPattern();

	/**
	 * @param slotIndex specific slot index
	 * @param itemStack item in slot
	 * @param world crafting world
	 *
	 * @return if an item can be used in the specific slot for this pattern.
	 */
	boolean isValidItemForSlot( int slotIndex, ItemStack itemStack, World world );

	/**
	 * @return if this pattern is a crafting pattern ( work bench )
	 */
	boolean isCraftable();

	/**
	 * @return a list of the inputs, will include nulls.
	 */
	IExAEStack<?>[] getUnivInputs();

	/**
	 * This method is for backwards-compatibility ONLY and will throw an exception if there are any non-item inputs!
	 *
	 * @return a list of the inputs, will include nulls.
	 * @deprecated use {@link #getUnivInputs()} instead.
	 */
	@Deprecated
	default IAEItemStack[] getInputs()
	{
		return coerceExToItemStacks(this.getUnivInputs());
	}

	/**
	 * @return a list of the inputs, will be clean
	 */
	IExAEStack<?>[] getCondensedUnivInputs();

	/**
	 * This method is for backwards-compatibility ONLY and will throw an exception if there are any non-item inputs!
	 *
	 * @return a list of the inputs, will be clean
	 * @deprecated use {@link #getCondensedUnivInputs()} instead.
	 */
	@Deprecated
	default IAEItemStack[] getCondensedInputs()
	{
		return coerceExToItemStacks(this.getCondensedUnivInputs());
	}

	/**
	 * @return a list of the outputs, will include nulls.
	 */
	IExAEStack<?>[] getUnivOutputs();

	/**
	 * This method is for backwards-compatibility ONLY and will throw an exception if there are any non-item inputs!
	 *
	 * @return a list of the outputs, will include nulls.
	 * @deprecated use {@link #getUnivOutputs()} instead.
	 */
	default IAEItemStack[] getOutputs()
	{
		return coerceExToItemStacks(this.getUnivOutputs());
	}

	/**
	 * @return a list of the outputs, will be clean
	 */
	IExAEStack<?>[] getCondensedUnivOutputs();

	/**
	 * This method is for backwards-compatibility ONLY and will throw an exception if there are any non-item inputs!
	 *
	 * @return a list of the outputs, will be clean
	 * @deprecated use {@link #getCondensedUnivOutputs()} instead.
	 */
	default IAEItemStack[] getCondensedOutputs()
	{
		return coerceExToItemStacks(this.getCondensedUnivOutputs());
	}

	/**
	 * Coerces an array of heterogeneous AE stacks to an array of only item stacks, throwing an exception if any entry
	 * is a non-item stack.
	 *
	 * @param ustacks the array to coerce
	 * @return the coerced array
	 * @deprecated this method is only intended for bridging deprecated methods!
	 */
	@Deprecated
	static IAEItemStack[] coerceExToItemStacks( final IExAEStack<?>[] ustacks )
	{
		final IAEItemStack[] stacks = new IAEItemStack[ustacks.length];
		for ( int i = 0; i < ustacks.length; i++ )
		{
			if ( ustacks[i] != null )
			{
				final IAEStack<?> stack = ustacks[i].unwrap();
				if ( !(stack instanceof IAEItemStack) )
				{
					throw new UnsupportedOperationException("Non-item stack " + stack + " in item stack array!");
				}
				stacks[i] = (IAEItemStack) stack;
			}
		}
		return stacks;
	}

	/**
	 * @return if this pattern is enabled to support substitutions.
	 */
	boolean canSubstitute();

	default List<IAEItemStack> getSubstituteInputs( int slot )
	{
		return Collections.emptyList();
	}

	/**
	 * Allow using this INSTANCE of the pattern details to preform the crafting action with performance enhancements.
	 *
	 * @param craftingInv inventory
	 * @param world crafting world
	 *
	 * @return the crafted ( work bench ) item.
	 */
	ItemStack getOutput( InventoryCrafting craftingInv, World world );

	/**
	 * Get the priority of this pattern
	 *
	 * @return the priority of this pattern
	 */
	int getPriority();

	/**
	 * Set the priority the of this pattern.
	 *
	 * @param priority priority of pattern
	 */
	void setPriority( int priority );
}
