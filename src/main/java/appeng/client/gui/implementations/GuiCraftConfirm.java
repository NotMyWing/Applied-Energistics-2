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

package appeng.client.gui.implementations;


import appeng.api.AEApi;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IUnivItemList;
import appeng.api.util.IExAEStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.core.AELog;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.IGuiHost;
import appeng.helpers.IPriorityHost;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.parts.reporting.*;
import appeng.util.Platform;
import com.google.common.base.Joiner;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class GuiCraftConfirm extends AEBaseGui {

    private final ContainerCraftConfirm ccc;

    private final int rows = 5;

    private final IUnivItemList storage = AEApi.instance().storage().createUnivList();
    private final IUnivItemList pending = AEApi.instance().storage().createUnivList();
    private final IUnivItemList missing = AEApi.instance().storage().createUnivList();

    private final List<IExAEStack<?>> visual = new ArrayList<>();

    private GuiBridge OriginalGui;
    private GuiButton cancel;
    private GuiButton start;
    private GuiButton selectCPU;
    private int tooltip = -1;

    public GuiCraftConfirm(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(new ContainerCraftConfirm(inventoryPlayer, te));
        this.xSize = 238;
        this.ySize = 206;

        final GuiScrollbar scrollbar = new GuiScrollbar();
        this.setScrollBar(scrollbar);

        this.ccc = (ContainerCraftConfirm) this.inventorySlots;
        this.ccc.setGui(this);

        if (te instanceof final IGuiHost gh) {
            this.OriginalGui = gh.getGui(inventoryPlayer.player);
        }
    }

    boolean isAutoStart() {
        return ((ContainerCraftConfirm) this.inventorySlots).isAutoStart();
    }

    @Override
    public void initGui() {
        super.initGui();

        this.start = new GuiButton(0, this.guiLeft + 162, this.guiTop + this.ySize - 25, 50, 20, GuiText.Start.getLocal());
        this.start.enabled = false;
        this.buttonList.add(this.start);

        this.selectCPU = new GuiButton(0, this.guiLeft + (219 - 180) / 2, this.guiTop + this.ySize - 68, 180, 20, GuiText.CraftingCPU
                .getLocal() + ": " + GuiText.Automatic);
        this.selectCPU.enabled = false;
        this.buttonList.add(this.selectCPU);

        this.cancel = new GuiButton(0, this.guiLeft + 6, this.guiTop + this.ySize - 25, 50, 20, GuiText.Cancel.getLocal());
        this.cancel.enabled = this.OriginalGui != null;
        this.buttonList.add(this.cancel);
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float btn) {
        this.updateCPUButtonText();

        this.start.enabled = !(this.ccc.hasNoCPU() || this.isSimulation());
        this.selectCPU.enabled = !this.isSimulation();

        final int gx = (this.width - this.xSize) / 2;
        final int gy = (this.height - this.ySize) / 2;

        this.tooltip = -1;

        final int offY = 23;
        int y = 0;
        int x = 0;
        for (int z = 0; z <= 4 * 5; z++) {
            final int minX = gx + 9 + x * 67;
            final int minY = gy + 22 + y * offY;

            if (minX < mouseX && minX + 67 > mouseX) {
                if (minY < mouseY && minY + offY - 2 > mouseY) {
                    this.tooltip = z;
                    break;
                }
            }

            x++;

            if (x > 2) {
                y++;
                x = 0;
            }
        }

        super.drawScreen(mouseX, mouseY, btn);
    }

    private void updateCPUButtonText() {
        String btnTextText = GuiText.CraftingCPU.getLocal() + ": " + GuiText.Automatic.getLocal();
        if (this.ccc.getSelectedCpu() >= 0)// && status.selectedCpu < status.cpus.size() )
        {
            if (this.ccc.getName().length() > 0) {
                final String name = this.ccc.getName().substring(0, Math.min(20, this.ccc.getName().length()));
                btnTextText = GuiText.CraftingCPU.getLocal() + ": " + name;
            } else {
                btnTextText = GuiText.CraftingCPU.getLocal() + ": #" + this.ccc.getSelectedCpu();
            }
        }

        if (this.ccc.hasNoCPU()) {
            btnTextText = GuiText.NoCraftingCPUs.getLocal();
        }

        this.selectCPU.displayString = btnTextText;
    }

    private boolean isSimulation() {
        return ((ContainerCraftConfirm) this.inventorySlots).isSimulation();
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        final long BytesUsed = this.ccc.getUsedBytes();
        final String byteUsed = NumberFormat.getInstance().format(BytesUsed);
        final String Add = BytesUsed > 0 ? (byteUsed + ' ' + GuiText.BytesUsed.getLocal()) : GuiText.CalculatingWait.getLocal();
        this.fontRenderer.drawString(GuiText.CraftingPlan.getLocal() + " - " + Add, 8, 7, 4210752);

        String dsp = null;

        if (this.isSimulation()) {
            dsp = GuiText.Simulation.getLocal();
        } else {
            dsp = this.ccc.getCpuAvailableBytes() > 0 ? (GuiText.Bytes.getLocal() + ": " + this.ccc.getCpuAvailableBytes() + " : " + GuiText.CoProcessors
                    .getLocal() + ": " + this.ccc.getCpuCoProcessors()) : GuiText.Bytes.getLocal() + ": N/A : " + GuiText.CoProcessors.getLocal() + ": N/A";
        }

        final int offset = (219 - this.fontRenderer.getStringWidth(dsp)) / 2;
        this.fontRenderer.drawString(dsp, offset, 165, 4210752);

        final int sectionLength = 67;

        int x = 0;
        int y = 0;
        final int xo = 9;
        final int yo = 22;
        final int viewStart = this.getScrollBar().getCurrentScroll() * 3;
        final int viewEnd = viewStart + 3 * this.rows;

        String dspToolTip = "";
        final List<String> lineList = new ArrayList<>();
        int toolPosX = 0;
        int toolPosY = 0;

        final int offY = 23;

        for (int z = viewStart; z < Math.min(viewEnd, this.visual.size()); z++) {
            final IExAEStack<?> refStack = this.visual.get(z);// repo.getReferenceItem( z );
            if (refStack != null) {
                boolean red = drawStackDecoration(refStack, x, y, xo, yo, offY, z, viewStart, sectionLength, lineList);

                final int posX = x * (1 + sectionLength) + xo + sectionLength - 19;
                final int posY = y * offY + yo;

                final ItemStack is = refStack.asItemStackRepresentation();

                if (this.tooltip == z - viewStart) {
                    dspToolTip = Platform.getItemDisplayName(is);

                    if (lineList.size() > 0) {
                        dspToolTip = dspToolTip + '\n' + Joiner.on("\n").join(lineList);
                    }

                    toolPosX = x * (1 + sectionLength) + xo + sectionLength - 8;
                    toolPosY = y * offY + yo;
                }

                this.drawItem(posX, posY, is);

                if (red) {
                    final int startX = x * (1 + sectionLength) + xo;
                    final int startY = posY - 4;
                    drawRect(startX, startY, startX + sectionLength, startY + offY, 0x1AFF0000);
                }

                x++;

                if (x > 2) {
                    y++;
                    x = 0;
                }
            }
        }

        if (this.tooltip >= 0 && !dspToolTip.isEmpty()) {
            this.drawTooltip(toolPosX, toolPosY + 10, dspToolTip);
        }
    }

    private <T extends IAEStack<T>> boolean drawStackDecoration(final IExAEStack<T> stack, final int x, final int y, final int xo, final int yo,
                                                                final int offY, final int z, final int viewStart, final int sectionLength,
                                                                final List<String> lineList) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5, 0.5, 0.5);

        final T refStack = stack.unwrap();
        final T stored = this.storage.findPrecise(refStack);
        final T pendingStack = this.pending.findPrecise(refStack);
        final T missingStack = this.missing.findPrecise(refStack);

        int lines = 0;

        if (stored != null && stored.getStackSize() > 0) {
            lines++;
        }
        if (missingStack != null && missingStack.getStackSize() > 0) {
            lines++;
        }
        if (pendingStack != null && pendingStack.getStackSize() > 0) {
            lines++;
        }

        final int negY = ((lines - 1) * 5) / 2;
        int downY = 0;

        if (stored != null && stored.getStackSize() > 0) {
            String str = Long.toString(stored.getStackSize());
            if (stored.getStackSize() >= 10000) {
                str = Long.toString(stored.getStackSize() / 1000) + 'k';
            }
            if (stored.getStackSize() >= 10000000) {
                str = Long.toString(stored.getStackSize() / 1000000) + 'm';
            }

            str = GuiText.FromStorage.getLocal() + ": " + str;
            final int w = 4 + this.fontRenderer.getStringWidth(str);
            this.fontRenderer.drawString(str, (int) ((x * (1 + sectionLength) + xo + sectionLength - 19 - (w * 0.5)) * 2),
                    (y * offY + yo + 6 - negY + downY) * 2, 4210752);

            if (this.tooltip == z - viewStart) {
                lineList.add(GuiText.FromStorage.getLocal() + ": " + stored.getStackSize());
            }

            downY += 5;
        }

        boolean red = false;
        if (missingStack != null && missingStack.getStackSize() > 0) {
            String str = Long.toString(missingStack.getStackSize());
            if (missingStack.getStackSize() >= 10000) {
                str = Long.toString(missingStack.getStackSize() / 1000) + 'k';
            }
            if (missingStack.getStackSize() >= 10000000) {
                str = Long.toString(missingStack.getStackSize() / 1000000) + 'm';
            }

            str = GuiText.Missing.getLocal() + ": " + str;
            final int w = 4 + this.fontRenderer.getStringWidth(str);
            this.fontRenderer.drawString(str, (int) ((x * (1 + sectionLength) + xo + sectionLength - 19 - (w * 0.5)) * 2),
                    (y * offY + yo + 6 - negY + downY) * 2, 4210752);

            if (this.tooltip == z - viewStart) {
                lineList.add(GuiText.Missing.getLocal() + ": " + missingStack.getStackSize());
            }

            red = true;
            downY += 5;
        }

        if (pendingStack != null && pendingStack.getStackSize() > 0) {
            String str = Long.toString(pendingStack.getStackSize());
            if (pendingStack.getStackSize() >= 10000) {
                str = Long.toString(pendingStack.getStackSize() / 1000) + 'k';
            }
            if (pendingStack.getStackSize() >= 10000000) {
                str = Long.toString(pendingStack.getStackSize() / 1000000) + 'm';
            }

            str = GuiText.ToCraft.getLocal() + ": " + str;
            final int w = 4 + this.fontRenderer.getStringWidth(str);
            this.fontRenderer.drawString(str, (int) ((x * (1 + sectionLength) + xo + sectionLength - 19 - (w * 0.5)) * 2),
                    (y * offY + yo + 6 - negY + downY) * 2, 4210752);

            if (this.tooltip == z - viewStart) {
                lineList.add(GuiText.ToCraft.getLocal() + ": " + pendingStack.getStackSize());
            }
        }

        GlStateManager.popMatrix();
        return red;
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.setScrollBar();
        this.bindTexture("guis/craftingreport.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    private void setScrollBar() {
        final int size = this.visual.size();

        this.getScrollBar().setTop(19).setLeft(218).setHeight(114);
        this.getScrollBar().setRange(0, (size + 2) / 3 - this.rows, 1);
    }

    public void postUpdate(final List<IExAEStack<?>> list, final byte ref) {
        switch (ref) {
            case 0:
                for (final IExAEStack<?> l : list) {
                    this.handleInput(this.storage, l);
                }
                break;

            case 1:
                for (final IExAEStack<?> l : list) {
                    this.handleInput(this.pending, l);
                }
                break;

            case 2:
                for (final IExAEStack<?> l : list) {
                    this.handleInput(this.missing, l);
                }
                break;
        }

        for (final IExAEStack<?> l : list) {
            if (l == null) {
                continue;
            }

            final long amt = this.getTotal(l);

            if (amt <= 0) {
                this.deleteVisualStack(l);
            } else {
                final IExAEStack<?> is = this.findVisualStack(l);
                is.setStackSize(amt);
            }
        }

        this.setScrollBar();
    }

    private <T extends IAEStack<T>> void handleInput(final IUnivItemList s, final IExAEStack<T> stack) {
        if (stack == null) {
            return;
        }

        final T l = stack.unwrap();
        T a = s.findPrecise(l);

        if (l.getStackSize() <= 0) {
            if (a != null) {
                a.reset();
            }
        } else {
            if (a == null) {
                s.add(l.copy());
                a = s.findPrecise(l);
            }

            if (a != null) {
                a.setStackSize(l.getStackSize());
            }
        }
    }

    private <T extends IAEStack<T>> long getTotal(final IExAEStack<T> stack) {
        final T is = stack.unwrap();
        final T a = this.storage.findPrecise(is);
        final T c = this.pending.findPrecise(is);
        final T m = this.missing.findPrecise(is);

        long total = 0;

        if (a != null) {
            total += a.getStackSize();
        }

        if (c != null) {
            total += c.getStackSize();
        }

        if (m != null) {
            total += m.getStackSize();
        }

        return total;
    }

    private void deleteVisualStack(final IExAEStack<?> l) {
        final Iterator<IExAEStack<?>> i = this.visual.iterator();
        while (i.hasNext()) {
            final IExAEStack<?> o = i.next();
            if (o.equals(l)) {
                i.remove();
                return;
            }
        }
    }

    private IExAEStack<?> findVisualStack(final IExAEStack<?> l) {
        for (final IExAEStack<?> o : this.visual) {
            if (o.equals(l)) {
                return o;
            }
        }

        final IExAEStack<?> stack = l.copy();
        this.visual.add(stack);
        return stack;
    }

    @Override
    protected void keyTyped(final char character, final int key) throws IOException {
        if (!this.checkHotbarKeys(key)) {
            if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                this.actionPerformed(this.start);
            }
            super.keyTyped(character, key);
        }
    }

    @Override
    protected void actionPerformed(final GuiButton btn) throws IOException {
        super.actionPerformed(btn);

        final boolean backwards = Mouse.isButtonDown(1);

        if (btn == this.selectCPU) {
            try {
                NetworkHandler.instance().sendToServer(new PacketValueConfig("Terminal.Cpu", backwards ? "Prev" : "Next"));
            } catch (final IOException e) {
                AELog.debug(e);
            }
        }

        if (btn == this.cancel && this.OriginalGui != null) {
            NetworkHandler.instance().sendToServer(new PacketSwitchGuis(this.OriginalGui));
        }

        if (btn == this.start) {
            try {
                NetworkHandler.instance().sendToServer(new PacketValueConfig("Terminal.Start", "Start"));
            } catch (final Throwable e) {
                AELog.debug(e);
            }
        }
    }

    public List<IExAEStack<?>> getVisual() {
        return visual;
    }

    public int getDisplayedRows() {
        return this.rows;
    }
}
