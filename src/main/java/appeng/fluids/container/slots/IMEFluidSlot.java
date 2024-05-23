/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2018, AlgorithmX2, All rights reserved.
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

package appeng.fluids.container.slots;


import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.util.IExAEStack;
import appeng.container.slot.IMESlot;
import appeng.util.item.ExAEStack;


/**
 * @author yueh
 * @version rv6
 * @since rv6
 */
public interface IMEFluidSlot extends IMESlot<IAEFluidStack> {
    IAEFluidStack getAEFluidStack();

    @Override
    default IAEFluidStack getAEStack() {
        return getAEFluidStack();
    }

    @Override
    default IStorageChannel<IAEFluidStack> getSlotChannel() {
        return AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
    }

    default boolean shouldRenderAsFluid() {
        return true;
    }
}
