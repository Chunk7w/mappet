package mchorse.mappet.api.conditions.blocks;

import mchorse.mappet.api.states.States;
import mchorse.mappet.api.utils.DataContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class StateBlock extends PropertyBlock
{
    public StateBlock()
    {}

    @Override
    public boolean evaluate(DataContext context)
    {
        States states = this.getStates(context);

        if (states == null)
        {
            return false;
        }

        double a = states.get(this.id);
        double b = this.value;

        return this.comparison.compare(a, b);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String stringify()
    {
        return this.id + " " + this.comparison.operation.sign + " " + this.value;
    }
}