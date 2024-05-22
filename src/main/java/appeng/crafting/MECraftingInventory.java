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

package appeng.crafting;


import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEUnivInventory;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.api.storage.data.IUnivItemList;
import appeng.api.util.IUnivStackIterable;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInformPlayer;
import appeng.util.inv.ItemListIgnoreCrafting;
import appeng.util.inv.UnivSubInventoryDelegate;
import net.minecraft.entity.player.EntityPlayerMP;

import java.io.IOException;


public class MECraftingInventory implements IMEUnivInventory {

    private final MECraftingInventory par;

    private final IMEUnivInventory target;
    private final IUnivItemList localCache;

    private final boolean logExtracted;
    private final IUnivItemList extractedCache;

    private final boolean logInjections;
    private final IUnivItemList injectedCache;

    private final boolean logMissing;
    private final IUnivItemList missingCache;

    public MECraftingInventory() {
        this.localCache = AEApi.instance().storage().createUnivList(ItemListIgnoreCrafting.FACTORY);
        this.extractedCache = null;
        this.injectedCache = null;
        this.missingCache = null;
        this.logExtracted = false;
        this.logInjections = false;
        this.logMissing = false;
        this.target = null;
        this.par = null;
    }

    public MECraftingInventory(final MECraftingInventory parent) {
        this.target = parent;
        this.logExtracted = parent.logExtracted;
        this.logInjections = parent.logInjections;
        this.logMissing = parent.logMissing;

        if (this.logMissing) {
            this.missingCache = AEApi.instance().storage().createUnivList();
        } else {
            this.missingCache = null;
        }

        if (this.logExtracted) {
            this.extractedCache = AEApi.instance().storage().createUnivList();
        } else {
            this.extractedCache = null;
        }

        if (this.logInjections) {
            this.injectedCache = AEApi.instance().storage().createUnivList();
        } else {
            this.injectedCache = null;
        }

        this.localCache = this.target.getAvailableItems(AEApi.instance().storage().createUnivList(ItemListIgnoreCrafting.FACTORY));

        this.par = parent;
    }

    public MECraftingInventory(final IMEUnivInventory target, final boolean logExtracted, final boolean logInjections, final boolean logMissing) {
        this.target = target;
        this.logExtracted = logExtracted;
        this.logInjections = logInjections;
        this.logMissing = logMissing;

        if (logMissing) {
            this.missingCache = AEApi.instance().storage().createUnivList();
        } else {
            this.missingCache = null;
        }

        if (logExtracted) {
            this.extractedCache = AEApi.instance().storage().createUnivList();
        } else {
            this.extractedCache = null;
        }

        if (logInjections) {
            this.injectedCache = AEApi.instance().storage().createUnivList();
        } else {
            this.injectedCache = null;
        }

        this.localCache = target.getAvailableItems(AEApi.instance().storage().createUnivList());
        this.par = null;

        /*target.forEach(new IMEUnivInventory.Visitor() {
            @Override
            public <T extends IAEStack<T>> void visit(final IMEInventory<T> inv) {
                if (inv instanceof IMEMonitor<T> mon) {
                    for (T stack : mon.getStorageList()) {
                        MECraftingInventory.this.localCache.add(mon.extractItems(stack, Actionable.SIMULATE, src));
                    }
                }
            }
        });*/
    }

    public MECraftingInventory(final IUnivItemList itemList) {
        this.localCache = AEApi.instance().storage().createUnivList(ItemListIgnoreCrafting.FACTORY);
        this.target = null;
        this.logExtracted = false;
        this.logInjections = false;
        this.logMissing = false;
        this.missingCache = null;
        this.extractedCache = null;
        this.injectedCache = null;

        itemList.onEach(MECraftingInventory.this.localCache::add);

        this.par = null;
    }

    @Override
    public <T extends IAEStack<T>> IMEInventory<T> inventoryFor(final IStorageChannel<T> channel) {
        return new UnivSubInventoryDelegate<>(this, channel);
    }

    @Override
    public <T extends IAEStack<T>> T injectItems(final T input, final Actionable mode, final IActionSource src) {
        if (input == null) {
            return null;
        }

        if (mode == Actionable.MODULATE) {
            if (this.logInjections) {
                this.injectedCache.add(input);
            }
            this.localCache.add(input);
        }

        return null;
    }

    @Override
    public <T extends IAEStack<T>> T extractItems(final T request, final Actionable mode, final IActionSource src) {
        if (request == null) {
            return null;
        }

        final T list = this.localCache.findPrecise(request);
        if (list == null || list.getStackSize() == 0) {
            return null;
        }

        if (list.getStackSize() >= request.getStackSize()) {
            if (mode == Actionable.MODULATE) {
                list.decStackSize(request.getStackSize());
                if (this.logExtracted) {
                    this.extractedCache.add(request);
                }
            }

            return request;
        }

        final T ret = request.copy();
        ret.setStackSize(list.getStackSize());

        if (mode == Actionable.MODULATE) {
            list.reset();
            if (this.logExtracted) {
                this.extractedCache.add(ret);
            }
        }

        return ret;
    }

    @Override
    public IUnivItemList getAvailableItems(final IUnivItemList out) {
        this.localCache.onEach(out::add);
        return out;
    }

    @Override
    public <T extends IAEStack<T>> IItemList<T> getAvailableItems(final IStorageChannel<T> channel, final IItemList<T> out) {
        for (final T is : this.localCache.listFor(channel)) {
            out.add(is);
        }

        return out;
    }

    public IUnivItemList getItemList() {
        return this.localCache;
    }

    public boolean commit(final IActionSource src) {
        final IUnivItemList added = AEApi.instance().storage().createUnivList();
        final IUnivItemList pulled = AEApi.instance().storage().createUnivList();

        if (this.logInjections) {
            if (!this.injectedCache.traverse(new IUnivStackIterable.Traversal() {
                @Override
                public <T extends IAEStack<T>> boolean traverse(final T stack) {
                    final T result = MECraftingInventory.this.target.injectItems(stack, Actionable.MODULATE, src);
                    if (result == null) {
                        return false;
                    }
                    added.add(result);
                    return true;
                }
            })) {
                added.onEach(new IUnivStackIterable.Visitor() {
                    @Override
                    public <T extends IAEStack<T>> void visit(final T stack) {
                        MECraftingInventory.this.target.extractItems(stack, Actionable.MODULATE, src);
                    }
                });
                return false;
            }
        }

        if (this.logExtracted) {
            if (this.extractedCache.foldL(false, new IUnivStackIterable.Accumulator<>() {
                @Override
                public <T extends IAEStack<T>> Boolean accumulate(final Boolean failed, final T stack) {
                    T result = MECraftingInventory.this.target.extractItems(stack, Actionable.MODULATE, src);
                    pulled.add(result);

                    if (result == null || result.getStackSize() != stack.getStackSize()) {
                        if (src.player().isPresent()) {
                            try {
                                if (result == null) {
                                    NetworkHandler.instance().sendTo(new PacketInformPlayer<>(stack, null, PacketInformPlayer.InfoType.NO_ITEMS_EXTRACTED), (EntityPlayerMP) src.player().get());
                                } else {
                                    NetworkHandler.instance().sendTo(new PacketInformPlayer<>(stack, result, PacketInformPlayer.InfoType.PARTIAL_ITEM_EXTRACTION), (EntityPlayerMP) src.player().get());
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return true;
                    }
                    return failed;
                }
            })) {
                added.onEach(new IUnivStackIterable.Visitor() {
                    @Override
                    public <T extends IAEStack<T>> void visit(final T stack) {
                        MECraftingInventory.this.target.extractItems(stack, Actionable.MODULATE, src);
                    }
                });

                pulled.onEach(new IUnivStackIterable.Visitor() {
                    @Override
                    public <T extends IAEStack<T>> void visit(final T stack) {
                        MECraftingInventory.this.target.injectItems(stack, Actionable.MODULATE, src);
                    }
                });

                return false;
            }
        }

        if (this.logMissing && this.par != null) {
            this.missingCache.onEach(MECraftingInventory.this.par::addMissing);
        }

        return true;
    }

    private <T extends IAEStack<T>> void addMissing(final T extra) {
        this.missingCache.add(extra);
    }

    <T extends IAEStack<T>> void ignore(final T what) {
        final T list = this.localCache.findPrecise(what);
        if (list != null) {
            list.setStackSize(0);
        }
    }
}
