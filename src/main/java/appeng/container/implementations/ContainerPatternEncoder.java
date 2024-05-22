package appeng.container.implementations;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.IDefinitions;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.IExAEStack;
import appeng.container.ContainerNull;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotPatternTerm;
import appeng.container.slot.SlotPlayerHotBar;
import appeng.container.slot.SlotPlayerInv;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.sync.packets.PacketPatternSlot;
import appeng.helpers.IContainerPatternPacket;
import appeng.items.storage.ItemViewCell;
import appeng.me.helpers.MachineSource;
import appeng.parts.reporting.AbstractPartEncoder;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.AppEngInternalUnivInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.AdaptorItemHandler;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import appeng.util.inv.WrapperCursorItemHandler;
import appeng.util.item.AEItemStack;
import appeng.util.item.ExAEStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class ContainerPatternEncoder extends ContainerMEMonitorable implements IAEAppEngInventory, IOptionalSlotHost, IContainerPatternPacket, AppEngInternalUnivInventory.IListener {

    protected AbstractPartEncoder patternTerminal = null;

    protected IGuiItemObject iGuiItemObject = null;

    final AppEngInternalInventory cOut = new AppEngInternalInventory(null, 1);

    protected AppEngInternalUnivInventory crafting;
    protected AppEngInternalUnivInventory output;
    protected SlotPatternTerm craftSlot;
    protected SlotRestrictedInput patternSlotIN;
    protected SlotRestrictedInput patternSlotOUT;
    protected IRecipe currentRecipe;

    protected SlotFakeCraftingMatrix[] craftingSlots;
    protected OptionalSlotFake[] outputSlots;

    @GuiSync(97)
    public boolean craftingMode = true;
    @GuiSync(96)
    public boolean substitute = false;

    protected ContainerPatternEncoder(InventoryPlayer ip, ITerminalHost monitorable, boolean bindInventory) {
        super(ip, monitorable, bindInventory);
        this.patternTerminal = (AbstractPartEncoder) monitorable;
        this.craftingMode = patternTerminal.isCraftingRecipe();
    }

    protected ContainerPatternEncoder(InventoryPlayer ip, ITerminalHost monitorable, IGuiItemObject iGuiItemObject, boolean bindInventory) {
        super(ip, monitorable, iGuiItemObject, bindInventory);
        if (monitorable instanceof AbstractPartEncoder) {
            patternTerminal = (AbstractPartEncoder) monitorable;
        }
        this.iGuiItemObject = iGuiItemObject;
    }

    @Override
    public AppEngInternalUnivInventory getCraftingInventory() {
        return this.crafting;
    }

    @Override
    public AppEngInternalUnivInventory getOutputInventory() {
        return this.output;
    }

    @Override
    public ItemStack transferStackInSlot(final EntityPlayer p, final int idx) {
        if (Platform.isClient()) {
            return ItemStack.EMPTY;
        }
        if (this.inventorySlots.get(idx) instanceof SlotPlayerInv || this.inventorySlots.get(idx) instanceof SlotPlayerHotBar) {
            final AppEngSlot clickSlot = (AppEngSlot) this.inventorySlots.get(idx); // require AE SLots!
            ItemStack itemStack = clickSlot.getStack();
            if (AEApi.instance().definitions().materials().blankPattern().isSameAs(itemStack)) {
                IItemHandler patternInv = this.getPart().getInventoryByName("pattern");
                ItemStack remainder = patternInv.insertItem(0, itemStack, false);
                clickSlot.putStack(remainder);
            }
        }
        return super.transferStackInSlot(p, idx);
    }

    public AbstractPartEncoder getPart() {
        return this.patternTerminal;
    }

    @Override
    public abstract boolean isSlotEnabled(int idx);

    @Override
    public void saveChanges() {
    }

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation mc, ItemStack removedStack, ItemStack newStack) {
    }

    @Override
    public void onChangeInventory(final AppEngInternalUnivInventory inv, final int slot, final InvOperation op, final IExAEStack<?> oldStack, final IExAEStack<?> newStack) {
        if (inv == this.crafting) {
            this.fixCraftingRecipes();
        }
    }

    void fixCraftingRecipes() {
        if (this.isCraftingMode()) {
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

    @Override
    public void putStackInSlot(int slotID, ItemStack stack) {
        super.putStackInSlot(slotID, stack);
        this.getAndUpdateOutput();
    }

    protected void updateOrderOfOutputSlots() {
        if (!this.isCraftingMode()) {
            if (craftSlot != null) {
                this.craftSlot.xPos = -9000;
            }

            for (int y = 0; y < 3; y++) {
                this.outputSlots[y].xPos = this.outputSlots[y].getX();
            }
        } else {
            if (craftSlot != null) {
                this.craftSlot.xPos = this.craftSlot.getX();
            }
            for (int y = 0; y < 3; y++) {
                this.outputSlots[y].xPos = -9000;
            }
        }
    }

    @Override
    public void onSlotChange(final Slot s) {
        if (s == this.patternSlotOUT && Platform.isServer()) {
            for (final IContainerListener listener : this.listeners) {
                for (final Slot slot : this.inventorySlots) {
                    if (slot instanceof OptionalSlotFake || slot instanceof SlotFakeCraftingMatrix) {
                        listener.sendSlotContents(this, slot.slotNumber, slot.getStack());
                    }
                }
                if (listener instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) listener).isChangingQuantityOnly = false;
                }
            }
            this.detectAndSendChanges();
        }
        if (s == this.craftSlot && Platform.isClient()) {
            this.getAndUpdateOutput();
        }
    }

    public void encodeAndMoveToInventory() {
        encode();
        ItemStack output = this.patternSlotOUT.getStack();
        if (!output.isEmpty()) {
            if (!getPlayerInv().addItemStackToInventory(output)) {
                getPlayerInv().player.dropItem(output, false);
            }
            this.patternSlotOUT.putStack(ItemStack.EMPTY);
        }
    }

    public void encode() {
        ItemStack output = this.patternSlotOUT.getStack();

        final IExAEStack<?>[] in = this.getInputs();
        final IExAEStack<?>[] out = this.getOutputs();

        // if there is no input, this would be silly.
        if (in == null || out == null) {
            return;
        }

        // first check the output slots, should either be null, or a pattern
        if (!output.isEmpty() && !this.isPattern(output)) {
            return;
        } // if nothing is there we should snag a new pattern.
        else if (output.isEmpty()) {
            output = this.patternSlotIN.getStack();
            if (output.isEmpty() || !this.isPattern(output)) {
                return; // no blanks.
            }

            // remove one, and clear the input slot.
            output.setCount(output.getCount() - 1);
            if (output.getCount() == 0) {
                this.patternSlotIN.putStack(ItemStack.EMPTY);
            }

            // add a new encoded pattern.
            Optional<ItemStack> maybePattern = AEApi.instance().definitions().items().encodedPattern().maybeStack(1);
            if (maybePattern.isPresent()) {
                output = maybePattern.get();
            }
        }

        // encode the slot.
        final NBTTagCompound encodedValue = new NBTTagCompound();

        final NBTTagList tagIn = new NBTTagList();
        final NBTTagList tagOut = new NBTTagList();

        for (final IExAEStack<?> i : in) {
            tagIn.appendTag(this.createItemTag(i));
        }

        for (final IExAEStack<?> i : out) {
            tagOut.appendTag(this.createItemTag(i));
        }

        encodedValue.setTag("in", tagIn);
        encodedValue.setTag("out", tagOut);
        encodedValue.setBoolean("crafting", this.isCraftingMode());
        encodedValue.setBoolean("substitute", this.isSubstitute());

        output.setTagCompound(encodedValue);

        patternSlotOUT.putStack(output);
    }

    public void multiply(int multiple) {
        ItemStack[] input = new ItemStack[craftingSlots.length];
        boolean canMultiplyInputs = true;
        boolean canMultiplyOutputs = true;

        for (int x = 0; x < this.craftingSlots.length; x++) {
            input[x] = this.craftingSlots[x].getStack();
            if (!input[x].isEmpty() && input[x].getCount() * multiple < 1) {
                canMultiplyInputs = false;
            }
        }
        for (final OptionalSlotFake outputSlot : this.outputSlots) {
            final ItemStack out = outputSlot.getStack();
            if (!out.isEmpty() && out.getCount() * multiple < 1) {
                canMultiplyOutputs = false;
            }
        }
        if (canMultiplyInputs && canMultiplyOutputs) {
            for (SlotFakeCraftingMatrix craftingSlot : this.craftingSlots) {
                ItemStack stack = craftingSlot.getStack();
                if (!stack.isEmpty()) {
                    craftingSlot.getStack().setCount(stack.getCount() * multiple);
                }
            }
            for (OptionalSlotFake outputSlot : this.outputSlots) {
                ItemStack stack = outputSlot.getStack();
                if (!stack.isEmpty()) {
                    outputSlot.getStack().setCount(stack.getCount() * multiple);
                }
            }
        }
    }

    public void divide(int divide) {
        ItemStack[] input = new ItemStack[craftingSlots.length];
        boolean canDivideInputs = true;
        boolean canDivideOutputs = true;

        for (int x = 0; x < this.craftingSlots.length; x++) {
            input[x] = this.craftingSlots[x].getStack();
            if (!input[x].isEmpty() && input[x].getCount() % divide != 0) {
                canDivideInputs = false;
            }
        }
        for (final OptionalSlotFake outputSlot : this.outputSlots) {
            final ItemStack out = outputSlot.getStack();
            if (!out.isEmpty() && out.getCount() % divide != 0) {
                canDivideOutputs = false;
            }
        }
        if (canDivideInputs && canDivideOutputs) {
            for (SlotFakeCraftingMatrix craftingSlot : this.craftingSlots) {
                ItemStack stack = craftingSlot.getStack();
                if (!stack.isEmpty()) {
                    craftingSlot.getStack().setCount(stack.getCount() / divide);
                }
            }
            for (OptionalSlotFake outputSlot : this.outputSlots) {
                ItemStack stack = outputSlot.getStack();
                if (!stack.isEmpty()) {
                    outputSlot.getStack().setCount(stack.getCount() / divide);
                }
            }
        }
    }

    public void increase(int increase) {
        ItemStack[] input = new ItemStack[craftingSlots.length];
        boolean canIncreaseInputs = true;
        boolean canIncreaseOutputs = true;

        for (int x = 0; x < this.craftingSlots.length; x++) {
            input[x] = this.craftingSlots[x].getStack();
            if (!input[x].isEmpty() && input[x].getCount() + increase < 1) {
                canIncreaseInputs = false;
            }
        }
        for (final OptionalSlotFake outputSlot : this.outputSlots) {
            final ItemStack out = outputSlot.getStack();
            if (!out.isEmpty() && out.getCount() + increase < 1) {
                canIncreaseOutputs = false;
            }
        }
        if (canIncreaseInputs && canIncreaseOutputs) {
            for (SlotFakeCraftingMatrix craftingSlot : this.craftingSlots) {
                ItemStack stack = craftingSlot.getStack();
                if (!stack.isEmpty()) {
                    craftingSlot.getStack().setCount(stack.getCount() + increase);
                }
            }
            for (OptionalSlotFake outputSlot : this.outputSlots) {
                ItemStack stack = outputSlot.getStack();
                if (!stack.isEmpty()) {
                    outputSlot.getStack().setCount(stack.getCount() + increase);
                }
            }
        }
    }

    public void decrease(int decrease) {
        ItemStack[] input = new ItemStack[craftingSlots.length];
        boolean canDecreaseInputs = true;
        boolean canDecreaseOutputs = true;

        for (int x = 0; x < this.craftingSlots.length; x++) {
            input[x] = this.craftingSlots[x].getStack();
            if (!input[x].isEmpty() && input[x].getCount() - decrease < 1) {
                canDecreaseInputs = false;
            }
        }
        for (final OptionalSlotFake outputSlot : this.outputSlots) {
            final ItemStack out = outputSlot.getStack();
            if (!out.isEmpty() && out.getCount() - decrease < 1) {
                canDecreaseOutputs = false;
            }
        }
        if (canDecreaseInputs && canDecreaseOutputs) {
            for (SlotFakeCraftingMatrix craftingSlot : this.craftingSlots) {
                ItemStack stack = craftingSlot.getStack();
                if (!stack.isEmpty()) {
                    craftingSlot.getStack().setCount(stack.getCount() - decrease);
                }
            }
            for (OptionalSlotFake outputSlot : this.outputSlots) {
                ItemStack stack = outputSlot.getStack();
                if (!stack.isEmpty()) {
                    outputSlot.getStack().setCount(stack.getCount() - decrease);
                }
            }
        }
    }

    public void maximizeCount() {
        ItemStack[] input = new ItemStack[craftingSlots.length];
        boolean canGrowInputs = true;
        boolean canGrowOutputs = true;
        int maxInputStackGrowth = 0;
        int maxOutputStackGrowth = 0;

        for (int x = 0; x < this.craftingSlots.length; x++) {
            input[x] = this.craftingSlots[x].getStack();
            if (!input[x].isEmpty() && input[x].getMaxStackSize() - input[x].getCount() > maxInputStackGrowth) {
                maxInputStackGrowth = input[x].getMaxStackSize() - input[x].getCount();
            }
            if (!input[x].isEmpty() && input[x].getCount() + maxInputStackGrowth > input[x].getMaxStackSize()) {
                canGrowInputs = false;
            }
        }
        for (final OptionalSlotFake outputSlot : this.outputSlots) {
            final ItemStack out = outputSlot.getStack();
            {
                maxOutputStackGrowth = out.getMaxStackSize() - out.getCount();
            }
            if (!out.isEmpty() && out.getCount() + maxOutputStackGrowth > out.getMaxStackSize()) {
                canGrowOutputs = false;
            }
        }
        if (canGrowInputs && canGrowOutputs) {
            int maxStackGrowth = Math.min(maxInputStackGrowth, maxOutputStackGrowth);
            for (SlotFakeCraftingMatrix craftingSlot : this.craftingSlots) {
                ItemStack stack = craftingSlot.getStack();
                if (!stack.isEmpty()) {
                    craftingSlot.getStack().setCount(stack.getCount() + maxStackGrowth);
                }
            }
            for (OptionalSlotFake outputSlot : this.outputSlots) {
                ItemStack stack = outputSlot.getStack();
                if (!stack.isEmpty()) {
                    outputSlot.getStack().setCount(stack.getCount() + maxStackGrowth);
                }
            }
        }
    }

    protected IExAEStack<?>[] getInputs() {
        final IExAEStack<?>[] input = new IExAEStack<?>[craftingSlots.length];
        boolean hasValue = false;

        for (int x = 0; x < this.crafting.getSlots(); x++) {
            input[x] = this.crafting.getStackInSlot(x);
            if (input[x] != null) {
                hasValue = true;
            }
        }

        if (hasValue) {
            return input;
        }

        return null;
    }

    protected IExAEStack<?>[] getOutputs() {
        if (this.isCraftingMode()) {
            final ItemStack out = this.getAndUpdateOutput();

            if (!out.isEmpty() && out.getCount() > 0) {
                return new IExAEStack<?>[] {ExAEStack.of(AEItemStack.fromItemStack(out))};
            }
        } else {
            final List<IExAEStack<?>> list = new ArrayList<>(outputSlots.length);

            for (int i = 0; i < this.output.getSlots(); i++) {
                final IExAEStack<?> out = this.output.getStackInSlot(i);

                if (out != null && out.getStackSize() > 0) {
                    list.add(out);
                }
            }

            if (!list.isEmpty()) {
                return list.toArray(new IExAEStack<?>[0]);
            }
        }

        return null;
    }

    protected ItemStack getAndUpdateOutput() {
        final World world = this.getPlayerInv().player.world;
        final InventoryCrafting ic = new InventoryCrafting(this, 3, 3);

        for (int x = 0; x < ic.getSizeInventory(); x++) {
            final IExAEStack<?> stack = this.crafting.getStackInSlot(x);
            if (stack != null) {
                if (stack.unwrap() instanceof final IAEItemStack ais) {
                    ic.setInventorySlotContents(x, ais.createItemStack());
                } else { // ingredient is not an item; invalid crafting recipe!
                    this.currentRecipe = null;
                    this.cOut.setStackInSlot(0, ItemStack.EMPTY);
                    return ItemStack.EMPTY;
                }
            }
        }

        if (this.currentRecipe == null || !this.currentRecipe.matches(ic, world)) {
            this.currentRecipe = CraftingManager.findMatchingRecipe(ic, world);
        }

        final ItemStack is;

        if (this.currentRecipe == null) {
            is = ItemStack.EMPTY;
        } else {
            is = this.currentRecipe.getCraftingResult(ic);
        }

        this.cOut.setStackInSlot(0, is);
        return is;
    }

    public boolean isCraftingMode() {
        return this.craftingMode;
    }

    public void setCraftingMode(final boolean craftingMode) {
        this.craftingMode = craftingMode;
        if (getPart() != null) {
            getPart().setCraftingRecipe(craftingMode);
        } else if (iGuiItemObject != null) {
            NBTTagCompound nbtTagCompound = iGuiItemObject.getItemStack().getTagCompound();
            if (nbtTagCompound != null) {
                nbtTagCompound.setBoolean("isCraftingMode", craftingMode);
                this.updateOrderOfOutputSlots();
            }
        }
        if (craftingMode) {
            this.fixCraftingRecipes();
        }
    }


    boolean isSubstitute() {
        return this.substitute;
    }

    public void setSubstitute(final boolean substitute) {
        this.substitute = substitute;
        if (getPart() != null) {
            getPart().setSubstitution(substitute);
        } else if (iGuiItemObject != null) {
            NBTTagCompound nbtTagCompound = iGuiItemObject.getItemStack().getTagCompound();
            if (nbtTagCompound != null) {
                nbtTagCompound.setBoolean("isSubstitute", substitute);
            }
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (Platform.isServer()) {
            if (getPart() != null) {
                if (this.isCraftingMode() != this.getPart().isCraftingRecipe()) {
                    this.setCraftingMode(this.getPart().isCraftingRecipe());
                    this.updateOrderOfOutputSlots();
                }
                this.substitute = this.getPart().isSubstitution();
            } else if (iGuiItemObject != null) {
                NBTTagCompound nbtTagCompound = iGuiItemObject.getItemStack().getTagCompound();
                if (nbtTagCompound != null) {
                    if (nbtTagCompound.hasKey("isCraftingMode")) {
                        boolean crafting = nbtTagCompound.getBoolean("isCraftingMode");
                        if (this.isCraftingMode() != crafting) {
                            this.setCraftingMode(crafting);
                            this.updateOrderOfOutputSlots();
                        }
                    } else {
                        nbtTagCompound.setBoolean("isCraftingMode", false);
                    }
                } else {
                    nbtTagCompound = new NBTTagCompound();
                    nbtTagCompound.setBoolean("isCraftingMode", false);
                    iGuiItemObject.getItemStack().setTagCompound(nbtTagCompound);
                }
                nbtTagCompound = iGuiItemObject.getItemStack().getTagCompound();
                if (nbtTagCompound != null) {
                    if (nbtTagCompound.hasKey("isSubstitute")) {
                        boolean substitute = nbtTagCompound.getBoolean("isSubstitute");
                        if (this.isSubstitute() != substitute) {
                            this.setSubstitute(substitute);
                        }
                    } else {
                        nbtTagCompound.setBoolean("isSubstitute", false);
                    }
                } else {
                    nbtTagCompound = new NBTTagCompound();
                    nbtTagCompound.setBoolean("isSubstitute", false);
                    iGuiItemObject.getItemStack().setTagCompound(nbtTagCompound);
                }
            }
        }
    }

    @Override
    public void onUpdate(final String field, final Object oldValue, final Object newValue) {
        super.onUpdate(field, oldValue, newValue);

        if (field.equals("craftingMode")) {
            this.setCraftingMode((Boolean) newValue);
            this.getAndUpdateOutput();
            this.updateOrderOfOutputSlots();
        }
    }

    boolean isPattern(final ItemStack output) {
        if (output.isEmpty()) {
            return false;
        }

        final IDefinitions definitions = AEApi.instance().definitions();

        boolean isPattern = definitions.items().encodedPattern().isSameAs(output);
        isPattern |= definitions.materials().blankPattern().isSameAs(output);

        return isPattern;
    }

    NBTBase createItemTag(final IExAEStack<?> i) {
        final NBTTagCompound c = new NBTTagCompound();

        if (i != null) {
            i.writeToNBT(c);
        }

        return c;
    }

    public void clear() {
        for (final Slot s : this.craftingSlots) {
            s.putStack(ItemStack.EMPTY);
        }

        for (final Slot s : this.outputSlots) {
            s.putStack(ItemStack.EMPTY);
        }

        this.detectAndSendChanges();
        this.getAndUpdateOutput();
    }

    public void craftOrGetItem(final PacketPatternSlot packetPatternSlot) {
        if (packetPatternSlot.slotItem != null && this.getCellInventory() != null) {
            final IAEItemStack out = packetPatternSlot.slotItem.copy();
            InventoryAdaptor inv = new AdaptorItemHandler(new WrapperCursorItemHandler(this.getPlayerInv().player.inventory));
            final InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(this.getPlayerInv().player);

            if (packetPatternSlot.shift) {
                inv = playerInv;
            }

            if (!inv.simulateAdd(out.createItemStack()).isEmpty()) {
                return;
            }

            final IAEItemStack extracted = Platform.poweredExtraction(this.getPowerSource(), this.getCellInventory(), out, this.getActionSource());
            final EntityPlayer p = this.getPlayerInv().player;

            if (extracted != null) {
                inv.addItems(extracted.createItemStack());
                if (p instanceof EntityPlayerMP) {
                    this.updateHeld((EntityPlayerMP) p);
                }
                this.detectAndSendChanges();
                return;
            }

            final InventoryCrafting ic = new InventoryCrafting(new ContainerNull(), 3, 3);
            final InventoryCrafting real = new InventoryCrafting(new ContainerNull(), 3, 3);

            for (int x = 0; x < 9; x++) {
                ic.setInventorySlotContents(x, packetPatternSlot.pattern[x] == null ? ItemStack.EMPTY : packetPatternSlot.pattern[x].createItemStack());
            }

            final IRecipe r = CraftingManager.findMatchingRecipe(ic, p.world);

            if (r == null) {
                return;
            }

            IMEMonitor<IAEItemStack> storage = null;
            if (getPart() != null) {
                storage = this.getPart()
                        .getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
            } else if (iGuiItemObject != null) {
                storage = ((ITerminalHost) iGuiItemObject).getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
            }

            final IItemList<IAEItemStack> all = storage.getStorageList();

            final ItemStack is = r.getCraftingResult(ic);

            for (int x = 0; x < ic.getSizeInventory(); x++) {
                if (!ic.getStackInSlot(x).isEmpty()) {
                    final ItemStack pulled = Platform.extractItemsByRecipe(this.getPowerSource(), this.getActionSource(), storage, p.world, r, is, ic,
                            ic.getStackInSlot(x), x, all, Actionable.MODULATE, ItemViewCell.createFilter(this.getViewCells()));
                    real.setInventorySlotContents(x, pulled);
                }
            }

            final IRecipe rr = CraftingManager.findMatchingRecipe(real, p.world);

            if (rr == r && Platform.itemComparisons().isSameItem(rr.getCraftingResult(real), is)) {
                final InventoryCraftResult craftingResult = new InventoryCraftResult();
                craftingResult.setRecipeUsed(rr);

                final SlotCrafting sc = new SlotCrafting(p, real, craftingResult, 0, 0, 0);
                sc.onTake(p, is);

                for (int x = 0; x < real.getSizeInventory(); x++) {
                    final ItemStack failed = playerInv.addItems(real.getStackInSlot(x));

                    if (!failed.isEmpty()) {
                        p.dropItem(failed, false);
                    }
                }

                inv.addItems(is);
                if (p instanceof EntityPlayerMP) {
                    this.updateHeld((EntityPlayerMP) p);
                }
                this.detectAndSendChanges();
            } else {
                for (int x = 0; x < real.getSizeInventory(); x++) {
                    final ItemStack failed = real.getStackInSlot(x);
                    if (!failed.isEmpty()) {
                        this.getCellInventory()
                                .injectItems(AEItemStack.fromItemStack(failed), Actionable.MODULATE,
                                        new MachineSource(this.getPart() != null ? this.getPart() : (IActionHost) iGuiItemObject));
                    }
                }
            }
        }
    }
}
