package mchorse.mappet.api.quests.objectives;

import mchorse.mappet.utils.InventoryUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class CollectObjective implements IObjective
{
    public ItemStack stack = ItemStack.EMPTY;

    public CollectObjective()
    {}

    public CollectObjective(ItemStack stack)
    {
        this.stack = stack == null ? ItemStack.EMPTY : stack;
    }

    @Override
    public boolean isComplete(EntityPlayer player)
    {
        return this.countItems(player) >= this.stack.getCount();
    }

    @Override
    public void complete(EntityPlayer player)
    {
        player.inventory.clearMatchingItems(this.stack.getItem(), -1, this.stack.getCount(), null);
    }

    private int countItems(EntityPlayer player)
    {
        return InventoryUtils.countItems(player, this.stack);
    }

    @Override
    public String stringify(EntityPlayer player)
    {
        return "Collect " + this.stack.getDisplayName() + " (" + Math.min(this.countItems(player), this.stack.getCount()) + "/" + this.stack.getCount() + ")";
    }

    @Override
    public String getType()
    {
        return "collect";
    }

    @Override
    public NBTTagCompound partialSerializeNBT()
    {
        return new NBTTagCompound();
    }

    @Override
    public void partialDeserializeNBT(NBTTagCompound tag)
    {}

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();

        if (!this.stack.isEmpty())
        {
            tag.setTag("Item", this.stack.serializeNBT());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag)
    {
        if (tag.hasKey("Item"))
        {
            this.stack = new ItemStack(tag.getCompoundTag("Item"));
        }
    }
}