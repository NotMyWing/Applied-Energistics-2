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

package appeng.core.sync.packets;


import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IExAEStack;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.helpers.IContainerCraftingPacket;
import appeng.helpers.IContainerPatternPacket;
import appeng.items.storage.ItemViewCell;
import appeng.tile.inventory.AppEngInternalUnivInventory;
import appeng.util.Platform;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.AdaptorItemHandler;
import appeng.util.inv.WrapperInvItemHandler;
import appeng.util.item.AEItemStack;
import appeng.util.item.ExAEStack;
import appeng.util.prioritylist.IPartitionList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public class PacketJEIRecipe extends AppEngPacket {

    private List<List<IExAEStack<?>>> inputs;
    private List<IExAEStack<?>> outputs;

    // automatic.
    @SuppressWarnings("unchecked")
    public PacketJEIRecipe(final ByteBuf stream) throws IOException {
        this.inputs = Arrays.asList(new List[stream.readByte()]);
        for (int i = 0; i < this.inputs.size(); i++) {
            final List<IExAEStack<?>> stacks = Arrays.asList(new IExAEStack<?>[stream.readByte()]);
            for (int j = 0; j < stacks.size(); j++) {
                if (stream.readBoolean()) {
                    stacks.set(j, ExAEStack.fromPacket(stream));
                } else {
                    stacks.set(j, null);
                }
            }
            this.inputs.set(i, stacks);
        }

        this.outputs = Arrays.asList(new IExAEStack<?>[stream.readByte()]);
        for (int i = 0; i < this.outputs.size(); i++) {
            this.outputs.set(i, ExAEStack.fromPacket(stream));
        }
    }

    // api
    public PacketJEIRecipe(final List<List<IExAEStack<?>>> inputs, final List<IExAEStack<?>> outputs) throws IOException {
        this.inputs = inputs;
        this.outputs = outputs;

        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());

        data.writeByte(inputs.size());
        for (final List<IExAEStack<?>> stacks : inputs) {
            data.writeByte(stacks.size());
            for (final IExAEStack<?> stack : stacks) {
                if (stack != null) {
                    data.writeBoolean(true);
                    stack.writeToPacket(data);
                } else {
                    data.writeBoolean(false);
                }
            }
        }

        data.writeByte(outputs.size());
        for (final IExAEStack<?> stack : outputs) {
            stack.writeToPacket(data);
        }

        this.configureWrite(data);
    }

    @Override
    public void serverPacketData(final INetworkInfo manager, final AppEngPacket packet, final EntityPlayer player) {
        final EntityPlayerMP pmp = (EntityPlayerMP) player;
        if (pmp.openContainer instanceof final IContainerCraftingPacket cont) {
            handleCraftingTable(cont, pmp);
        } else if (pmp.openContainer instanceof final IContainerPatternPacket cont) {
            handlePatternEncoder(cont);
        }
    }

    private void handleCraftingTable(final IContainerCraftingPacket cct, final EntityPlayerMP pmp) {
        final IGridNode node = cct.getNetworkNode();

        if (node == null) {
            return;
        }

        final IGrid grid = node.getGrid();
        if (grid == null) {
            return;
        }

        final IStorageGrid inv = grid.getCache(IStorageGrid.class);
        final IEnergyGrid energy = grid.getCache(IEnergyGrid.class);
        final ISecurityGrid security = grid.getCache(ISecurityGrid.class);
        final ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
        final IItemHandler craftMatrix = cct.getInventoryByName("crafting");
        final IItemHandler playerInventory = cct.getInventoryByName("player");

        if (inv != null && security != null) {
            final IMEMonitor<IAEItemStack> storage = inv.getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
            final IPartitionList<IAEItemStack> filter = ItemViewCell.createFilter(cct.getViewCells());

            for (int x = 0; x < craftMatrix.getSlots(); x++) {
                if (x >= this.inputs.size()) {
                    ItemHandlerUtil.setStackInSlot(craftMatrix, x, ItemStack.EMPTY);
                    continue;
                }

                ItemStack currentItem = craftMatrix.getStackInSlot(x);
                final List<IExAEStack<?>> input = this.inputs.get(x);

                // prepare slots
                if (!currentItem.isEmpty()) {
                    // already the correct item?
                    ItemStack newItem = this.canUseInSlot(x, currentItem);

                    if (!cct.useRealItems() && input != null) {
                        if (!input.isEmpty()) {
                            currentItem.setCount((int) Math.max(input.get(0).getStackSize(), Integer.MAX_VALUE));
                        }
                    }

                    // put away old item
                    if (newItem != currentItem && security.hasPermission(pmp, SecurityPermissions.INJECT)) {
                        final IAEItemStack in = AEItemStack.fromItemStack(currentItem);
                        final IAEItemStack out = cct.useRealItems() ? Platform.poweredInsert(energy, storage, in, cct.getActionSource()) : null;
                        if (out != null) {
                            currentItem = out.createItemStack();
                        } else {
                            currentItem = ItemStack.EMPTY;
                        }
                    }
                }

                if (currentItem.isEmpty() && inputs.size() > x && input != null) {
                    // for each variant
                    for (int y = 0; y < input.size() && currentItem.isEmpty(); y++) {
                        final IExAEStack<?> exRequest = input.get(y);
                        if (exRequest == null || !(exRequest.unwrap() instanceof final IAEItemStack request)) {
                            continue;
                        }

                        final ItemStack reqIs = request.createItemStack();
                        // try ae
                        if ((filter == null || filter.isListed(request)) && security.hasPermission(pmp, SecurityPermissions.EXTRACT)) {
                            request.setStackSize(1);
                            IAEItemStack out;

                            if (cct.useRealItems()) {
                                out = Platform.poweredExtraction(energy, storage, request, cct.getActionSource());
                                if (out == null) {
                                    if (request.getItem().isDamageable() || Platform.isGTDamageableItem(request.getItem())) {
                                        Collection<IAEItemStack> outList = inv.getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class)).getStorageList().findFuzzy(request, FuzzyMode.IGNORE_ALL);
                                        for (IAEItemStack is : outList) {
                                            if (is.getStackSize() == 0) {
                                                continue;
                                            }
                                            if (Platform.isGTDamageableItem(request.getItem())) {
                                                if (!(is.getDefinition().getMetadata() == request.getDefinition().getMetadata())) {
                                                    continue;
                                                }
                                            }
                                            out = Platform.poweredExtraction(energy, storage, is.copy().setStackSize(1), cct.getActionSource());
                                            if (out != null) {
                                                break;
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Query the crafting grid if there is a pattern providing the item
                                if (!crafting.getUnivCraftingFor(request, null, 0, null).isEmpty()) {
                                    out = request;
                                } else {
                                    // Fall back using an existing item
                                    out = storage.extractItems(request, Actionable.SIMULATE, cct.getActionSource());
                                }
                            }

                            if (out != null) {
                                if (!cct.useRealItems()) {
                                    out.setStackSize(request.getStackSize());
                                }
                                currentItem = out.createItemStack();
                            }
                        }

                        // try inventory
                        if (currentItem.isEmpty()) {
                            AdaptorItemHandler ad = new AdaptorItemHandler(playerInventory);

                            if (cct.useRealItems()) {
                                currentItem = ad.removeSimilarItems(1, reqIs, FuzzyMode.IGNORE_ALL, null);
                            } else {
                                currentItem = ad.simulateSimilarRemove(reqIs.getCount(), reqIs, FuzzyMode.IGNORE_ALL, null);
                            }
                        }
                    }
                    if (!cct.useRealItems()) {
                        if (currentItem.isEmpty() && inputs.size() > x) {
                            final IExAEStack<?> stack = input.get(0);
                            if (stack != null && stack.unwrap() instanceof final IAEItemStack ais) {
                                currentItem = ais.createItemStack();
                            }
                        }
                    }
                }
                ItemHandlerUtil.setStackInSlot(craftMatrix, x, currentItem);
            }

            pmp.openContainer.onCraftMatrixChanged(new WrapperInvItemHandler(craftMatrix));
        }
    }

    /**
     * @param slot
     * @param is   itemstack
     * @return is if it can be used, else EMPTY
     */
    private ItemStack canUseInSlot(int slot, ItemStack is) {
        if (this.inputs.get(slot) != null) {
            for (IExAEStack<?> option : this.inputs.get(slot)) {
                if (option != null && option.unwrap() instanceof final IAEItemStack ais && ais.isSameType(is)) {
                    return is;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private void handlePatternEncoder(final IContainerPatternPacket cont) {
        final AppEngInternalUnivInventory ic = cont.getCraftingInventory();
        final AppEngInternalUnivInventory io = cont.getOutputInventory();

        int i = 0;
        for (final List<IExAEStack<?>> input : this.inputs) {
            if (i >= ic.getSlots()) {
                break;
            }
            if (input.isEmpty()) {
                continue;
            }
            for (final IExAEStack<?> stack : input) {
                if (stack != null) {
                    ic.setStackInSlot(i, input.get(0));
                    ++i;
                    break;
                }
            }
        }
        for (; i < ic.getSlots(); i++) {
            ic.setStackInSlot(i, (IExAEStack<?>) null);
        }

        i = 0;
        for (final IExAEStack<?> stack : this.outputs) {
            if (i >= io.getSlots()) {
                break;
            }
            if (stack == null) {
                continue;
            }
            io.setStackInSlot(i, stack);
            ++i;
        }
        for (; i < io.getSlots(); i++) {
            io.setStackInSlot(i, (IExAEStack<?>) null);
        }
    }

}
