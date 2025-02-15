package mchorse.mappet.api.conditions.blocks;

import mchorse.mappet.Mappet;
import mchorse.mappet.api.conditions.utils.Target;
import mchorse.mappet.api.factions.Faction;
import mchorse.mappet.api.factions.FactionAttitude;
import mchorse.mappet.api.states.States;
import mchorse.mappet.api.utils.DataContext;
import mchorse.mappet.utils.EnumUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class FactionBlock extends PropertyBlock
{
    public FactionCheck faction = FactionCheck.SCORE;

    public FactionBlock()
    {
        super();
    }

    @Override
    protected Target getDefaultTarget()
    {
        return Target.SUBJECT;
    }

    @Override
    public boolean evaluate(DataContext context)
    {
        if (this.target != Target.GLOBAL)
        {
            States states = this.getStates(context);

            if (states == null)
            {
                return false;
            }

            if (this.faction == FactionCheck.SCORE)
            {
                if (!states.hasFaction(this.id))
                {
                    return false;
                }

                double a = states.getFactionScore(this.id);
                double b = this.value;

                return this.comparison.compare(a, b);
            }

            Faction faction = Mappet.factions.load(this.id);

            if (faction == null)
            {
                return false;
            }

            return faction.get(states) == this.faction.attitude;
        }

        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String stringify()
    {
        if (this.faction == FactionCheck.SCORE)
        {
            return this.id + " " + this.comparison.operation.sign + " " + this.value;
        }
        else if (this.faction == FactionCheck.AGGRESSIVE)
        {
            return I18n.format("mappet.gui.conditions.faction.is_aggressive", this.id);
        }
        else if (this.faction == FactionCheck.PASSIVE)
        {
            return I18n.format("mappet.gui.conditions.faction.is_passive", this.id);
        }

        return I18n.format("mappet.gui.conditions.faction.is_friendly", this.id);
    }

    @Override
    public void serializeNBT(NBTTagCompound tag)
    {
        super.serializeNBT(tag);

        tag.setInteger("Faction", this.faction.ordinal());
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag)
    {
        super.deserializeNBT(tag);

        this.faction = EnumUtils.getValue(tag.getInteger("Faction"), FactionCheck.values(), FactionCheck.SCORE);
    }

    public static enum FactionCheck
    {
        AGGRESSIVE(FactionAttitude.AGGRESSIVE), PASSIVE(FactionAttitude.PASSIVE), FRIENDLY(FactionAttitude.FRIENDLY), SCORE(null);

        public final FactionAttitude attitude;

        FactionCheck(FactionAttitude attitude)
        {
            this.attitude = attitude;
        }
    }
}