package mchorse.mappet.api.quests;

import mchorse.mappet.Mappet;
import mchorse.mappet.network.Dispatcher;
import mchorse.mappet.network.common.quests.PacketQuest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.LinkedHashMap;
import java.util.Map;

public class Quests implements INBTSerializable<NBTTagCompound>
{
    public Map<String, Quest> quests = new LinkedHashMap<String, Quest>();

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();

        for (Map.Entry<String, Quest> entry : this.quests.entrySet())
        {
            tag.setTag(entry.getKey(), entry.getValue().partialSerializeNBT());
        }

        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag)
    {
        for (String key : tag.getKeySet())
        {
            Quest quest = Mappet.quests.load(key);

            if (quest != null)
            {
                quest.partialDeserializeNBT(tag.getCompoundTag(key));
                this.quests.put(key, quest);
            }
        }
    }

    public boolean add(Quest quest, EntityPlayer player)
    {
        if (this.has(quest.getId()))
        {
            return false;
        }

        this.quests.put(quest.getId(), quest);
        quest.accept.trigger(player);

        if (player instanceof EntityPlayerMP)
        {
            Dispatcher.sendTo(new PacketQuest(quest.getId(), quest), (EntityPlayerMP) player);
        }

        return true;
    }

    public boolean complete(String id, EntityPlayer player)
    {
        return this.remove(id, player, true);
    }

    public boolean decline(String id, EntityPlayer player)
    {
        return this.remove(id, player, false);
    }

    public boolean remove(String id, EntityPlayer player, boolean reward)
    {
        Quest quest = this.quests.remove(id);

        if (quest == null)
        {
            return false;
        }

        if (reward)
        {
            quest.reward(player);
        }
        else
        {
            quest.decline.trigger(player);
        }

        if (player instanceof EntityPlayerMP)
        {
            Dispatcher.sendTo(new PacketQuest(id, null), (EntityPlayerMP) player);
        }

        return false;
    }

    public boolean has(String id)
    {
        return this.quests.containsKey(id);
    }

    public Quest getByName(String id)
    {
        return this.quests.get(id);
    }
}