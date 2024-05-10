package appeng.recipes.handlers;


import appeng.api.AEApi;
import appeng.api.features.IInscriberRecipeBuilder;
import appeng.api.features.IInscriberRegistry;
import appeng.api.features.InscriberProcessType;
import appeng.recipes.IAERecipeFactory;
import appeng.recipes.factories.recipes.PartRecipeFactory;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class InscriberHandler implements IAERecipeFactory {

    @Override
    public void register(JsonObject json, JsonContext ctx) {
        ItemStack result = PartRecipeFactory.getResult(json, ctx);
        String mode = JsonUtils.getString(json, "mode");

        JsonObject ingredients = JsonUtils.getJsonObject(json, "ingredients");

        List<ItemStack> middle = Arrays.asList(CraftingHelper.getIngredient(ingredients.get("middle"), ctx).getMatchingStacks());
        List<ItemStack> top = Collections.emptyList();
        if (ingredients.has("top")) {
            top = Arrays.asList(CraftingHelper.getIngredient(JsonUtils.getJsonObject(ingredients, "top"), ctx).getMatchingStacks());
        }

        List<ItemStack> bottom = Collections.emptyList();
        if (ingredients.has("bottom")) {
            bottom = Arrays.asList(CraftingHelper.getIngredient(JsonUtils.getJsonObject(ingredients, "bottom"), ctx).getMatchingStacks());
        }

        final IInscriberRegistry reg = AEApi.instance().registries().inscriber();
        if (!top.isEmpty() || !bottom.isEmpty()) {
            final IInscriberRecipeBuilder builder = reg.builder();
            builder.withOutput(result);
            builder.withProcessType("press".equals(mode) ? InscriberProcessType.PRESS : InscriberProcessType.INSCRIBE);
            builder.withTopOptional(top);
            builder.withInputs(middle);
            builder.withBottomOptional(bottom);
            reg.addRecipe(builder.build());
        }
    }
}
