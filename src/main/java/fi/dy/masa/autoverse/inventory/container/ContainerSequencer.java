package fi.dy.masa.autoverse.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandler;
import fi.dy.masa.autoverse.inventory.slot.MergeSlotRange;
import fi.dy.masa.autoverse.inventory.slot.SlotItemHandlerGeneric;
import fi.dy.masa.autoverse.tileentity.TileEntitySequencer;

public class ContainerSequencer extends ContainerCustomSlotClick
{
    private final TileEntitySequencer teseq;

    public ContainerSequencer(EntityPlayer player, TileEntitySequencer te)
    {
        super(player, te);

        this.teseq = te;
        this.addCustomInventorySlots();
        this.addPlayerInventorySlots(8, 73);
    }

    @Override
    protected void addCustomInventorySlots()
    {
        int posX = 8;
        int posY = 22;
        IItemHandler inv = this.teseq.getBaseItemHandler();

        // Add the Filter slots
        for (int slot = 0, col = 0, row = 0; slot < inv.getSlots(); slot++)
        {
            this.addSlotToContainer(new SlotItemHandlerGeneric(inv, slot, posX + col * 18, posY + row * 18));

            if (++col == 8 && inv.getSlots() > 9)
            {
                col = 0;
                row++;
            }
        }

        this.customInventorySlots = new MergeSlotRange(0, this.inventorySlots.size());
    }
}