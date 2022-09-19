package appeng.tile.crafting;


import appeng.api.AEApi;
import appeng.api.definitions.IBlocks;
import appeng.block.crafting.BlockCraftingUnit;
import net.minecraft.item.ItemStack;

import java.util.Optional;

public class TileCraftingUnitTile extends TileCraftingTile {

    @Override
    protected ItemStack getItemFromTile(final Object obj) {
        final IBlocks blocks = AEApi.instance().definitions().blocks();
        final int storage = ((TileCraftingTile) obj).getAcceleration();

        Optional<ItemStack> is;

        switch (storage) {
            case 1:
                is = blocks.craftingAccelerator1().maybeStack(1);
                break;
            case 4:
                is = blocks.craftingAccelerator4().maybeStack(1);
                break;
            case 16:
                is = blocks.craftingAccelerator16().maybeStack(1);
                break;
            case 64:
                is = blocks.craftingAccelerator64().maybeStack(1);
                break;
            default:
                is = Optional.empty();
                break;
        }

        return is.orElseGet(() -> super.getItemFromTile(obj));
    }

    @Override
    public boolean isAccelerator() {
        return true;
    }

    @Override
    public boolean isStorage() {
        return false;
    }

    @Override
    public int getAcceleration() {
        if(this.world == null || this.notLoaded() || this.isInvalid()){
            return 0;
        }
        final BlockCraftingUnit unit = (BlockCraftingUnit) this.world.getBlockState( this.pos ).getBlock();

        switch (unit.type){
            default:
            case ACCELERATOR_1:
                return 1;
            case ACCELERATOR_4:
                return 4;
            case ACCELERATOR_16:
                return 16;
            case ACCELERATOR_64:
                return 64;
        }
    }
}
