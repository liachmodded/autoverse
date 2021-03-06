package fi.dy.masa.autoverse.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.autoverse.Autoverse;
import fi.dy.masa.autoverse.item.block.ItemBlockAutoverse;
import fi.dy.masa.autoverse.network.PacketHandler;
import fi.dy.masa.autoverse.network.message.MessageSyncNBTTag;
import fi.dy.masa.autoverse.reference.Reference;

public class PlacementProperties
{
    private static final String FILE_NAME_BASE = "placement_properties";
    private static PlacementProperties instance;
    private Map<UUID, Map<ItemType, NBTTagCompound>> properties = new HashMap<UUID, Map<ItemType, NBTTagCompound>>();
    private Map<UUID, Map<ItemType, Integer>> indices = new HashMap<UUID, Map<ItemType, Integer>>();
    private boolean dirty = false;

    public static PlacementProperties getInstance()
    {
        if (instance == null)
        {
            instance = new PlacementProperties();
        }

        return instance;
    }

    public int getPropertyIndex(UUID uuid, ItemType type)
    {
        Map<ItemType, Integer> map = this.indices.get(uuid);

        if (map != null)
        {
            Integer value = map.get(type);
            return value != null ? value : 0;
        }

        return 0;
    }

    public void setPropertyIndex(UUID uuid, ItemType type, int index)
    {
        Map<ItemType, Integer> map = this.indices.get(uuid);

        if (map == null)
        {
            map = new HashMap<ItemType, Integer>();
            this.indices.put(uuid, map);
        }

        map.put(type, index);
        this.dirty = true;
    }

    public void setPropertyValue(UUID uuid, ItemType itemType, PlacementProperty property, int index, int value)
    {
        Pair<String, Integer> pair = property.getProperty(index);

        if (pair != null)
        {
            String key = pair.getLeft();
            Integer valueType = pair.getRight();
            NBTTagCompound tag = this.getOrCreatePropertyTag(uuid, itemType);

            switch (valueType)
            {
                case Constants.NBT.TAG_BYTE:
                    tag.setByte(key, (byte) value);
                    break;

                case Constants.NBT.TAG_SHORT:
                    tag.setShort(key, (short) value);
                    break;

                case Constants.NBT.TAG_INT:
                    tag.setInteger(key, value);
                    break;

                default:
            }

            this.dirty = true;
        }
    }

    public int getPropertyValue(UUID uuid, ItemType itemType, PlacementProperty property, int index)
    {
        Pair<String, Integer> pair = property.getProperty(index);

        if (pair != null)
        {
            String key = pair.getLeft();
            Integer valueType = pair.getRight();
            NBTTagCompound tag = this.getPropertyTag(uuid, itemType);

            if (tag != null && tag.hasKey(key))
            {
                switch (valueType)
                {
                    case Constants.NBT.TAG_BYTE:    return ((int) tag.getByte(key)) & 0xFF;
                    case Constants.NBT.TAG_SHORT:   return ((int) tag.getShort(key)) & 0xFFFF;
                    case Constants.NBT.TAG_INT:     return tag.getInteger(key);
                }
            }
            else
            {
                Integer defaultValue = property.getDefaultValue(index);

                if (defaultValue != null)
                {
                    int value = defaultValue.intValue();

                    switch (valueType)
                    {
                        case Constants.NBT.TAG_BYTE:    return ((int) value) & 0xFF;
                        case Constants.NBT.TAG_SHORT:   return ((int) value) & 0xFFFF;
                        case Constants.NBT.TAG_INT:     return value;
                    }
                }
            }
        }

        return 0;
    }

    @Nullable
    public NBTTagCompound getPropertyTag(UUID uuid, ItemType type)
    {
        Map<ItemType, NBTTagCompound> map = this.properties.get(uuid);
        return map != null ? map.get(type) : null;
    }

    @Nonnull
    private NBTTagCompound getOrCreatePropertyTag(UUID uuid, ItemType type)
    {
        Map<ItemType, NBTTagCompound> map = this.properties.get(uuid);

        if (map == null)
        {
            map = new HashMap<ItemType, NBTTagCompound>();
            this.properties.put(uuid, map);
        }

        NBTTagCompound tag = map.get(type);

        if (tag == null)
        {
            tag = new NBTTagCompound();
            map.put(type, tag);
        }

        return tag;
    }

    public void readFromDisk()
    {
        // Clear the data structures when reading the data for a world/save, so that data
        // from another world won't carry over to a world/save that doesn't have the file yet.
        this.properties.clear();
        this.indices.clear();

        try
        {
            File saveDir = DimensionManager.getCurrentSaveRootDirectory();

            if (saveDir == null)
            {
                return;
            }

            File file = new File(new File(saveDir, Reference.MOD_ID), FILE_NAME_BASE + ".dat");

            if (file.exists() && file.isFile())
            {
                this.readFromNBT(CompressedStreamTools.readCompressed(new FileInputStream(file)));
            }
        }
        catch (Exception e)
        {
            Autoverse.logger.warn("Failed to read Placement Properties data from file");
        }
    }

    public void writeToDisk()
    {
        if (this.dirty == false)
        {
            return;
        }

        try
        {
            File saveDir = DimensionManager.getCurrentSaveRootDirectory();

            if (saveDir == null)
            {
                return;
            }

            saveDir = new File(saveDir, Reference.MOD_ID);

            if (saveDir.exists() == false && saveDir.mkdirs() == false)
            {
                Autoverse.logger.warn("Failed to create the save directory '{}'", saveDir.toString());
                return;
            }

            File fileTmp = new File(saveDir, FILE_NAME_BASE + ".dat.tmp");
            File fileReal = new File(saveDir, FILE_NAME_BASE + ".dat");
            CompressedStreamTools.writeCompressed(this.writeToNBT(new NBTTagCompound()), new FileOutputStream(fileTmp));

            if (fileReal.exists())
            {
                fileReal.delete();
            }

            fileTmp.renameTo(fileReal);
            this.dirty = false;
        }
        catch (Exception e)
        {
            Autoverse.logger.warn("Failed to write Placement Properties data to file", e);
        }
    }

    private void readFromNBT(NBTTagCompound nbt)
    {
        if (nbt == null || nbt.hasKey("PlacementProperties", Constants.NBT.TAG_LIST) == false)
        {
            return;
        }

        NBTTagList tagListPlayers = nbt.getTagList("PlacementProperties", Constants.NBT.TAG_COMPOUND);
        int countPlayers = tagListPlayers.tagCount();

        for (int playerIndex = 0; playerIndex < countPlayers; playerIndex++)
        {
            this.readAllDataForPlayerFromNBT(tagListPlayers.getCompoundTagAt(playerIndex));
        }
    }

    public void readAllDataForPlayerFromNBT(NBTTagCompound tagPlayer)
    {
        if (tagPlayer.hasKey("UUID", Constants.NBT.TAG_STRING) && tagPlayer.hasKey("Data", Constants.NBT.TAG_LIST))
        {
            UUID uuid = UUID.fromString(tagPlayer.getString("UUID"));
            NBTTagList tagListData = tagPlayer.getTagList("Data", Constants.NBT.TAG_COMPOUND);
            int countData = tagListData.tagCount();
            Map<ItemType, NBTTagCompound> mapTags = new HashMap<ItemType, NBTTagCompound>();
            Map<ItemType, Integer> mapIndices = new HashMap<ItemType, Integer>();
            this.properties.put(uuid, mapTags);
            this.indices.put(uuid, mapIndices);

            for (int dataIndex = 0; dataIndex < countData; dataIndex++)
            {
                this.readDataForItemTypeFromNBT(tagListData.getCompoundTagAt(dataIndex), mapTags, mapIndices);
            }
        }
    }

    private void readDataForItemTypeFromNBT(NBTTagCompound tagData, Map<ItemType, NBTTagCompound> mapTags, Map<ItemType, Integer> mapIndices)
    {
        if (tagData.hasKey("ItemType", Constants.NBT.TAG_COMPOUND) && tagData.hasKey("Tag", Constants.NBT.TAG_COMPOUND))
        {
            ItemStack stack = new ItemStack(tagData.getCompoundTag("ItemType"));

            if (stack.isEmpty() == false && stack.getItem() instanceof ItemBlockAutoverse)
            {
                ItemBlockAutoverse item = (ItemBlockAutoverse) stack.getItem();

                if (item.hasPlacementProperty(stack))
                {
                    boolean nbtSensitive = item.getPlacementProperty(stack).isNBTSensitive();
                    ItemType type = new ItemType(stack, nbtSensitive);
                    mapTags.put(type, tagData.getCompoundTag("Tag"));

                    if (tagData.hasKey("Index", Constants.NBT.TAG_BYTE))
                    {
                        mapIndices.put(type, Integer.valueOf(tagData.getByte("Index")));
                    }
                }
            }
        }
    }

    public void readSyncedItemData(UUID uuid, NBTTagCompound tag)
    {
        Map<ItemType, NBTTagCompound> mapTags = this.properties.get(uuid);
        Map<ItemType, Integer> mapIndices = this.indices.get(uuid);

        if (mapTags == null)
        {
            mapTags = new HashMap<ItemType, NBTTagCompound>();
            this.properties.put(uuid, mapTags);
        }

        if (mapIndices == null)
        {
            mapIndices = new HashMap<ItemType, Integer>();
            this.indices.put(uuid, mapIndices);
        }

        this.readDataForItemTypeFromNBT(tag, mapTags, mapIndices);
    }

    private NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        NBTTagList tagListPlayers = new NBTTagList();

        for (Map.Entry<UUID, Map<ItemType, NBTTagCompound>> playerEntry : this.properties.entrySet())
        {
            tagListPlayers.appendTag(this.writeDataForPlayerToNBT(playerEntry.getKey(), playerEntry.getValue()));
        }

        nbt.setTag("PlacementProperties", tagListPlayers);

        return nbt;
    }

    private NBTTagCompound writeDataForPlayerToNBT(UUID uuid, Map<ItemType, NBTTagCompound> playerData)
    {
        NBTTagCompound tagPlayer = new NBTTagCompound();
        NBTTagList tagListData = new NBTTagList();
        tagPlayer.setString("UUID", uuid.toString());
        tagPlayer.setTag("Data", tagListData);

        for (Map.Entry<ItemType, NBTTagCompound> dataEntry : playerData.entrySet())
        {
            NBTTagCompound tagData = new NBTTagCompound();
            tagData.setTag("ItemType", dataEntry.getKey().getStack().writeToNBT(new NBTTagCompound()));
            tagData.setTag("Tag", dataEntry.getValue());
            int index = this.getPropertyIndex(uuid, dataEntry.getKey());

            if (index != 0)
            {
                tagData.setByte("Index", (byte) index);
            }

            tagListData.appendTag(tagData);
        }

        return tagPlayer;
    }

    public void syncCurrentlyHeldItemDataForPlayer(EntityPlayerMP player, ItemStack stack)
    {
        if (stack.isEmpty() == false && stack.getItem() instanceof ItemBlockAutoverse)
        {
            ItemBlockAutoverse item = (ItemBlockAutoverse) stack.getItem();

            if (item.hasPlacementProperty(stack))
            {
                UUID uuid = player.getUniqueID();
                boolean nbtSensitive = item.getPlacementProperty(stack).isNBTSensitive();
                ItemType type = new ItemType(stack, nbtSensitive);
                NBTTagCompound props = this.getPropertyTag(uuid, type);

                if (props != null)
                {
                    NBTTagCompound tag = new NBTTagCompound();
                    tag.setString("UUID", uuid.toString());
                    tag.setTag("ItemType", stack.writeToNBT(new NBTTagCompound()));
                    tag.setByte("Index", (byte) this.getPropertyIndex(uuid, type));
                    tag.setTag("Tag", props);

                    MessageSyncNBTTag message = new MessageSyncNBTTag(uuid, MessageSyncNBTTag.Type.PLACEMENT_PROPERTIES_CURRENT, tag);
                    PacketHandler.INSTANCE.sendTo(message, player);
                }
            }
        }
    }

    public void syncAllDataForPlayer(EntityPlayerMP player)
    {
        UUID uuid = player.getUniqueID();
        Map<ItemType, NBTTagCompound> playerData = this.properties.get(uuid);

        if (playerData != null)
        {
            NBTTagCompound tag = this.writeDataForPlayerToNBT(uuid, playerData);

            MessageSyncNBTTag message = new MessageSyncNBTTag(uuid, MessageSyncNBTTag.Type.PLACEMENT_PROPERTIES_FULL, tag);
            PacketHandler.INSTANCE.sendTo(message, player);
        }
    }

    public static class PlacementProperty
    {
        private boolean isNBTSensitive;
        private List<Pair<String, Integer>> propertyTypes = new ArrayList<>();
        private List<Pair<Integer, Integer>> propertyValueRange = new ArrayList<>();
        private Map<String, String[]> propertyValueNames = new HashMap<>();
        private Map<String, Integer> defaultValues = new HashMap<>();

        public boolean hasPlacementProperties()
        {
            return this.propertyTypes.isEmpty() == false;
        }

        public boolean isNBTSensitive()
        {
            return this.isNBTSensitive;
        }

        public void setIsNBTSensitive(boolean checkNBT)
        {
            this.isNBTSensitive = checkNBT;
        }

        public void addProperty(String key, int type, int minValue, int maxValue, int defaultValue)
        {
            this.addProperty(key, type, minValue, maxValue);
            this.defaultValues.put(key, defaultValue);
        }

        public void addProperty(String key, int type, int minValue, int maxValue)
        {
            this.propertyTypes.add(Pair.of(key, type));
            this.propertyValueRange.add(Pair.of(minValue, maxValue));
        }

        public void addValueNames(String key, String[] names)
        {
            this.propertyValueNames.put(key, names);
        }

        @Nullable
        public Integer getDefaultValue(int selection)
        {
            return selection >= 0 && selection < this.propertyTypes.size() ? this.defaultValues.get(this.propertyTypes.get(selection).getLeft()) : null;
        }

        @Nullable
        public Pair<String, Integer> getProperty(int selection)
        {
            return selection >= 0 && selection < this.propertyTypes.size() ? this.propertyTypes.get(selection) : null;
        }

        @Nullable
        public Pair<Integer, Integer> getPropertyValueRange(int selection)
        {
            return selection >= 0 && selection < this.propertyValueRange.size() ? this.propertyValueRange.get(selection) : null;
        }

        @Nullable
        public String getPropertyValueName(String key, int value)
        {
            String[] names = this.propertyValueNames.get(key);

            if (names != null && value >= 0 && value < names.length)
            {
                return names[value];
            }

            return null;
        }

        public int getPropertyCount()
        {
            return this.propertyTypes.size();
        }
    }
}
