package mchorse.mappet.api.events;

import mchorse.mappet.CommonProxy;
import mchorse.mappet.api.events.nodes.EventNode;
import mchorse.mappet.api.utils.DataContext;
import mchorse.mappet.api.utils.nodes.NodeSystem;
import mchorse.mappet.entities.EntityNpc;
import net.minecraft.entity.EntityLivingBase;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class EventContext
{
    public NodeSystem<EventNode> system;

    public DataContext data;

    public boolean debug;
    public StringBuilder log = new StringBuilder();
    public int nesting = 0;
    public int executions = 0;

    public List<EventExecutionFork> executionForks = new ArrayList<EventExecutionFork>();

    public EventContext(DataContext data)
    {
        this.data = data;
    }

    public EventContext debug()
    {
        this.debug = true;

        return this;
    }

    public void addExecutionFork(EventNode node, int timer)
    {
        if (this.system != null && timer > 0)
        {
            this.executionForks.add(new EventExecutionFork(this.system, node, this, timer));
        }
    }

    public void submitDelayedExecutions()
    {
        if (!this.executionForks.isEmpty())
        {
            CommonProxy.eventHandler.addExecutionForks(this.executionForks);

            this.executionForks.clear();
        }
    }

    public void log(String message)
    {
        if (this.debug)
        {
            if (this.nesting > 0)
            {
                message = StringUtils.repeat("-", this.nesting) + " " + message;
            }

            this.log.append(message + "\n");
        }
    }
}