package appeng.integration.modules.jei;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.helpers.IContainerCraftingPacket;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.gui.TooltipRenderer;
import mezz.jei.gui.recipes.RecipeLayout;
import mezz.jei.gui.recipes.RecipeTransferButton;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static mezz.jei.api.recipe.transfer.IRecipeTransferError.Type.USER_FACING;

public class JEIMissingItem implements IRecipeTransferError {

    private boolean errored;
    public long lastUpdate;
    private final List<Integer> craftableSlots = new ArrayList<>();
    private final List<Integer> foundSlots = new ArrayList<>();

    IItemList<IAEItemStack> available = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();

    IItemList<IAEItemStack> used = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();

    JEIMissingItem(Container container, @Nonnull IRecipeLayout recipeLayout) {
        if (container instanceof ContainerMEMonitorable) {
            IItemList<IAEItemStack> ir = ((ContainerMEMonitorable) container).items;

            IItemList<IAEItemStack> available = mergeInventories(ir, (ContainerMEMonitorable) container);

            boolean found;
            this.errored = false;
            recipeLayout.getItemStacks().addTooltipCallback(new CraftableCallBack(container, available));

            IItemList<IAEItemStack> used = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();
            for (IGuiIngredient<?> i : recipeLayout.getItemStacks().getGuiIngredients().values()) {
                found = false;
                if (i.isInput() && !i.getAllIngredients().isEmpty()) {
                    List<?> allIngredients = i.getAllIngredients();
                    for (Object allIngredient : allIngredients) {
                        if (allIngredient instanceof ItemStack) {
                            ItemStack stack = (ItemStack) allIngredient;
                            if (!stack.isEmpty()) {
                                IAEItemStack search = AEItemStack.fromItemStack(stack);
                                if (stack.getItem().isDamageable() || Platform.isGTDamageableItem(stack.getItem())) {
                                    Collection<IAEItemStack> fuzzy = available.findFuzzy(search, FuzzyMode.IGNORE_ALL);
                                    if (!fuzzy.isEmpty()) {
                                        for (IAEItemStack itemStack : fuzzy) {
                                            if (itemStack.getStackSize() > 0) {
                                                if (Platform.isGTDamageableItem(stack.getItem())) {
                                                    if (!(stack.getMetadata() == itemStack.getDefinition().getMetadata())) {
                                                        continue;
                                                    }
                                                }
                                                found = true;
                                                used.add(itemStack.copy().setStackSize(1));
                                            }
                                        }
                                    }
                                } else {
                                    IAEItemStack ext = available.findPrecise(search);
                                    if (ext != null) {
                                        IAEItemStack usedStack = used.findPrecise(ext);
                                        if (ext.getStackSize() > 0 && (usedStack == null || ext.getStackSize() > usedStack.getStackSize())) {
                                            used.add(ext.copy().setStackSize(1));
                                            found = true;
                                        }
                                    }
                                }
                            } else {
                                found = true;
                            }
                        }
                    }
                    if (!found) {
                        this.errored = true;
                        break;
                    }
                }
            }
        }
    }

    @Nonnull
    @Override
    public Type getType() {
        return USER_FACING;
    }

    @Override
    public void showError(Minecraft minecraft, int mouseX, int mouseY, @Nonnull IRecipeLayout recipeLayout, int recipeX, int recipeY) {
        Container c = minecraft.player.openContainer;
        if (c instanceof ContainerMEMonitorable container) {
            IItemList<IAEItemStack> ir = ((ContainerMEMonitorable) c).items;
            boolean found = false;
            boolean foundAny = false;
            boolean craftable = false;
            boolean foundAnyCraftable = false;
            int currentSlot = 0;
            this.errored = false;

            if (System.currentTimeMillis() - lastUpdate > 1000) {
                lastUpdate = System.currentTimeMillis();
                available = mergeInventories(ir, container);
                this.foundSlots.clear();
                this.craftableSlots.clear();
            } else {
                for (IGuiIngredient<?> i : recipeLayout.getItemStacks().getGuiIngredients().values()) {
                    if (i.isInput()) {
                        if (!foundSlots.contains(currentSlot)) {
                            if (craftableSlots.contains(currentSlot)) {
                                i.drawHighlight(minecraft, new Color(0.0f, 0.0f, 1.0f, 0.4f), recipeX, recipeY);
                            } else {
                                i.drawHighlight(minecraft, new Color(1.0f, 0.0f, 0.0f, 0.4f), recipeX, recipeY);
                            }
                        }
                    }
                    currentSlot++;
                }
                return;
            }
            this.used.resetStatus();

            for (IGuiIngredient<?> i : recipeLayout.getItemStacks().getGuiIngredients().values()) {
                found = false;
                craftable = false;
                IItemList<IAEItemStack> valid = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();
                if (i.isInput()) {
                    List<?> allIngredients = i.getAllIngredients();
                    for (Object allIngredient : allIngredients) {
                        if (allIngredient instanceof ItemStack stack) {
                            if (!stack.isEmpty()) {
                                IAEItemStack search = AEItemStack.fromItemStack(stack);
                                if (stack.getItem().isDamageable() || Platform.isGTDamageableItem(stack.getItem())) {
                                    Collection<IAEItemStack> fuzzy = available.findFuzzy(search, FuzzyMode.IGNORE_ALL);
                                    if (!fuzzy.isEmpty()) {
                                        for (IAEItemStack itemStack : fuzzy) {
                                            if (itemStack.getStackSize() > 0) {
                                                if (Platform.isGTDamageableItem(stack.getItem())) {
                                                    if (!(stack.getMetadata() == itemStack.getDefinition().getMetadata())) {
                                                        continue;
                                                    }
                                                }
                                                found = true;
                                                used.add(itemStack.copy().setStackSize(1));
                                                valid.add(itemStack.copy().setStackSize(1));
                                            } else {
                                                if (itemStack.isCraftable()) {
                                                    craftable = true;
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    IAEItemStack ext = available.findPrecise(search);
                                    if (ext != null) {
                                        IAEItemStack usedStack = used.findPrecise(ext);
                                        if (ext.getStackSize() > 0 && (usedStack == null || usedStack.getStackSize() < ext.getStackSize())) {
                                            used.add(ext.copy().setStackSize(1));
                                            if (craftable) {
                                                valid.resetStatus();
                                            }
                                            valid.add(ext.copy().setStackSize(1));
                                            found = true;
                                        } else if (ext.isCraftable()) {
                                            valid.add(ext.copy().setStackSize(1));
                                            craftable = true;
                                        }
                                    }
                                }
                            } else {
                                found = true;
                            }
                        }
                    }
                    if (i.getAllIngredients().isEmpty()) {
                        currentSlot++;
                        continue;
                    }
                    ArrayList<ItemStack> validStacks = new ArrayList<>();
                    valid.forEach(v -> {
                        if (v.getStackSize() > 0) {
                            ItemStack validStack = v.createItemStack();
                            validStack.setCount(1);
                            validStacks.add(validStack);
                        }
                    });
                    if (!found) {
                        if (craftable) {
                            i.drawHighlight(minecraft, new Color(0.0f, 0.0f, 1.0f, 0.4f), recipeX, recipeY);
                            this.craftableSlots.add(currentSlot);
                            recipeLayout.getItemStacks().set(currentSlot, validStacks);
                            foundAnyCraftable = true;
                        } else {
                            i.drawHighlight(minecraft, new Color(1.0f, 0.0f, 0.0f, 0.4f), recipeX, recipeY);
                        }
                        this.errored = true;
                    } else {
                        foundAny = true;
                        this.foundSlots.add(currentSlot);
                        recipeLayout.getItemStacks().set(currentSlot, validStacks);
                    }
                }
                currentSlot++;
            }
            RecipeTransferButton b = ((RecipeLayout) recipeLayout).getRecipeTransferButton();
            if (b != null) {
                List<String> tooltipLines = new ArrayList<>();
                b.init(c, minecraft.player);
                if (errored && foundAny) {
                    tooltipLines.add(I18n.translateToLocal("gui.tooltips.appliedenergistics2.PartialTransfer"));
                    b.enabled = true;
                    b.visible = true;
                }
                if (errored) {
                    tooltipLines.add(I18n.translateToLocal("gui.tooltips.appliedenergistics2.MissingItem"));
                }
                if (foundAnyCraftable) {
                    tooltipLines.add(I18n.translateToLocal("gui.tooltips.appliedenergistics2.CraftableItem"));
                }
                TooltipRenderer.drawHoveringText(minecraft, tooltipLines, mouseX, mouseY);
            }
        }
    }

    IItemList<IAEItemStack> mergeInventories(IItemList<IAEItemStack> repo, ContainerMEMonitorable containerCraftingTerm) {
        IItemList<IAEItemStack> itemList = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();
        for (IAEItemStack i : repo) {
            itemList.addStorage(i);
        }

        PlayerMainInvWrapper invWrapper = new PlayerMainInvWrapper(containerCraftingTerm.getPlayerInv());
        for (int i = 0; i < invWrapper.getSlots(); i++) {
            itemList.addStorage(AEItemStack.fromItemStack(invWrapper.getStackInSlot(i)));
        }

        if (containerCraftingTerm instanceof IContainerCraftingPacket) {
            IItemHandler itemHandler = ((IContainerCraftingPacket) containerCraftingTerm).getInventoryByName("crafting");
            if (itemHandler != null) {
                for (int i = 0; i < itemHandler.getSlots(); i++) {
                    itemList.addStorage(AEItemStack.fromItemStack(itemHandler.getStackInSlot(i)));
                }
            }
        }
        return itemList;
    }

    public boolean errored() {
        return errored;
    }
}
