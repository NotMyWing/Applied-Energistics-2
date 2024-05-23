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


import appeng.api.AEApi;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.storage.data.IUnivItemList;
import appeng.api.util.IExAEStack;


public interface ICraftingJob
{

	/**
	 * @return if this job is a simulation, simulations cannot be submitted and only represent 1 possible future
	 * crafting job with fake items.
	 */
	boolean isSimulation();

	/**
	 * @return total number of bytes to process this job.
	 */
	long getByteTotal();

	/**
	 * Populates the plan list with stack size, and requestable values that represent the stored, and crafting job
	 * contents respectively. This overload is for backwards-compatibility ONLY and will throw an exception if the
	 * crafting job contains any non-item ingredients!
	 *
	 * @param plan plan
	 * @deprecated use {@link #populatePlan(IUnivItemList)} instead.
	 */
	@Deprecated
	default void populatePlan( final IItemList<IAEItemStack> plan )
	{
		this.populatePlan(AEApi.instance().deprecation().wrapAsUniv(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class), plan));
	}

	/**
	 * Populates the plan list with stack size, and requestable values that represent the stored, and crafting job
	 * contents respectively.
	 *
	 * @param plan plan
	 */
	void populatePlan( IUnivItemList plan );

	/**
	 * This overload is for backwards-compatibility ONLY and will throw an exception if the output is not an item!
	 *
	 * @return the final output of the job
	 * @deprecated use {@link #getUnivOutput()} instead.
	 */
	@Deprecated
	default IAEItemStack getOutput()
	{
		final IExAEStack<?> stack = getUnivOutput();
		return stack != null ? AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(stack.asItemStackRepresentation()) : null;
	}

	/**
	 * @return the final output of the job.
	 */
	IExAEStack<?> getUnivOutput();
}
