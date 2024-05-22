package appeng.util;

import appeng.api.storage.IMEInventory;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;


public class InventoryAdaptationCache {

    private final Object inv;
    private final ICapabilityProvider caps;
    private final EnumFacing side;
    private final Map<IStorageChannel<?>, Optional<IMEInventory<?>>> cache = new IdentityHashMap<>();

    public InventoryAdaptationCache(final Object inv, final ICapabilityProvider caps, final EnumFacing side) {
        this.inv = inv;
        this.caps = caps;
        this.side = side;
    }

    @SuppressWarnings("unchecked")
    public <T extends IAEStack<T>> IMEInventory<T> getAdaptor(IStorageChannel<T> channel) {
        return (IMEInventory<T>) cache.computeIfAbsent(channel, c -> Optional.ofNullable(c.adaptInventory(inv, caps, side))).orElse(null);
    }

}
