package mchorse.mappet.api.utils;

import mchorse.mappet.utils.ExpressionRewriter;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DataContext
{
    public static final ExpressionRewriter REWRITER = new ExpressionRewriter();

    public MinecraftServer server;
    public World world;
    public EntityLivingBase subject;
    public EntityLivingBase object;

    private TriggerSender sender;
    private Map<String, Object> values = new HashMap<String, Object>();

    public DataContext(EntityLivingBase subject, EntityLivingBase object)
    {
        this(subject);

        this.object = object;
        this.setup();
    }

    public DataContext(EntityLivingBase subject)
    {
        this(subject.world);

        this.subject = subject;
        this.setup();
    }

    public DataContext(World world)
    {
        this(world.getMinecraftServer());

        this.world = world;
    }

    public DataContext(MinecraftServer server)
    {
        this.server = server;

        this.setup();
    }

    private void setup()
    {
        this.set("subject", this.subject == null ? "" : this.subject.getCachedUniqueIdString());
        this.set("object", this.object == null ? "" : this.object.getCachedUniqueIdString());
    }

    public DataContext set(String key, double value)
    {
        this.values.put(key, value);

        return this;
    }

    public DataContext set(String key, String value)
    {
        this.values.put(key, value);

        return this;
    }

    public DataContext parse(String nbt)
    {
        try
        {
            NBTTagCompound tag = JsonToNBT.getTagFromJson(nbt);

            for (String key : tag.getKeySet())
            {
                NBTBase value = tag.getTag(key);

                if (value instanceof NBTPrimitive)
                {
                    this.set(key, ((NBTPrimitive) value).getDouble());
                }
                else if (value instanceof NBTTagString)
                {
                    this.set(key, ((NBTTagString) value).getString());
                }
            }
        }
        catch (Exception e)
        {}

        return this;
    }

    public Set<Map.Entry<String, Object>> getValues()
    {
        return this.values.entrySet();
    }

    public Object getValue(String key)
    {
        return this.values.get(key);
    }

    public int execute(String command)
    {
        command = this.process(command);

        return this.server.getCommandManager().executeCommand(this.getSender(), command);
    }

    public String process(String text)
    {
        if (!text.contains("${"))
        {
            return text;
        }

        return REWRITER.set(this).rewrite(text);
    }

    public Set<String> getKeys()
    {
        return this.values.keySet();
    }

    public TriggerSender getSender()
    {
        if (this.sender == null)
        {
            this.sender = new TriggerSender();
        }

        if (this.subject == null)
        {
            this.sender.set(this.server);
        }
        else
        {
            this.sender.set(this.subject);
        }

        return this.sender;
    }
}