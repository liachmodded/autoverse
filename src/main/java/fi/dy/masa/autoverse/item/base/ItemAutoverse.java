package fi.dy.masa.autoverse.item.base;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.gui.client.CreativeTab;
import fi.dy.masa.autoverse.reference.ReferenceNames;

public class ItemAutoverse extends Item
{
    public static final String PRE_BLUE = TextFormatting.BLUE.toString();
    public static final String PRE_GREEN = TextFormatting.GREEN.toString();
    public static final String RST_GRAY = TextFormatting.RESET.toString() + TextFormatting.GRAY.toString();
    public static final String RST_WHITE = TextFormatting.RESET.toString() + TextFormatting.WHITE.toString();

    protected String name;
    protected String commonTooltip = null;
    protected boolean enabled = true;

    public ItemAutoverse()
    {
        super();
        this.setCreativeTab(CreativeTab.AUTOVERSE_TAB);
        this.addItemOverrides();
    }

    @Override
    public Item setUnlocalizedName(String name)
    {
        this.name = name;
        return super.setUnlocalizedName(ReferenceNames.getDotPrefixedName(name));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged || ItemStack.areItemStacksEqual(oldStack, newStack) == false;
    }

    public String getBaseItemDisplayName(ItemStack stack)
    {
        // If the item has been renamed, show that name
        if (stack.hasDisplayName())
        {
            String name = stack.getTagCompound().getCompoundTag("display").getString("Name");
            return TextFormatting.ITALIC.toString() + name + TextFormatting.RESET.toString();
        }

        return super.getItemStackDisplayName(stack);
    }

    /**
     * Custom addInformation() method, which allows selecting a subset of the tooltip strings.
     */
    public void addInformationSelective(ItemStack stack, EntityPlayer player, List<String> list, boolean advancedTooltips, boolean verbose)
    {
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean advancedTooltips)
    {
        ArrayList<String> tmpList = new ArrayList<String>();
        boolean verbose = Autoverse.proxy.isShiftKeyDown();

        // "Fresh" items without NBT data: display the tips before the usual tooltip data
        if (stack.getTagCompound() == null)
        {
            this.addTooltips(stack, tmpList, verbose);

            if (verbose == false && tmpList.size() > 2)
            {
                list.add(I18n.format("enderutilities.tooltip.item.holdshiftfordescription"));
            }
            else
            {
                list.addAll(tmpList);
            }
        }

        tmpList.clear();
        this.addInformationSelective(stack, player, tmpList, advancedTooltips, true);

        // If we want the compact version of the tooltip, and the compact list has more than 2 lines, only show the first line
        // plus the "Hold Shift for more" tooltip.
        if (verbose == false && tmpList.size() > 2)
        {
            tmpList.clear();
            this.addInformationSelective(stack, player, tmpList, advancedTooltips, false);
            if (tmpList.size() > 0)
            {
                list.add(tmpList.get(0));
            }
            list.add(I18n.format("enderutilities.tooltip.item.holdshift"));
        }
        else
        {
            list.addAll(tmpList);
        }
    }

    public static void addTooltips(String key, List<String> list, boolean verbose, Object... args)
    {
        String translated = I18n.format(key, args);

        // Translation found
        if (translated.equals(key) == false)
        {
            // We currently use '|lf' as a delimiter to split the string into multiple lines
            if (translated.contains("|lf"))
            {
                String[] lines = translated.split(Pattern.quote("|lf"));
                for (String line : lines)
                {
                    list.add(line);
                }
            }
            else
            {
                list.add(translated);
            }
        }
    }

    public void addTooltips(ItemStack stack, List<String> list, boolean verbose)
    {
        addTooltips(this.getUnlocalizedName(stack) + ".tooltips", list, verbose);

        if (this.commonTooltip != null)
        {
            addTooltips(this.commonTooltip, list, verbose);
        }
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public ItemAutoverse setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        return this;
    }

    @SideOnly(Side.CLIENT)
    public ResourceLocation[] getItemVariants()
    {
        return new ResourceLocation[] { ForgeRegistries.ITEMS.getKey(this) };
    }

    @SideOnly(Side.CLIENT)
    public ModelResourceLocation getModelLocation(ItemStack stack)
    {
        return null;
    }

    protected void addItemOverrides()
    {
    }
}