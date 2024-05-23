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


import appeng.api.config.Actionable;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;


/**
 * Represents a device that can submit item autocrafting requests and receive the results.
 *
 * @deprecated implement and use {@link IUnivCraftingRequester} instead.
 */
@Deprecated
public interface ICraftingRequester extends IUnivCraftingRequester
{

	/**
	 * items are injected into the requester as they are completed, any items that cannot be taken, or are unwanted can
	 * be returned.
	 *
	 * @param items item
	 * @param mode action mode
	 *
	 * @return unwanted item
	 */
	IAEItemStack injectCraftedItems( ICraftingLink link, IAEItemStack items, Actionable mode );

	@SuppressWarnings("unchecked")
    @Override
	default <T extends IAEStack<T>> T injectCraftedUniv( final ICraftingLink link, final T items, final Actionable mode )
	{
		return items instanceof IAEItemStack ? (T) this.injectCraftedItems(link, (IAEItemStack) items, mode) : items;
	}
}
