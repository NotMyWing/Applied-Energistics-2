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

package appeng.integration.modules.jei;


import appeng.api.AEApi;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IExAEStack;
import appeng.container.implementations.ContainerCraftingTerm;
import appeng.container.implementations.ContainerPatternEncoder;
import appeng.container.implementations.ContainerWirelessCraftingTerminal;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.core.AELog;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketJEIRecipe;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import appeng.util.item.ExAEStack;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.transfer.RecipeTransferErrorInternal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


class RecipeTransferHandler<T extends Container> implements IRecipeTransferHandler<T> {

    private final Class<T> containerClass;
    private final JeiStackTypeTable stt;

    RecipeTransferHandler(final Class<T> containerClass, final JeiStackTypeTable stt) {
        this.containerClass = containerClass;
        this.stt = stt;
    }

    @Override
    public Class<T> getContainerClass() {
        return this.containerClass;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(@Nonnull T container, IRecipeLayout recipeLayout, @Nonnull EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
        final String recipeType = recipeLayout.getRecipeCategory().getUid();

        if (recipeType.equals(VanillaRecipeCategoryUid.INFORMATION) || recipeType.equals(VanillaRecipeCategoryUid.FUEL)) {
            return RecipeTransferErrorInternal.INSTANCE;
        }

        if (!doTransfer) {
            if (recipeType.equals(VanillaRecipeCategoryUid.CRAFTING) && (container instanceof ContainerCraftingTerm || container instanceof ContainerWirelessCraftingTerminal)) {
                JEIMissingItem error = new JEIMissingItem(container, recipeLayout);
                if (error.errored())
                    return error;
            }
            return null;
        }

        if (container instanceof ContainerPatternEncoder) {
            try {
                if (!((ContainerPatternEncoder) container).isCraftingMode()) {
                    if (recipeType.equals(VanillaRecipeCategoryUid.CRAFTING)) {
                        NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.CraftMode", "1"));
                    }
                } else if (!recipeType.equals(VanillaRecipeCategoryUid.CRAFTING)) {

                    NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.CraftMode", "0"));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        final List<List<IExAEStack<?>>> inputs = new ArrayList<>();
        final List<IExAEStack<?>> outputs = new ArrayList<>();

        if (recipeType.equals(VanillaRecipeCategoryUid.CRAFTING)) {
            this.transferCrafting(recipeLayout.getItemStacks(), inputs, outputs, container);
        } else {
            this.transferProcessing(recipeLayout, inputs, outputs);
        }

        try {
            NetworkHandler.instance().sendToServer(new PacketJEIRecipe(inputs, outputs));
        } catch (IOException e) {
            AELog.debug(e);
        }

        return null;
    }

    // crafting recipe: all ingredients are items, but we have to worry about slot indices
    private void transferCrafting(final IGuiItemStackGroup group, final List<List<IExAEStack<?>>> inputs, final List<IExAEStack<?>> outputs, final T container) {
        int slotIndex = 0;
        for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> ingredientEntry : group.getGuiIngredients().entrySet()) {
            IGuiIngredient<ItemStack> ingredient = ingredientEntry.getValue();
            if (!ingredient.isInput()) {
                ItemStack output = ingredient.getDisplayedIngredient();
                if (output != null) {
                    outputs.add(ExAEStack.of(AEItemStack.fromItemStack(output)));
                }
                continue;
            }

            boolean noSlot = true;
            for (final Slot slot : container.inventorySlots) {
                if (slot instanceof SlotCraftingMatrix || slot instanceof SlotFakeCraftingMatrix) {
                    if (slot.getSlotIndex() == slotIndex) {
                        final List<IExAEStack<?>> list = new ArrayList<>();
                        final ItemStack displayed = ingredient.getDisplayedIngredient();

                        // prefer currently displayed item
                        if (displayed != null && !displayed.isEmpty()) {
                            list.add(ExAEStack.of(AEItemStack.fromItemStack(displayed)));
                        }

                        // prefer pure crystals.
                        for (ItemStack stack : ingredient.getAllIngredients()) {
                            if (stack == null) {
                                continue;
                            }
                            if (Platform.isRecipePrioritized(stack)) {
                                list.add(0, ExAEStack.of(AEItemStack.fromItemStack(stack)));
                            } else {
                                list.add(ExAEStack.of(AEItemStack.fromItemStack(stack)));
                            }
                        }

                        inputs.add(list);
                        noSlot = false;
                        break;
                    }
                }
            }

            if (noSlot) {
                inputs.add(Collections.singletonList(null));
            }
            slotIndex++;
        }
    }

    // processing recipe: ingredients are heterogeneous, but we don't have to worry about slot indices
    private void transferProcessing(final IRecipeLayout recipe, final List<List<IExAEStack<?>>> inputs, final List<IExAEStack<?>> outputs) {
        for (final IStorageChannel<?> channel : AEApi.instance().storage().storageChannels()) {
            transferProcessingIngredients(channel, recipe.getIngredientsGroup(this.stt.getIngredientType(channel)).getGuiIngredients().values(), inputs, outputs);
        }
    }

    private <U extends IAEStack<U>> void transferProcessingIngredients(final IStorageChannel<U> channel, final Iterable<? extends IGuiIngredient<?>> ings,
                                                                       final List<List<IExAEStack<?>>> inputs, final List<IExAEStack<?>> outputs) {
        for (final IGuiIngredient<?> ing : ings) {
            if (ing.isInput()) {
                final List<IExAEStack<?>> stacks = new ArrayList<>();

                // prefer currently displayed item
                final Object displayed = ing.getDisplayedIngredient();
                if (displayed != null) {
                    final U stack = channel.createStack(displayed);
                    if (stack != null) {
                        stacks.add(ExAEStack.of(stack));
                    }
                }

                // prefer pure crystals.
                for (final Object ingObj : ing.getAllIngredients()) {
                    if (ingObj == null) {
                        continue;
                    }
                    final U stack = channel.createStack(ingObj);
                    if (stack == null) {
                        continue;
                    }

                    if (ingObj instanceof final ItemStack is && Platform.isRecipePrioritized(is)) {
                        stacks.add(0, ExAEStack.of(stack));
                    } else {
                        stacks.add(ExAEStack.of(stack));
                    }
                }

                inputs.add(stacks);
            } else {
                final Object ingObj = ing.getDisplayedIngredient();
                if (ingObj != null) {
                    final U stack = channel.createStack(ingObj);
                    if (stack != null) {
                        outputs.add(ExAEStack.of(stack));
                    }
                }
            }
        }
    }

}
