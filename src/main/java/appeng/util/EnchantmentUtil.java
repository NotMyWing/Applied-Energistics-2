package appeng.util;

import com.google.common.collect.Maps;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class EnchantmentUtil {
    private EnchantmentUtil() {
    }

    public static Map<Enchantment, Integer> deserializeEnchantments(NBTTagList tagList) {
        Map<Enchantment, Integer> map = Maps.newLinkedHashMap();

        for(int i = 0; i < tagList.tagCount(); ++i) {
            NBTTagCompound compoundtag = tagList.getCompoundTagAt(i);
            Enchantment enchantmentByID = Enchantment.getEnchantmentByID(getEnchantmentId(compoundtag));
            if (enchantmentByID != null) {
                map.put(enchantmentByID, compoundtag.getInteger("lvl"));
            }
        }

        return map;
    }

    public static int getEnchantmentId(NBTTagCompound tagCompound) {
        return tagCompound.getInteger("id");
    }

    public static int getEnchantmentLevel(NBTTagCompound tagCompound) {
        return Math.max(0, Math.min(255, tagCompound.getInteger("lvl")));
    }

    public static NBTTagCompound storeEnchantment(int id, int level) {
        NBTTagCompound compoundtag = new NBTTagCompound();
        compoundtag.setInteger("id",id);
        compoundtag.setShort("lvl", ((short) level));
        return compoundtag;
    }
    /**
     * Read enchants written using {@link #setEnchantments} or added to an itemstack's tag using normal enchanting.
     *
     * @return null if no enchants are present
     */
    @Nullable
    public static Map<Enchantment, Integer> getEnchantments(NBTTagCompound data) {
        if (data.hasKey("ench",9)) {
            var list = data.getTagList("ench",10);
            var enchants = deserializeEnchantments(list);
            if (!enchants.isEmpty()) {
                return enchants;
            }
        }
        return null;
    }

    /**
     * Writes a list of enchantments to the given tag the same way as
     * {@link EnchantmentHelper#setEnchantments(Map, ItemStack)} would.
     */
    public static void setEnchantments(NBTTagCompound tag, Map<Enchantment, Integer> enchantments) {
        NBTTagList enchantList = new NBTTagList();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment == null)
                continue;
            int level = entry.getValue();
            enchantList.appendTag(storeEnchantment(Enchantment.getEnchantmentID(enchantment), level));
        }
        tag.setTag("Enchantments", enchantList);
    }
}
