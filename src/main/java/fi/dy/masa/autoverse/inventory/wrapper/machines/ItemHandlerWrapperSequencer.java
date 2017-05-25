package fi.dy.masa.autoverse.inventory.wrapper.machines;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.ItemStackHandlerTileEntity;
import fi.dy.masa.autoverse.util.InventoryUtils;

public class ItemHandlerWrapperSequencer implements IItemHandler, INBTSerializable<NBTTagCompound>
{
    protected final ItemStackHandlerTileEntity baseHandler;
    protected int extractSlot;

    public ItemHandlerWrapperSequencer(ItemStackHandlerTileEntity baseHandler)
    {
        this.baseHandler = baseHandler;
    }

    public int getOutputSlot()
    {
        return this.extractSlot;
    }

    @Override
    public int getSlots()
    {
        return 2;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return this.baseHandler.getSlotLimit(slot);
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound nbt = this.baseHandler.serializeNBT();

        nbt.setByte("OutSlot", (byte) this.extractSlot);

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        this.baseHandler.deserializeNBT(nbt);

        this.extractSlot = nbt.getByte("OutSlot");
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        // The first "virtual slot" is for extraction, the second is for insertion (and thus always empty)
        if (slot == 0)
        {
            ItemStack stack = this.baseHandler.getStackInSlot(this.extractSlot);

            // Do "live" searching in case the current extraction slot is empty, otherwise
            // the sequencer would get stuck since other blocks wouldn't know to pull from it
            if (stack.isEmpty())
            {
                slot = InventoryUtils.getNextNonEmptySlot(this.baseHandler, this.extractSlot + 1);

                if (slot != -1)
                {
                    stack = this.baseHandler.getStackInSlot(slot);
                }
            }

            return stack;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return InventoryUtils.tryInsertItemStackToInventory(this.baseHandler, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        ItemStack stack = this.baseHandler.extractItem(this.extractSlot, 1, simulate);
        int nextSlot = InventoryUtils.getNextNonEmptySlot(this.baseHandler, this.extractSlot + 1);

        // Do "live" searching in case the current extraction slot is empty, otherwise
        // the sequencer would get stuck since other blocks wouldn't know to pull from it
        if (stack.isEmpty() && nextSlot != -1)
        {
            stack = this.baseHandler.extractItem(nextSlot, 1, simulate);
        }

        if (simulate == false)
        {
            // No point in advancing if the sequencer is empty
            if (nextSlot != -1)
            {
                this.extractSlot = nextSlot;
            }
        }

        return stack;
    }
}
