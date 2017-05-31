package fi.dy.masa.autoverse.inventory.wrapper.machines;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;
import fi.dy.masa.autoverse.inventory.wrapper.InventoryCraftingWrapper;
import fi.dy.masa.autoverse.util.InventoryUtils;
import fi.dy.masa.autoverse.util.InventoryUtils.InvResult;

public class ItemHandlerWrapperCrafter extends ItemHandlerWrapperSequenceBase
{
    private final SequenceMatcher sequenceEmpty;
    private final SequenceMatcher sequenceRecipe;
    private final IItemHandler emptyMarkerInventory;
    private final IItemHandler recipeSequenceInventory;
    private Mode mode = Mode.CONFIGURE_RESET;
    private int outputPosition;
    private ItemStack resultStackTemplate = ItemStack.EMPTY;

    private final InventoryCraftingWrapper inventoryCrafting;
    private final InventoryCraftingWrapper inventoryCraftingSequenceCraftingWrapper;
    private final IItemHandler inventoryCraftingGrid;
    private final IItemHandler inventoryCraftingOutput;
    private final IItemHandler inventoryOutputBuffer;

    public ItemHandlerWrapperCrafter(int resetSequenceLength,
            IItemHandler inventoryInput,
            InventoryCraftingWrapper inventoryCrafting,
            IItemHandlerModifiable inventoryCraftingOutput,
            IItemHandler inventoryOutputBuffer)
    {
        super(resetSequenceLength, inventoryInput);

        this.inventoryCrafting = inventoryCrafting;
        this.inventoryCraftingGrid = new InvWrapper(this.inventoryCrafting);
        this.inventoryCraftingOutput = inventoryCraftingOutput;
        this.inventoryOutputBuffer = inventoryOutputBuffer;

        this.sequenceEmpty  = new SequenceMatcher(1, "SequenceEmpty");
        this.sequenceRecipe = new SequenceMatcher(9, "SequenceRecipe");

        this.emptyMarkerInventory = this.sequenceEmpty.getSequenceInventory(false);
        this.recipeSequenceInventory = this.sequenceRecipe.getSequenceInventory(false);
        this.inventoryCraftingSequenceCraftingWrapper = new InventoryCraftingWrapper(3, 3,
                this.sequenceRecipe.getSequenceInventory(false), inventoryCraftingOutput, null);
    }

    @Override
    protected void handleInputItem(ItemStack inputStack)
    {
        switch (this.getMode())
        {
            case CONFIGURE_RESET:
                //System.out.printf("CONFIGURE_RESET\n");
                if (this.getResetSequence().configureSequence(inputStack))
                {
                    //System.out.printf("CONFIGURE_RESET - done\n");
                    this.setMode(Mode.CONFIGURE_EMPTY_MARKER);
                }
                break;

            case CONFIGURE_EMPTY_MARKER:
                //System.out.printf("CONFIGURE_EMPTY_MARKER\n");
                if (this.sequenceEmpty.configureSequence(inputStack))
                {
                    //System.out.printf("CONFIGURE_EMPTY_MARKER - done\n");
                    this.setMode(Mode.CONFIGURE_RECIPE);
                }
                break;

            case CONFIGURE_RECIPE:
                if (InventoryUtils.areItemStacksEqual(inputStack, this.emptyMarkerInventory.getStackInSlot(0)))
                {
                    inputStack = ItemStack.EMPTY;
                }

                //System.out.printf("CONFIGURE_RECIPE\n");
                if (this.sequenceRecipe.configureSequence(inputStack))
                {
                    //System.out.printf("CONFIGURE_RECIPE - done\n");
                    this.setMode(Mode.CONFIGURE_RECIPE_DONE);
                }
                break;

            case NORMAL_OPERATION:
                //System.out.printf("NORMAL_OPERATION\n");
                if (this.getResetSequence().checkInputItem(inputStack))
                {
                    //System.out.printf("NORMAL_OPERATION - reset\n");
                    this.getResetSequence().reset();
                    this.sequenceEmpty.reset();
                    this.sequenceRecipe.reset();
                    this.onReset();
                }
                break;

            case OUTPUT_ITEMS_FROM_GRID:
            case OUTPUT_ITEMS_FROM_BUFFER:
                break;

            default:
                break;
        }
    }

    public boolean moveItems()
    {
        Mode mode = this.getMode();

        switch (mode)
        {
            case CONFIGURE_RESET:
            case CONFIGURE_EMPTY_MARKER:
            case CONFIGURE_RECIPE:
            case CONFIGURE_RECIPE_DONE:
                //System.out.printf("moveItems - conf\n");
                if (InventoryUtils.tryMoveEntireStackOnly(this.getInputInventory(), 0, this.inventoryOutputBuffer, 0) == InvResult.MOVED_ALL)
                {
                    if (mode == Mode.CONFIGURE_RECIPE_DONE)
                    {
                        this.createMatchingSlotsMap(this.sequenceRecipe.getSequence());
                        this.setMode(Mode.NORMAL_OPERATION);

                        this.resultStackTemplate = CraftingManager.getInstance()
                                .findMatchingRecipe(this.inventoryCraftingSequenceCraftingWrapper, this.inventoryCrafting.getWorld());
                        //System.out.printf("moveItems - CONFIGURE_FILTER_DONE - done\n");
                    }
                    //else System.out.printf("moveItems - done\n");

                    return true;
                }
                break;

            case NORMAL_OPERATION:
                //System.out.printf("moveItems - sorting\n");
                return this.moveInputItem();

            case OUTPUT_ITEMS_FROM_GRID:
                return this.moveItemsFromGrid();

            case OUTPUT_ITEMS_FROM_BUFFER:
                return this.moveItemsFromOutputBuffer();

            case RESET:
                return this.flushItems();

            default:
                break;
        }

        return false;
    }

    private boolean moveInputItem()
    {
        ItemStack inputStack = this.getInputInventory().getStackInSlot(0);
        List<Integer> slots = this.getMatchingSlots(inputStack);
        boolean success = false;

        if (slots != null)
        {
            for (int slot : slots)
            {
                if (inputStack.isEmpty())
                {
                    success = true;
                    break;
                }

                // Only move one item at a time to each grid slot
                if (this.inventoryCraftingGrid.getStackInSlot(slot).isEmpty() &&
                    InventoryUtils.areItemStacksEqual(this.recipeSequenceInventory.getStackInSlot(slot), inputStack))
                {
                    InventoryUtils.tryMoveStack(this.getInputInventory(), 0, this.inventoryCraftingGrid, slot, 1);
                }

                inputStack = this.getInputInventory().getStackInSlot(0);
            }

            if (success == false)
            {
                // Couldn't move the item to the grid, probably because the grid was full, move to the output instead
                success |= InventoryUtils.tryMoveStack(this.getInputInventory(), 0, this.inventoryOutputBuffer, 0) != InvResult.MOVED_NOTHING;
            }
        }

        if (this.resultStackTemplate.isEmpty() == false &&
            InventoryUtils.areItemStacksEqual(this.inventoryCraftingOutput.getStackInSlot(0), this.resultStackTemplate))
        {
            success |= InventoryUtils.tryMoveEntireStackOnly(this.inventoryCraftingOutput, 0, this.inventoryOutputBuffer, 0) != InvResult.MOVED_NOTHING;
        }
        else
        {
            success |= InventoryUtils.tryMoveStack(this.getInputInventory(), 0, this.inventoryOutputBuffer, 0) != InvResult.MOVED_NOTHING;
        }

        return success;
    }

    private boolean moveItemsFromGrid()
    {
        GridMoveResult result = this.moveNonMatchingItemsFromGrid();

        if (this.gridMatchesRecipe())
        {
            this.setMode(Mode.NORMAL_OPERATION);
            return true;
        }

        return result == GridMoveResult.MOVED_SOME;
    }

    private boolean moveItemsFromOutputBuffer()
    {
        return true;
    }

    /**
     * Move all items from internal buffers to the output, before returning
     * to the programming phase for the next operation cycle.
     * @return
     */
    private boolean flushItems()
    {
        boolean success = false;

        while (this.outputPosition < 9)
        {
            if (this.inventoryCraftingGrid.getStackInSlot(this.outputPosition).isEmpty())
            {
                this.outputPosition++;
            }
            else if (InventoryUtils.tryMoveStack(this.inventoryCraftingGrid, this.outputPosition,
                                                 this.inventoryOutputBuffer, 0) == InvResult.MOVED_ALL)
            {
                this.outputPosition++;
                success = true;
                break;
            }
            else
            {
                break;
            }
        }

        // All items moved, return back to the programming phase
        if (this.outputPosition >= 9)
        {
            this.outputPosition = 0;
            this.setMode(Mode.CONFIGURE_RESET);
            return true;
        }

        return success;
    }

    private boolean gridMatchesRecipe()
    {
        for (int slot = 0; slot < 9; ++slot)
        {
            if (InventoryUtils.areItemStacksEqual(
                    this.inventoryCraftingGrid.getStackInSlot(slot),
                    this.recipeSequenceInventory.getStackInSlot(slot)) == false)
            {
                return false;
            }
        }

        return true;
    }

    private GridMoveResult moveNonMatchingItemsFromGrid()
    {
        for (int slot = 0; slot < 9; ++slot)
        {
            ItemStack stackOnGrid = this.inventoryCraftingGrid.getStackInSlot(slot);

            if (stackOnGrid.isEmpty() == false &&
                InventoryUtils.areItemStacksEqual(stackOnGrid, this.recipeSequenceInventory.getStackInSlot(slot)) == false)
            {
                return InventoryUtils.tryMoveStack(this.inventoryCraftingGrid, slot,
                        this.inventoryOutputBuffer, 0) != InvResult.MOVED_NOTHING ? GridMoveResult.MOVED_SOME : GridMoveResult.MOVED_NOTHING;
            }
        }

        return GridMoveResult.GRID_MATCHES;
    }

    private void onReset()
    {
        this.outputPosition = 0;
        this.setMode(Mode.RESET);
    }

    public IItemHandler getEmptyMarkerInventory()
    {
        return this.emptyMarkerInventory;
    }

    public IItemHandler getRecipeSequenceInventory()
    {
        return this.recipeSequenceInventory;
    }

    @Override
    protected void readFromNBT(NBTTagCompound tag)
    {
        super.readFromNBT(tag);

        this.setMode(Mode.fromId(tag.getByte("Mode")));

        this.sequenceEmpty.readFromNBT(tag);
        this.sequenceRecipe.readFromNBT(tag);

        this.createMatchingSlotsMap(this.sequenceRecipe.getSequence());
    }

    @Override
    protected NBTTagCompound writeToNBT(NBTTagCompound tag)
    {
        tag = super.writeToNBT(tag);

        tag.setByte("Mode", (byte) this.mode.getId());

        this.sequenceEmpty.writeToNBT(tag);
        this.sequenceRecipe.writeToNBT(tag);

        return tag;
    }

    public void onLoad(World world)
    {
        this.inventoryCraftingSequenceCraftingWrapper.setWorld(world);
        this.resultStackTemplate = CraftingManager.getInstance()
                .findMatchingRecipe(this.inventoryCraftingSequenceCraftingWrapper, world);
    }

    protected Mode getMode()
    {
        return this.mode;
    }

    protected void setMode(Mode mode)
    {
        this.mode = mode;
    }

    public enum Mode
    {
        CONFIGURE_RESET             (0),
        CONFIGURE_EMPTY_MARKER      (1),
        CONFIGURE_RECIPE            (2),
        CONFIGURE_RECIPE_DONE       (3),
        NORMAL_OPERATION            (4),
        OUTPUT_ITEMS_FROM_GRID      (5),
        OUTPUT_ITEMS_FROM_BUFFER    (6),
        RESET                       (7);

        private final int id;

        private Mode (int id)
        {
            this.id = id;
        }

        public int getId()
        {
            return this.id;
        }

        public static Mode fromId(int id)
        {
            return values()[id % values().length];
        }
    }

    private enum GridMoveResult
    {
        MOVED_NOTHING,
        MOVED_SOME,
        GRID_MATCHES;
    }
}
