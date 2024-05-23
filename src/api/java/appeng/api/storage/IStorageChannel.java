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

package appeng.api.storage;


import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;


public interface IStorageChannel<T extends IAEStack<T>>
{

	/**
	 * @return the channel type for this channel
	 */
	Class<? extends IStorageChannel<?>> getChannelType();

	/**
	 * Can be used as factor for transferring stacks of a channel.
	 * 
	 * E.g. used by IO Ports to transfer 1000 mB, not 1 mB to match the
	 * item channel transferring a full bucket per operation.
	 * 
	 * @return
	 */
	default int transferFactor()
	{
		return 1;
	}

	/**
	 * The number of units (eg item count, or millibuckets) that can be stored per byte in a storage cell.
	 * Standard value for items is 8, and for fluids it's 8000
	 *
	 * @return number of units
	 */
	default int getUnitsPerByte()
	{
		return 8;
	}

	/**
	 * Create a new {@link IItemList} of the specific type.
	 * 
	 * @return
	 */
	@Nonnull
	IItemList<T> createList();

	/**
	 * Create a new {@link IAEStack} subtype of the specific object.
	 * 
	 * The parameter is unbound to allow a slightly more flexible approach.
	 * But the general intention is about converting an {@link ItemStack} or {@link FluidStack} into the corresponding
	 * {@link IAEStack}.
	 * Another valid case might be to use it instead of {@link IAEStack#copy()}, but this might not be supported by all
	 * types.
	 * IAEStacks that use custom items for {@link IAEStack#asItemStackRepresentation()} must also be able to convert
	 * these.
	 * 
	 * @param input The object to turn into an {@link IAEStack}
	 * @return The converted stack or null
	 */
	@Nullable
	T createStack( @Nonnull Object input );

	/**
	 * Gets the underlying stack type of AE stacks for this storage channel. For example,
	 * {@link appeng.api.storage.channels.IItemStorageChannel} has an underlying stack type of {@link ItemStack} and
	 * {@link appeng.api.storage.channels.IFluidStorageChannel} has an underlying stack type of {@link FluidStack}.
	 *
	 * @return the underlying stack type
	 */
	Class<?> getUnderlyingStackType();

	/**
	 * Coerce a stack to an instance of the underlying stack type given by {@link #getUnderlyingStackType()}.
	 *
	 * @param stack the stack to convert
	 * @return the converted stack, or null if no underlying representation was possible
	 */
	@Nullable
	Object unwrapStack( T stack );

	/**
	 * Tries to wrap some kind of object in an AE inventory. The given capability provider should be used if the
	 * inventory type is capability-based (e.g. a Forge item handler). Otherwise, the inventory object can be used if,
	 * for example, the inventory type is based on an implemented interface.
	 *
	 * @param inventory the object which may have an inventory (e.g. a tile entity)
	 * @param caps      a capability provider which may provide the inventory
	 * @param side      the side of the object which is being interacted with
	 * @return the wrapped inventory, or null if no valid inventory could be wrapped
	 */
	@Nullable
	IMEInventory<T> adaptInventory( @Nonnull Object inventory, @Nullable ICapabilityProvider caps, @Nullable EnumFacing side );

	/**
	 * 
	 * @param input
	 * @return
	 * @throws IOException
	 */
	@Nullable
	T readFromPacket( @Nonnull ByteBuf input ) throws IOException;

	/**
	 * create from nbt data
	 * 
	 * @param nbt
	 * @return
	 */
	@Nullable
	T createFromNBT( @Nonnull NBTTagCompound nbt );
}
