package appeng.parts.networking;

import appeng.api.util.AECableType;
import net.minecraft.item.ItemStack;

public class PartUltraDenseCableCovered extends PartUltraDenseCable {

    public PartUltraDenseCableCovered( ItemStack is )
    {
        super( is );
    }

    @Override
    public AECableType getCableConnectionType()
    {
        return AECableType.ULTRA_DENSE_COVERED;
    }
}
