package fi.dy.masa.autoverse.tileentity.base;

import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.autoverse.block.base.BlockAutoverse;
import fi.dy.masa.autoverse.gui.client.GuiAutoverse;
import fi.dy.masa.autoverse.inventory.container.base.ContainerAutoverse;
import fi.dy.masa.autoverse.reference.Reference;

public abstract class TileEntityAutoverse extends TileEntity
{
    protected String tileEntityName;
    protected EnumFacing facing = EnumFacing.UP;
    protected EnumFacing facingOpposite = EnumFacing.DOWN;
    protected BlockPos posFront;
    protected boolean redstoneState;

    public TileEntityAutoverse(String name)
    {
        this.facing = BlockAutoverse.DEFAULT_FACING;
        this.tileEntityName = name;
    }

    public String getTEName()
    {
        return this.tileEntityName;
    }

    public void setFacing(EnumFacing facing)
    {
        this.facing = facing;
        this.facingOpposite = facing.getOpposite();
        this.posFront = this.getPos().offset(facing);
        this.markDirty();

        if (this.getWorld() != null && this.getWorld().isRemote == false)
        {
            this.notifyBlockUpdate(this.getPos());
        }
    }

    public EnumFacing getFacing()
    {
        return this.facing;
    }

    @Override
    public void mirror(Mirror mirrorIn)
    {
        this.rotate(mirrorIn.toRotation(this.facing));
    }

    @Override
    public void rotate(Rotation rotationIn)
    {
        this.setFacing(rotationIn.rotate(this.getFacing()));
    }

    protected Vec3d getSpawnedItemPosition()
    {
        return this.getSpawnedItemPosition(this.facing);
    }

    protected Vec3d getSpawnedItemPosition(EnumFacing side)
    {
        double x = this.getPos().getX() + 0.5 + side.getFrontOffsetX() * 0.625;
        double y = this.getPos().getY() + 0.5 + side.getFrontOffsetY() * 0.5;
        double z = this.getPos().getZ() + 0.5 + side.getFrontOffsetZ() * 0.625;

        if (side == EnumFacing.DOWN)
        {
            y -= 0.25;
        }

        return new Vec3d(x, y, z);
    }

    public void setPlacementProperties(World world, BlockPos pos, @Nonnull ItemStack stack, @Nonnull NBTTagCompound tag) { }

    public boolean isUsableByPlayer(EntityPlayer player)
    {
        return this.getWorld().getTileEntity(this.getPos()) == this && player.getDistanceSq(this.getPos()) <= 64.0d;
    }

    /**
     * @return true if something happened, and further processing (such as opening the GUI) should not happen
     */
    public boolean onRightClickBlock(EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        return false;
    }

    public void onLeftClickBlock(EntityPlayer player) { }

    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock)
    {
        boolean redstone = this.getWorld().isBlockPowered(this.getPos());

        if (redstone != this.redstoneState)
        {
            this.onRedstoneChange(redstone);
        }

        this.redstoneState = redstone;
    }

    public void onScheduledBlockUpdate(World world, BlockPos pos, IBlockState state, Random rand)
    {
    }

    public void scheduleBlockUpdate(int delay, boolean force)
    {
        World world = this.getWorld();

        if (world != null && (force || world.isUpdateScheduled(this.getPos(), this.getBlockType()) == false))
        {
            //System.out.printf("scheduleBlockUpdate(), actually scheduling for %s\n", this.getPos());
            world.scheduleUpdate(this.getPos(), this.getBlockType(), delay);
        }
    }

    protected void notifyBlockUpdate(BlockPos pos)
    {
        IBlockState state = this.getWorld().getBlockState(pos);
        this.getWorld().notifyBlockUpdate(pos, state, state, 3);
    }

    protected void onRedstoneChange(boolean state)
    {
        if (state)
        {
            World world = this.getWorld();
            this.scheduleBlockUpdate(world.getBlockState(this.getPos()).getBlock().tickRate(world), false);
        }
    }

    public void readFromNBTCustom(NBTTagCompound nbt)
    {
        this.redstoneState = nbt.getBoolean("Redstone");

        // Update the opposite and the front and back BlockPos
        this.setFacing(EnumFacing.getFront(nbt.getByte("Facing")));
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        this.readFromNBTCustom(nbt); // This call needs to be at the super-most custom TE class
    }

    protected NBTTagCompound writeToNBTCustom(NBTTagCompound nbt)
    {
        nbt.setString("Version", Reference.MOD_VERSION);
        nbt.setByte("Facing", (byte)this.facing.getIndex());
        nbt.setBoolean("Redstone", this.redstoneState);

        return nbt;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        nbt = super.writeToNBT(nbt);

        this.writeToNBTCustom(nbt);

        return nbt;
    }

    /**
     * Get the data used for syncing the TileEntity to the client.
     * The data returned from this method doesn't have the position,
     * the position will be added in getUpdateTag() which calls this method.
     */
    public NBTTagCompound getUpdatePacketTag(NBTTagCompound tag)
    {
        tag.setByte("f", (byte)(this.getFacing().getIndex() & 0x07));
        return tag;
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        // The tag from this method is used for the initial chunk packet,
        // and it needs to have the TE position!
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("x", this.getPos().getX());
        nbt.setInteger("y", this.getPos().getY());
        nbt.setInteger("z", this.getPos().getZ());

        // Add the per-block data to the tag
        return this.getUpdatePacketTag(nbt);
    }

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        if (this.getWorld() != null)
        {
            return new SPacketUpdateTileEntity(this.getPos(), 0, this.getUpdatePacketTag(new NBTTagCompound()));
        }

        return null;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag)
    {
        if (tag.hasKey("f"))
        {
            this.setFacing(EnumFacing.getFront((byte)(tag.getByte("f") & 0x07)));
        }

        this.notifyBlockUpdate(this.getPos());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet)
    {
        this.handleUpdateTag(packet.getNbtCompound());
    }

    public void performGuiAction(EntityPlayer player, int action, int element)
    {
    }

    /*
    protected void sendPacketToWatchers(IMessage message)
    {
        World world = this.getWorld();

        if (world instanceof WorldServer)
        {
            WorldServer worldServer = (WorldServer) world;
            int chunkX = this.getPos().getX() >> 4;
            int chunkZ = this.getPos().getZ() >> 4;
            PlayerChunkMap map = worldServer.getPlayerChunkMap();

            for (EntityPlayerMP player : worldServer.getPlayers(EntityPlayerMP.class, Predicates.alwaysTrue()))
            {
                if (map.isPlayerWatchingChunk(player, chunkX, chunkZ))
                {
                    PacketHandler.INSTANCE.sendTo(message, player);
                }
            }
        }
    }
    */

    @Override
    public String toString()
    {
        return this.getClass().getSimpleName() + "(" + this.getPos() + ")@" + System.identityHashCode(this);
    }

    public boolean hasGui()
    {
        return true;
    }

    public ContainerAutoverse getContainer(EntityPlayer player)
    {
        return null;
    }

    @SideOnly(Side.CLIENT)
    public GuiAutoverse getGui(EntityPlayer player)
    {
        return null;
    }
}
