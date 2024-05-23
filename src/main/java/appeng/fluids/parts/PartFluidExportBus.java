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

package appeng.fluids.parts;


import appeng.api.config.*;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.core.AELog;
import appeng.core.AppEng;
import appeng.core.settings.TickRates;
import appeng.helpers.MultiCraftingTracker;
import appeng.items.parts.PartModels;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.PartModel;
import appeng.util.Platform;
import appeng.util.inv.AdaptorFluidHandler;
import com.google.common.collect.ImmutableSet;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;


/**
 * @author BrockWS
 * @version rv6 - 30/04/2018
 * @since rv6 30/04/2018
 */
public class PartFluidExportBus extends PartSharedFluidBus implements ICraftingRequester {
    public static final ResourceLocation MODEL_BASE = new ResourceLocation(AppEng.MOD_ID, "part/fluid_export_bus_base");
    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, new ResourceLocation(AppEng.MOD_ID, "part/fluid_export_bus_off"));
    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, new ResourceLocation(AppEng.MOD_ID, "part/fluid_export_bus_on"));
    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, new ResourceLocation(AppEng.MOD_ID, "part/fluid_export_bus_has_channel"));

    private final IActionSource source;

    private final MultiCraftingTracker craftingTracker = new MultiCraftingTracker(this, 9);

    public PartFluidExportBus(ItemStack is) {
        super(is);
        this.getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        this.getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.getConfigManager().registerSetting(Settings.CRAFT_ONLY, YesNo.NO);
        this.getConfigManager().registerSetting(Settings.SCHEDULING_MODE, SchedulingMode.DEFAULT);
        this.source = new MachineSource(this);
    }

    @Override
    public void readFromNBT(final NBTTagCompound extra) {
        super.readFromNBT(extra);
        this.craftingTracker.readFromNBT(extra);
    }

    @Override
    public void writeToNBT(final NBTTagCompound extra) {
        super.writeToNBT(extra);
        this.craftingTracker.writeToNBT(extra);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.FluidExportBus.getMin(), TickRates.FluidExportBus.getMax(), this.isSleeping(), false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        return this.canDoBusWork() ? this.doBusWork() : TickRateModulation.IDLE;
    }

    @Override
    protected boolean canDoBusWork() {
        return this.getProxy().isActive();
    }

    @Override
    protected TickRateModulation doBusWork() {
        if (!this.canDoBusWork()) {
            return TickRateModulation.IDLE;
        }

        final IFluidHandler fh = this.getHandler();
        if (fh != null) {
            try {
                final World world = this.getTile().getWorld();
                final IGrid g = this.getProxy().getGrid();
                final ICraftingGrid cg = this.getProxy().getCrafting();
                final IEnergyGrid eg = this.getProxy().getEnergy();
                final IMEMonitor<IAEFluidStack> inv = this.getProxy().getStorage().getInventory(this.getChannel());
                final IMEInventory<IAEFluidStack> dest = new AdaptorFluidHandler(fh);

                final boolean craftingEnabled = this.getInstalledUpgrades(Upgrades.CRAFTING) > 0;
                final boolean craftOnly = this.getConfigManager().getSetting(Settings.CRAFT_ONLY) == YesNo.YES;
                long sendQuota = this.calculateAmountToSend();
                boolean didSomething = false;

                for (int i = 0; sendQuota > 0 && i < this.getConfig().getSlots(); i++) {
                    final IAEFluidStack fluid = this.getConfig().getFluidInSlot(i);
                    if (fluid == null) {
                        continue;
                    }

                    if (craftOnly) {
                        if (craftingEnabled) {
                            didSomething |= this.craftingTracker.handleCrafting(i, sendQuota, fluid, dest, world, g, cg, this.source);
                        }
                        continue;
                    }

                    final long transferred = pushFluidIntoTarget(fluid.copy().setStackSize(sendQuota), inv, dest, eg);

                    if (transferred > 0) {
                        sendQuota -= transferred;
                        didSomething = true;
                    } else if (craftingEnabled) {
                        didSomething |= this.craftingTracker.handleCrafting(i, sendQuota, fluid, dest, world, g, cg, this.source);
                    }
                }

                return didSomething ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
            } catch (GridAccessException e) {
                // Ignore
            }
        }

        return TickRateModulation.SLEEP;
    }

    private long pushFluidIntoTarget(final IAEFluidStack request, final IMEMonitor<IAEFluidStack> inv, final IMEInventory<IAEFluidStack> dest, final IEnergyGrid eg) {
        final IAEFluidStack tFluid = inv.extractItems(request, Actionable.SIMULATE, this.source);
        if (tFluid == null) {
            return 0;
        }

        final IAEFluidStack tRem = dest.injectItems(tFluid, Actionable.SIMULATE, this.source);
        if (tRem != null && tRem.getStackSize() >= tFluid.getStackSize()) {
            return 0;
        }

        final IAEFluidStack tTfr = tRem == null ? tFluid : tFluid.copy().setStackSize(tFluid.getStackSize() - tRem.getStackSize());
        final IAEFluidStack fluid = Platform.poweredExtraction(eg, inv, tTfr, this.source);
        if (fluid == null) {
            return 0;
        }

        final IAEFluidStack rem = dest.injectItems(fluid, Actionable.MODULATE, this.source);
        if (rem == null) {
            return fluid.getStackSize();
        } else {
            inv.injectItems(rem, Actionable.MODULATE, this.source);
            return Math.max(fluid.getStackSize() - rem.getStackSize(), 0);
        }
    }

    @Override
    public void getBoxes(final IPartCollisionHelper bch) {
        bch.addBox(4, 4, 12, 12, 12, 14);
        bch.addBox(5, 5, 14, 11, 11, 15);
        bch.addBox(6, 6, 15, 10, 10, 16);
        bch.addBox(6, 6, 11, 10, 10, 12);
    }

    @Override
    public RedstoneMode getRSMode() {
        return (RedstoneMode) this.getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED);
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.craftingTracker.getRequestedJobs();
    }

    @Override
    public <T extends IAEStack<T>> T injectCraftedItems(final ICraftingLink link, final T i, final Actionable mode) {
        if (!(i instanceof final IAEFluidStack fluid)) {
            return i;
        }

        final IFluidHandler d = this.getHandler();

        try {
            if (d != null && this.getProxy().isActive()) {
                final IEnergyGrid energy = this.getProxy().getEnergy();
                final double power = fluid.getStackSize() / Math.max(1.0, fluid.getChannel().transferFactor());

                if (energy.extractAEPower(power, mode, PowerMultiplier.CONFIG) > power - 0.01) {
                    final int transferred = d.fill(fluid.getFluidStack(), mode == Actionable.MODULATE);
                    return (T) fluid.copy().setStackSize(fluid.getStackSize() - transferred);
                }
            }
        } catch (final GridAccessException e) {
            AELog.debug(e);
        }

        return i;
    }

    @Override
    public void jobStateChange(final ICraftingLink link) {
        this.craftingTracker.jobStateChange(link);
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return MODELS_ON;
        } else {
            return MODELS_OFF;
        }
    }
}
