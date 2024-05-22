package appeng.integration.modules.jei;

import appeng.api.storage.IStorageChannel;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.recipe.IIngredientType;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;


public class JeiStackTypeTable {

    private final IIngredientRegistry registry;
    private final Map<IStorageChannel<?>, Optional<IIngredientType<?>>> table = new IdentityHashMap<>();

    public JeiStackTypeTable(final IModRegistry registry) {
        this.registry = registry.getIngredientRegistry();
    }

    public IIngredientType<?> getIngredientType(IStorageChannel<?> channel) {
        return this.table.computeIfAbsent(channel, this::computeIngredientType).orElse(null);
    }

    private Optional<IIngredientType<?>> computeIngredientType(IStorageChannel<?> channel) {
        try {
            return Optional.of(registry.getIngredientType(channel.getUnderlyingStackType()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}
