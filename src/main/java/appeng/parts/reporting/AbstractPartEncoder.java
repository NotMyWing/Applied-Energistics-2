package appeng.parts.reporting;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.parts.IPartModel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IExAEStack;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IGuiHost;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.AppEngInternalUnivInventory;
import appeng.util.inv.InvOperation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class AbstractPartEncoder extends AbstractPartTerminal implements AppEngInternalUnivInventory.IListener, IGuiHost {

    protected AppEngInternalUnivInventory crafting;
    protected AppEngInternalUnivInventory output;
    protected AppEngInternalInventory pattern;

    protected boolean craftingMode = true;
    protected boolean substitute = false;

    public AbstractPartEncoder(ItemStack is) {
        super(is);
    }

    @Override
    public void getDrops(final List<ItemStack> drops, final boolean wrenched) {
        for (final ItemStack is : this.pattern) {
            if (!is.isEmpty()) {
                drops.add(is);
            }
        }
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.pattern.readFromNBT(data, "pattern");
        this.output.readFromNBT(data, "outputList");
        this.crafting.readFromNBT(data, "crafting");
    }

    @Override
    public void writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        this.pattern.writeToNBT(data, "pattern");
        this.output.writeToNBT(data, "outputList");
        this.crafting.writeToNBT(data, "crafting");
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc, final ItemStack removedStack, final ItemStack newStack) {
        if (inv == this.pattern && slot == 1) {
            final ItemStack is = this.pattern.getStackInSlot(1);
            if (!is.isEmpty() && is.getItem() instanceof ICraftingPatternItem) {
                final ICraftingPatternItem pattern = (ICraftingPatternItem) is.getItem();
                final ICraftingPatternDetails details = pattern.getPatternForItem(is, this.getHost().getTile().getWorld());
                if (details != null) {
                    this.setCraftingRecipe(details.isCraftable());
                    this.setSubstitution(details.canSubstitute());

                    for (int x = 0; x < this.crafting.getSlots() && x < details.getUnivInputs().length; x++) {
                        this.crafting.setStackInSlot(x, details.getUnivInputs()[x]);
                    }

                    for (int x = 0; x < this.output.getSlots(); x++) {
                        if (x < details.getUnivOutputs().length) {
                            this.output.setStackInSlot(x, details.getUnivOutputs()[x]);
                        } else {
                            this.output.setStackInSlot(x, (IExAEStack<?>) null);
                        }
                    }
                }
            }
        }

        this.getHost().markForSave();
    }

    @Override
    public void onChangeInventory(final AppEngInternalUnivInventory inv, final int slot, final InvOperation op, final IExAEStack<?> oldStack, final IExAEStack<?> newStack) {
        if (inv == this.crafting) {
            this.fixCraftingRecipes();
        }
        this.getHost().markForSave();
    }

    private void fixCraftingRecipes() {
        if (this.isCraftingRecipe()) {
            for (int x = 0; x < this.crafting.getSlots(); x++) {
                final IExAEStack<?> is = this.crafting.getStackInSlot(x);
                if (is != null) {
                    if (!(is.unwrap() instanceof IAEItemStack)) {
                        this.crafting.setStackInSlot(x, (IExAEStack<?>) null);
                    } else {
                        is.setStackSize(1);
                    }
                }
            }
        }
    }

    public boolean isCraftingRecipe() {
        return this.craftingMode;
    }

    public void setCraftingRecipe(final boolean craftingMode) {
        this.craftingMode = craftingMode;
        this.fixCraftingRecipes();
    }

    public boolean isSubstitution() {
        return this.substitute;
    }

    public void setSubstitution(final boolean canSubstitute) {
        this.substitute = canSubstitute;
    }

    @Override
    public IItemHandler getInventoryByName(final String name) {
        if (name.equals("pattern")) {
            return this.pattern;
        }

        return super.getInventoryByName(name);
    }

    public AppEngInternalUnivInventory getCraftingInventory() {
        return this.crafting;
    }

    public AppEngInternalUnivInventory getOutputInventory() {
        return this.output;
    }

    @Override
    public GuiBridge getGui(final EntityPlayer p) {
        int x = (int) p.posX;
        int y = (int) p.posY;
        int z = (int) p.posZ;
        if (this.getHost().getTile() != null) {
            x = this.getTile().getPos().getX();
            y = this.getTile().getPos().getY();
            z = this.getTile().getPos().getZ();
        }

        if (getGuiBridge().hasPermissions(this.getHost().getTile(), x, y, z, this.getSide(), p)) {
            return getGuiBridge();
        }
        return GuiBridge.GUI_ME;
    }

    abstract public GuiBridge getGuiBridge();

    @Nonnull
    @Override
    abstract public IPartModel getStaticModels();
}
