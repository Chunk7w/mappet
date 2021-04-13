package mchorse.mappet.api.quests;

import mchorse.mappet.api.quests.objectives.IObjective;
import mchorse.mappet.api.quests.objectives.KillObjective;
import mchorse.mappet.api.quests.rewards.IReward;
import mchorse.mappet.api.utils.Trigger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.List;

public class Quest implements INBTSerializable<NBTTagCompound>, INBTPartialSerializable
{
    public String customTitle = "";
    public String customStory = "";

    public Trigger accept = new Trigger();
    public Trigger decline = new Trigger();
    public Trigger complete = new Trigger();

    public final List<IObjective> objectives = new ArrayList<IObjective>();
    public final List<IReward> rewards = new ArrayList<IReward>();

    public Quest()
    {}

    /* Quest building */

    public Quest setStory(String title, String story)
    {
        this.customTitle = title;
        this.customStory = story;

        return this;
    }

    public Quest addObjective(IObjective objective)
    {
        this.objectives.add(objective);

        return this;
    }

    public Quest addReward(IReward reward)
    {
        this.rewards.add(reward);

        return this;
    }

    /* Quest hooks */

    public void mobWasKilled(EntityPlayer player, Entity entity)
    {
        for (IObjective objective : this.objectives)
        {
            if (objective instanceof KillObjective)
            {
                ((KillObjective) objective).playerKilled(player, entity);
            }
        }
    }

    /* Rewards */

    public boolean isComplete(EntityPlayer player)
    {
        boolean result = true;

        for (IObjective objective : this.objectives)
        {
            result = result && objective.isComplete(player);
        }

        return result;
    }

    public void reward(EntityPlayer player)
    {
        for (IObjective objective : this.objectives)
        {
            objective.complete(player);
        }

        for (IReward reward : this.rewards)
        {
            reward.reward(player);
        }

        this.complete.trigger(player);
    }

    public boolean rewardIfComplete(EntityPlayer player)
    {
        if (!this.isComplete(player))
        {
            return false;
        }

        this.reward(player);

        return true;
    }

    /* NBT stuff */

    @Override
    public NBTTagCompound partialSerializeNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList objectives = new NBTTagList();

        tag.setTag("Objectives", objectives);

        for (IObjective objective : this.objectives)
        {
            objectives.appendTag(objective.partialSerializeNBT());
        }

        return tag;
    }

    @Override
    public void partialDeserializeNBT(NBTTagCompound tag)
    {
        if (tag.hasKey("Objectives", Constants.NBT.TAG_LIST))
        {
            NBTTagList list = tag.getTagList("Objectives", Constants.NBT.TAG_COMPOUND);

            for (int i = 0; i < Math.min(list.tagCount(), this.objectives.size()); i++)
            {
                this.objectives.get(i).partialDeserializeNBT(list.getCompoundTagAt(i));
            }
        }
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList objectives = new NBTTagList();
        NBTTagList rewards = new NBTTagList();

        tag.setString("Title", this.customTitle);
        tag.setString("Story", this.customStory);

        NBTTagCompound accept = this.accept.serializeNBT();
        NBTTagCompound decline = this.decline.serializeNBT();
        NBTTagCompound complete = this.complete.serializeNBT();

        if (accept.getSize() > 0) tag.setTag("Accept", accept);
        if (decline.getSize() > 0) tag.setTag("Decline", decline);
        if (complete.getSize() > 0) tag.setTag("Complete", complete);

        tag.setTag("Objectives", objectives);
        tag.setTag("Rewards", rewards);

        for (IObjective objective : this.objectives)
        {
            NBTTagCompound item = objective.serializeNBT();

            item.setString("Type", objective.getType());
            objectives.appendTag(item);
        }

        for (IReward reward : this.rewards)
        {
            NBTTagCompound item = reward.serializeNBT();

            item.setString("Type", reward.getType());
            rewards.appendTag(item);
        }

        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag)
    {
        this.customTitle = tag.getString("Title");
        this.customStory = tag.getString("Story");

        if (tag.hasKey("Accept"))
        {
            this.accept.deserializeNBT(tag.getCompoundTag("Accept"));
        }

        if (tag.hasKey("Decline"))
        {
            this.decline.deserializeNBT(tag.getCompoundTag("Decline"));
        }

        if (tag.hasKey("Complete"))
        {
            this.complete.deserializeNBT(tag.getCompoundTag("Complete"));
        }

        if (tag.hasKey("Objectives"))
        {
            NBTTagList list = tag.getTagList("Objectives", Constants.NBT.TAG_COMPOUND);

            for (int i = 0; i < list.tagCount(); i ++)
            {
                NBTTagCompound tagCompound = list.getCompoundTagAt(i);
                IObjective objective = IObjective.fromType(tagCompound.getString("Type"));

                if (objective != null)
                {
                    objective.deserializeNBT(tagCompound);
                    this.objectives.add(objective);
                }
            }
        }

        if (tag.hasKey("Rewards"))
        {
            NBTTagList list = tag.getTagList("Rewards", Constants.NBT.TAG_COMPOUND);

            for (int i = 0; i < list.tagCount(); i ++)
            {
                NBTTagCompound tagCompound = list.getCompoundTagAt(i);
                IReward reward = IReward.fromType(tagCompound.getString("Type"));

                if (reward != null)
                {
                    reward.deserializeNBT(tagCompound);
                    this.rewards.add(reward);
                }
            }
        }
    }
}