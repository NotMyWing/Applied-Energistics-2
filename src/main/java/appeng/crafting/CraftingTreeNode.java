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
import appeng.api.config.FuzzyMode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IUnivItemList;
import appeng.api.util.IExAEStack;
import appeng.api.util.IUnivStackIterable;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInformPlayer;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import appeng.util.item.ExAEStack;
import appeng.util.item.MeaningfulItemIterator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CraftingTreeNode<T extends IAEStack<T>> {

    // what slot!
    private final int slot;
    private final CraftingJob<?> job;
    private final IUnivItemList used = AEApi.instance().storage().createUnivList();
    // parent node.
    private final CraftingTreeProcess<?> parent;
    private final World world;
    // what item is this?
    private final T what;
    // what are the crafting patterns for this?
    private final ArrayList<CraftingTreeProcess<T>> nodes = new ArrayList<>();
    private final ICraftingGrid cc;
    private final int depth;
    private long bytes = 0;
    private boolean canEmit = false;
    private long missing = 0;
    private long howManyEmitted = 0;
    private boolean exhausted = false;

    public CraftingTreeNode(final ICraftingGrid cc, final CraftingJob<?> job, final T wat, final CraftingTreeProcess<?> par, final int slot, final int depth) {
        this.what = wat;
        this.parent = par;
        this.slot = slot;
        this.world = job.getWorld();
        this.job = job;
        this.cc = cc;
        this.depth = depth;

        this.canEmit = cc.canEmitFor(this.what);
    }

    public void addNode() {
        if (!nodes.isEmpty()) {
            return;
        }

        if (this.canEmit) {
            return; // if you can emit for something, you can't make it with patterns.
        }

        for (final ICraftingPatternDetails details : cc.getCraftingFor(this.what, this.parent == null ? null : this.parent.details, slot, this.world))// in
        // order.
        {
            if (this.parent == null || notRecursive(details) && this.parent.details != details) {
                this.nodes.add(new CraftingTreeProcess<>(cc, job, details, this, depth + 1));
            }
        }
    }

    T request(final MECraftingInventory inv, long l, final IActionSource src) throws CraftBranchFailure, InterruptedException {
        addNode();
        this.job.handlePausing();

        final IUnivItemList inventoryList = inv.getItemList();
        final List<IExAEStack<?>> thingsUsed = new ArrayList<>();

        this.what.setStackSize(l);

        if (this.getSlot() >= 0 && this.parent != null && this.parent.details.isCraftable()) {
            final IAEItemStack ais = (IAEItemStack) this.what; // *should* be safe
            LinkedList<IAEItemStack> itemList = new LinkedList<>();

            boolean damageableItem = ais.getItem().isDamageable() || Platform.isGTDamageableItem(ais.getItem());

            if (this.parent.details.canSubstitute()) {
                for (IAEItemStack subs : this.parent.details.getSubstituteInputs(this.slot)) {
                    if (damageableItem) {
                        Iterator<IAEItemStack> it = new MeaningfulItemIterator<>(inventoryList.findFuzzy(ais, FuzzyMode.IGNORE_ALL));
                        while (it.hasNext()) {
                            IAEItemStack i = it.next();
                            if (i.getStackSize() > 0) {
                                itemList.add(i);
                            }
                        }
                    }
                    subs = inventoryList.findPrecise(subs);
                    if (subs != null && subs.getStackSize() > 0) {
                        itemList.add(subs);
                    }
                }
            } else {
                if (damageableItem) {
                    Iterator<IAEItemStack> it = new MeaningfulItemIterator<>(inventoryList.findFuzzy(ais, FuzzyMode.IGNORE_ALL));
                    while (it.hasNext()) {
                        IAEItemStack i = it.next();
                        if (i.getStackSize() > 0) {
                            itemList.add(i);
                        }
                    }
                } else {
                    final IAEItemStack item = inventoryList.findPrecise(ais);
                    if (item != null && item.getStackSize() > 0) {
                        itemList.add(item);
                    }
                }
            }

            for (IAEItemStack fuzz : itemList) {
                if (this.parent.details.isValidItemForSlot(this.getSlot(), fuzz.getDefinition(), this.world)) {
                    fuzz = fuzz.copy();
                    fuzz.setStackSize(l);

                    final IAEItemStack available = inv.extractItems(fuzz, Actionable.MODULATE, src);

                    if (available != null) {
                        if (available.getItem().hasContainerItem(available.getDefinition())) {
                            final ItemStack is2 = Platform.getContainerItem(available.createItemStack());
                            final IAEItemStack o = AEItemStack.fromItemStack(is2);

                            if (o != null) {
                                this.parent.addContainers(o);
                            }
                        }

                        if (!this.exhausted) {
                            final IAEItemStack is = this.job.checkUse(available);

                            if (is != null) {
                                thingsUsed.add(ExAEStack.of(is.copy()));
                                this.used.add(is);
                            }
                        }

                        addBytesFor(available);
                        l -= available.getStackSize();

                        if (l == 0) {
                            return (T) available; // the cast earlier forces T = IAEItemStack
                        }
                    }
                }
            }
        } else {
            final T available = inv.extractItems(this.what, Actionable.MODULATE, src);

            if (available != null) {
                if (!this.exhausted) {
                    final T is = this.job.checkUse(available);

                    if (is != null) {
                        thingsUsed.add(ExAEStack.of(is.copy()));
                        this.used.add(is);
                    }
                }

                addBytesFor(available);
                l -= available.getStackSize();

                if (l == 0) {
                    return available;
                }
            }
        }

        if (this.canEmit) {
            final T wat = this.what.copy();
            wat.setStackSize(l);

            this.howManyEmitted = wat.getStackSize();
            addBytesFor(wat);

            return wat;
        }

        this.exhausted = true;

        if (this.nodes.size() == 1) {
            final CraftingTreeProcess<T> pro = this.nodes.get(0);

            while (pro.possible && l > 0) {
                final T madeWhat = pro.getAmountCrafted(this.what);
                pro.request(inv, pro.getTimes(l, madeWhat.getStackSize()), src);

                madeWhat.setStackSize(l);
                final T available = inv.extractItems(madeWhat, Actionable.MODULATE, src);

                if (available != null) {

                    if (parent != null && available instanceof final IAEItemStack ais && ais.getItem().hasContainerItem(ais.getDefinition())) {
                        final ItemStack is2 = Platform.getContainerItem(ais.createItemStack());
                        final IAEItemStack o = AEItemStack.fromItemStack(is2);

                        if (o != null) {
                            this.parent.addContainers(o);
                        }
                    }

                    addBytesFor(available);
                    l -= available.getStackSize();

                    if (l <= 0) {
                        return available;
                    }
                } else {
                    pro.possible = false; // ;P
                }
            }
        } else if (this.nodes.size() > 1) {
            for (final CraftingTreeProcess<T> pro : this.nodes) {
                try {
                    while (pro.possible && l > 0) {
                        final MECraftingInventory subInv = new MECraftingInventory(inv, true, true, true);
                        pro.request(subInv, 1, src);

                        this.what.setStackSize(l);
                        final T available = subInv.extractItems(this.what, Actionable.MODULATE, src);

                        if (available != null) {
                            if (!subInv.commit(src)) {
                                throw new CraftBranchFailure(ExAEStack.of(this.what), l);
                            }

                            addBytesFor(available);
                            l -= available.getStackSize();

                            if (l <= 0) {
                                return available;
                            }
                        } else {
                            pro.possible = false; // ;P
                        }
                    }
                } catch (final CraftBranchFailure fail) {
                    pro.possible = true;
                }
            }
        }

        if (job.isSimulation()) {
            this.bytes += (long) Math.ceil(l / (double) what.getChannel().transferFactor());
            if (parent != null && this.what instanceof final IAEItemStack ais && ais.getItem().hasContainerItem(ais.getDefinition())) {
                final ItemStack is2 = Platform.getContainerItem(ais.copy().setStackSize(1).createItemStack());
                final IAEItemStack o = AEItemStack.fromItemStack(is2);

                if (o != null) {
                    this.parent.addContainers(o);
                }
            }
            this.missing += l;
            final T rv = this.what.copy();
            rv.setStackSize(l);
            return rv;
        }

        IExAEStack.onEach(thingsUsed, new IUnivStackIterable.Visitor() {
            @Override
            public <U extends IAEStack<U>> void visit(final U stack) {
                CraftingTreeNode.this.job.refund(stack.copy());
                stack.setStackSize(-stack.getStackSize());
                CraftingTreeNode.this.used.add(stack);
            }
        });

        throw new CraftBranchFailure(ExAEStack.of(this.what), l);
    }

    private void addBytesFor(final IAEStack<?> stack) {
        this.bytes += (long) Math.ceil(stack.getStackSize() / (double) stack.getChannel().transferFactor());
    }

    boolean notRecursive(ICraftingPatternDetails details) {
        if (this.parent == null) {
            return true;
        }
        if (this.parent.details == details) {
            return false;
        }
        return this.parent.notRecursive(details);
    }

    void dive(final CraftingJob<?> job) {
        if (this.missing > 0) {
            job.addMissing(this.getStack(this.missing));
        }
        // missing = 0;

        job.addBytes(this.bytes);

        for (final CraftingTreeProcess<T> pro : this.nodes) {
            pro.dive(job);
        }
    }

    T getStack(final long size) {
        final T is = this.what.copy();
        is.setStackSize(size);
        return is;
    }

    void setSimulate() {
        this.missing = 0;
        this.bytes = 0;
        this.used.resetStatus();
        this.exhausted = false;

        for (final CraftingTreeProcess<T> pro : this.nodes) {
            pro.setSimulate();
        }
    }

    public void setJob(final MECraftingInventory storage, final CraftingCPUCluster craftingCPUCluster, final IActionSource src) throws CraftBranchFailure {
        for (final IExAEStack<?> i : this.used) {
            extractUsed(storage, craftingCPUCluster, src, i);
        }

        if (this.howManyEmitted > 0) {
            craftingCPUCluster.addEmitable(this.what.copy().reset().setStackSize(this.howManyEmitted));
        }

        for (final CraftingTreeProcess<T> pro : this.nodes) {
            pro.setJob(storage, craftingCPUCluster, src);
        }
    }

    private static <T extends IAEStack<T>> void extractUsed(final MECraftingInventory storage, final CraftingCPUCluster craftingCPUCluster, final IActionSource src, final IExAEStack<T> input) throws CraftBranchFailure {
        final T i = input.unwrap();
        final T actuallyExtracted = storage.extractItems(i, Actionable.MODULATE, src);

        if (actuallyExtracted == null || actuallyExtracted.getStackSize() != i.getStackSize()) {
            if (src.player().isPresent()) {
                try {
                    if (actuallyExtracted == null) {
                        NetworkHandler.instance().sendTo(new PacketInformPlayer<>(i, null, PacketInformPlayer.InfoType.NO_ITEMS_EXTRACTED), (EntityPlayerMP) src.player().get());
                    } else {
                        NetworkHandler.instance().sendTo(new PacketInformPlayer<>(i, actuallyExtracted, PacketInformPlayer.InfoType.PARTIAL_ITEM_EXTRACTION), (EntityPlayerMP) src.player().get());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            throw new CraftBranchFailure(input, i.getStackSize());
        }

        craftingCPUCluster.addStorage(actuallyExtracted);
    }

    void getPlan(final IUnivItemList plan) {
        if (this.missing > 0) {
            plan.add(this.what.copy().setStackSize(this.missing));
        }

        if (this.howManyEmitted > 0) {
            plan.addRequestable(this.what.copy().setCountRequestable(this.howManyEmitted));
        }

        this.used.onEach(new IUnivStackIterable.Visitor() {
            @Override
            public <T extends IAEStack<T>> void visit(final T stack) {
                plan.add(stack.copy());
            }
        });

        for (final CraftingTreeProcess<T> pro : this.nodes) {
            pro.getPlan(plan);
        }
    }

    int getSlot() {
        return this.slot;
    }
}
