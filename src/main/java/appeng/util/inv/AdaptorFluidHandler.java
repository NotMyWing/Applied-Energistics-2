package appeng.util.inv;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import appeng.fluids.util.AEFluidStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;


public class AdaptorFluidHandler implements IMEInventory<IAEFluidStack> {

    private final IFluidHandler fluidHandler;

    public AdaptorFluidHandler(final IFluidHandler fluidHandler) {
        this.fluidHandler = fluidHandler;
    }

    @Override
    public IAEFluidStack injectItems(final IAEFluidStack input, final Actionable type, final IActionSource src) {
        final int filled = this.fluidHandler.fill(input.getFluidStack(), type == Actionable.MODULATE);
        if (filled >= input.getStackSize()) {
            return null;
        }
        return input.copy().setStackSize(input.getStackSize() - filled);
    }

    @Override
    public IAEFluidStack extractItems(final IAEFluidStack request, final Actionable mode, final IActionSource src) {
        final FluidStack drained = this.fluidHandler.drain(request.getFluidStack(), mode == Actionable.MODULATE);
        if (drained == null || drained.amount <= 0) {
            return null;
        }
        return AEFluidStack.fromFluidStack(drained);
    }

    @Override
    public IItemList<IAEFluidStack> getAvailableItems(final IItemList<IAEFluidStack> out) {
        for (final IFluidTankProperties tank : this.fluidHandler.getTankProperties()) {
            out.add(AEFluidStack.fromFluidStack(tank.getContents()));
        }
        return out;
    }

    @Override
    public IStorageChannel<IAEFluidStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
    }

}
