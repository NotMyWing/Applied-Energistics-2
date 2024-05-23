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

package appeng.core.api;


import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.IUnivCraftingRequester;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.IStorageHelper;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.IExAEStack;
import appeng.api.util.IItemListFactory;
import appeng.api.storage.data.IUnivItemList;
import appeng.crafting.CraftingLink;
import appeng.fluids.items.FluidDummyItem;
import appeng.fluids.util.AEFluidStack;
import appeng.fluids.util.FluidList;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.AdaptorFluidHandler;
import appeng.util.item.ExAEStack;
import appeng.util.item.UnivItemList;
import appeng.util.item.AEItemStack;
import appeng.util.item.ItemList;
import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;


public class ApiStorage implements IStorageHelper {

    private final ClassToInstanceMap<IStorageChannel<?>> channels;
    private final List<IStorageChannel<?>> channelList = new ArrayList<>(); // for insertion ordering
    private IStorageChannel<?>[] idToChannel = null;
    private Object2ByteMap<IStorageChannel<?>> channelToId = null;

    public ApiStorage() {
        this.channels = MutableClassToInstanceMap.create();
        this.registerStorageChannel(IItemStorageChannel.class, new ItemStorageChannel());
        this.registerStorageChannel(IFluidStorageChannel.class, new FluidStorageChannel());
    }

    @Override
    public <T extends IAEStack<T>, C extends IStorageChannel<T>> void registerStorageChannel(Class<C> channel, C factory) {
        if (this.idToChannel != null) {
            throw new IllegalStateException("Storage channel registry is frozen!");
        }

        Preconditions.checkNotNull(channel);
        Preconditions.checkNotNull(factory);
        Preconditions.checkArgument(channel.isInstance(factory));
        Preconditions.checkArgument(!this.channels.containsKey(channel));

        this.channels.putInstance(channel, factory);
        this.channelList.add(factory);
    }

    @Override
    public <T extends IAEStack<T>, C extends IStorageChannel<T>> C getStorageChannel(Class<C> channel) {
        Preconditions.checkNotNull(channel);

        final C type = this.channels.getInstance(channel);

        Preconditions.checkNotNull(type);

        return type;
    }

    public void freeze() {
        this.idToChannel = this.channels.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().getCanonicalName()))
                .map(Map.Entry::getValue)
                .toArray(IStorageChannel[]::new);

        this.channelToId = new Object2ByteOpenHashMap<>(); // might be faster to use an array map?
        for (int i = 0; i < this.idToChannel.length; i++) {
            channelToId.put(idToChannel[i], (byte) i);
        }
    }

    public byte getStorageChannelId(final IStorageChannel<?> channel) {
        return this.channelToId.getByte(channel);
    }

    public IStorageChannel<?> getStorageChannelById(final byte id) {
        if (id < 0 || id >= this.idToChannel.length) {
            throw new IllegalArgumentException("Channel ID out of bounds: " + id);
        }
        return this.idToChannel[id];
    }

    @Override
    public Collection<IStorageChannel<? extends IAEStack<?>>> storageChannels() {
        return Collections.unmodifiableCollection(this.channelList);
    }

    @Override
    public ICraftingLink loadCraftingLink(final NBTTagCompound data, final IUnivCraftingRequester req) {
        Preconditions.checkNotNull(data);
        Preconditions.checkNotNull(req);

        return new CraftingLink(data, req);
    }

    @Override
    public IUnivItemList createUnivList(final IItemListFactory subListFactory) {
        Preconditions.checkNotNull(subListFactory);

        return new UnivItemList(subListFactory);
    }

    @Override
    public <T extends IAEStack<T>> IExAEStack<T> createExStack(final T stack) {
        return ExAEStack.of(stack);
    }

    @Override
    public IExAEStack<?> readExStackFromPacket(@Nonnull final ByteBuf input) throws IOException {
        return ExAEStack.fromPacket(input);
    }

    @Override
    public IExAEStack<?> createExStackFromNBT(final NBTTagCompound nbt) {
        return ExAEStack.fromNBT(nbt);
    }

    @Override
    public <T extends IAEStack<T>> T poweredInsert(IEnergySource energy, IMEInventory<T> inv, T input, IActionSource src, Actionable mode) {
        return Platform.poweredInsert(energy, inv, input, src, mode);
    }

    @Override
    public <T extends IAEStack<T>> T poweredExtraction(IEnergySource energy, IMEInventory<T> inv, T request, IActionSource src, Actionable mode) {
        return Platform.poweredExtraction(energy, inv, request, src, mode);
    }

    @Override
    public void postChanges(IStorageGrid gs, ItemStack removedCell, ItemStack addedCell, IActionSource src) {
        Preconditions.checkNotNull(gs);
        Preconditions.checkNotNull(removedCell);
        Preconditions.checkNotNull(addedCell);
        Preconditions.checkNotNull(src);

        Platform.postChanges(gs, removedCell, addedCell, src);
    }

    private static final class ItemStorageChannel implements IItemStorageChannel {

        @Override
        public Class<? extends IStorageChannel<?>> getChannelType() {
            return IItemStorageChannel.class;
        }

        @Override
        public IItemList<IAEItemStack> createList() {
            return new ItemList();
        }

        @Override
        public IAEItemStack createStack(Object input) {
            Preconditions.checkNotNull(input);

            if (input instanceof ItemStack) {
                return AEItemStack.fromItemStack((ItemStack) input);
            }

            return null;
        }

        @Override
        public Class<?> getUnderlyingStackType() {
            return ItemStack.class;
        }

        @Nullable
        @Override
        public ItemStack unwrapStack(final IAEItemStack stack) {
            return stack.createItemStack();
        }

        @Nullable
        @Override
        public IMEInventory<IAEItemStack> adaptInventory(@Nonnull final Object inventory, @Nullable final ICapabilityProvider caps, @Nullable final EnumFacing side) {
            return InventoryAdaptor.getAdaptor(caps, side);
        }

        @Override
        public IAEItemStack createFromNBT(NBTTagCompound nbt) {
            Preconditions.checkNotNull(nbt);
            return AEItemStack.fromNBT(nbt);
        }

        @Override
        public IAEItemStack readFromPacket(ByteBuf input) throws IOException {
            Preconditions.checkNotNull(input);

            return AEItemStack.fromPacket(input);
        }
    }

    private static final class FluidStorageChannel implements IFluidStorageChannel {

        @Override
        public Class<? extends IStorageChannel<?>> getChannelType() {
            return IFluidStorageChannel.class;
        }

        @Override
        public int transferFactor() {
            return 1000;
        }

        @Override
        public int getUnitsPerByte() {
            return 8000;
        }

        @Override
        public IItemList<IAEFluidStack> createList() {
            return new FluidList();
        }

        @Override
        public IAEFluidStack createStack(Object input) {
            Preconditions.checkNotNull(input);

            if (input instanceof FluidStack) {
                return AEFluidStack.fromFluidStack((FluidStack) input);
            }
            if (input instanceof ItemStack) {
                final ItemStack is = (ItemStack) input;
                if (is.getItem() instanceof FluidDummyItem) {
                    return AEFluidStack.fromFluidStack(((FluidDummyItem) is.getItem()).getFluidStack(is));
                } else {
                    return AEFluidStack.fromFluidStack(FluidUtil.getFluidContained(is));
                }
            }

            return null;
        }

        @Override
        public Class<?> getUnderlyingStackType() {
            return FluidStack.class;
        }

        @Nullable
        @Override
        public FluidStack unwrapStack(final IAEFluidStack stack) {
            return stack.getFluidStack();
        }

        @Nullable
        @Override
        public IMEInventory<IAEFluidStack> adaptInventory(@Nonnull final Object inventory, @Nullable final ICapabilityProvider caps, @Nullable final EnumFacing side) {
            if (caps.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side)) {
                final IFluidHandler fluidHandler = caps.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);
                if (fluidHandler != null) {
                    return new AdaptorFluidHandler(fluidHandler);
                }
            }
            return null;
        }

        @Override
        public IAEFluidStack readFromPacket(ByteBuf input) throws IOException {
            Preconditions.checkNotNull(input);

            return AEFluidStack.fromPacket(input);
        }

        @Override
        public IAEFluidStack createFromNBT(NBTTagCompound nbt) {
            Preconditions.checkNotNull(nbt);
            return AEFluidStack.fromNBT(nbt);
        }
    }

}
