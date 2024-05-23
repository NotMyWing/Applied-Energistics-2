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


import java.util.concurrent.Future;

import appeng.api.storage.data.IAEStack;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;

import net.minecraft.world.World;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridCache;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;


public interface ICraftingGrid extends IGridCache
{

	/**
	 * @param whatToCraft requested craft
	 * @param world crafting world
	 * @param slot slot index
	 * @param details pattern details
	 *
	 * @return a collection of crafting patterns for the item in question.
	 * @deprecated use {@link #getUnivCraftingFor(IAEStack, ICraftingPatternDetails, int, World)} instead.
	 */
	@Deprecated
	default ImmutableCollection<ICraftingPatternDetails> getCraftingFor( final IAEItemStack whatToCraft, final ICraftingPatternDetails details, final int slot, final World world )
	{
		return this.getUnivCraftingFor(whatToCraft, details, slot, world);
	}

	/**
	 * @param whatToCraft requested craft
	 * @param world crafting world
	 * @param slot slot index
	 * @param details pattern details
	 *
	 * @return a collection of crafting patterns for the item in question.
	 */
	<T extends IAEStack<T>> ImmutableCollection<ICraftingPatternDetails> getUnivCraftingFor( T whatToCraft, ICraftingPatternDetails details, int slot, World world );


	/**
	 * Begin calculating a crafting job.
	 *
	 * @param world crafting world
	 * @param grid network
	 * @param actionSrc source
	 * @param craftWhat result
	 * @param callback callback
	 * -- optional
	 *
	 * @return a future which will at an undetermined point in the future get you the {@link ICraftingJob} do not wait
	 * on this, your be waiting forever.
	 * @deprecated use {@link #beginUnivCraftingJob(World, IGrid, IActionSource, IAEStack, ICraftingCallback)} instead.
	 */
	@Deprecated
	default Future<ICraftingJob> beginCraftingJob( final World world, final IGrid grid, final IActionSource actionSrc, final IAEItemStack craftWhat, final ICraftingCallback callback )
	{
		return this.beginUnivCraftingJob(world, grid, actionSrc, craftWhat, callback);
	}

	/**
	 * Begin calculating a crafting job.
	 *
	 * @param world crafting world
	 * @param grid network
	 * @param actionSrc source
	 * @param craftWhat result
	 * @param callback callback
	 * -- optional
	 *
	 * @return a future which will at an undetermined point in the future get you the {@link ICraftingJob} do not wait
	 * on this, your be waiting forever.
	 */
	<T extends IAEStack<T>> Future<ICraftingJob> beginUnivCraftingJob( World world, IGrid grid, IActionSource actionSrc, T craftWhat, ICraftingCallback callback );

	/**
	 * Submit the job to the Crafting system for processing.
	 *
	 * @param job - the crafting job from beginCraftingJob
	 * @param requestingMachine - a machine if its being requested via automation, may be null.
	 * @param target - can be null
	 * @param prioritizePower - if cpu is null, this determine if the system should prioritize power, or if it should
	 * find the lower
	 * end cpus, automatic processes generally should pick lower end cpus.
	 * @param src - the action source to use when starting the job, this will be used for extracting items, should
	 * usually be the same as the one provided to beginCraftingJob.
	 *
	 * @return null ( if failed ) or an {@link ICraftingLink} other wise, if you send requestingMachine you need to
	 * properly keep track of this and handle the nbt saving and loading of the object as well as the
	 * {@link IUnivCraftingRequester} methods. if you send null, this object should be discarded after verifying the
	 * return state.
	 */
	ICraftingLink submitJob( ICraftingJob job, IUnivCraftingRequester requestingMachine, ICraftingCPU target, boolean prioritizePower, IActionSource src );

	/**
	 * @return list of all the crafting cpus on the grid
	 */
	ImmutableSet<ICraftingCPU> getCpus();


	/**
	 * @param what to be requested item
	 *
	 * @return true if the item can be requested via a crafting emitter.
	 * @deprecated use {@link #canEmitForUniv(IAEStack)} instead.
	 */
	@Deprecated
	default boolean canEmitFor( final IAEItemStack what )
	{
		return this.canEmitForUniv(what);
	}

	/**
	 * @param what to be requested item
	 *
	 * @return true if the item can be requested via a crafting emitter.
	 */
	<T extends IAEStack<T>> boolean canEmitForUniv( T what );

	/**
	 * is this item being crafted?
	 *
	 * @param what item being crafted
	 *
	 * @return true if it is being crafting
	 * @deprecated use {@link #isRequestingUniv(IAEStack)} instead.
	 */
	@Deprecated
	default boolean isRequesting( final IAEItemStack what )
	{
		return this.isRequestingUniv(what);
	}

	/**
	 * is this item being crafted?
	 *
	 * @param what item being crafted
	 *
	 * @return true if it is being crafting
	 */
	<T extends IAEStack<T>> boolean isRequestingUniv( T what );

	/**
	 * The total amount being requested across all crafting cpus of a grid.
	 *
	 * @param what item being requested, ignores stacksize
	 *
	 * @return The total amount being requested.
	 * @deprecated use {@link #requestingUniv(IAEStack)} instead.
	 */
	@Deprecated
	default long requesting( final IAEItemStack what )
	{
		return this.requestingUniv(what);
	}

	/**
	 * The total amount being requested across all crafting cpus of a grid.
	 *
	 * @param what item being requested, ignores stacksize
	 *
	 * @return The total amount being requested.
	 */
	<T extends IAEStack<T>> long requestingUniv( T what );
}
