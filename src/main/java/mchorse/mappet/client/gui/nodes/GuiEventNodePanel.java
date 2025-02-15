package mchorse.mappet.client.gui.nodes;

import mchorse.mappet.api.events.nodes.EventNode;
import mchorse.mclib.client.gui.framework.elements.buttons.GuiToggleElement;
import mchorse.mclib.client.gui.utils.keys.IKey;
import mchorse.mclib.utils.Direction;
import net.minecraft.client.Minecraft;

public abstract class GuiEventNodePanel <T extends EventNode> extends GuiNodePanel<T>
{
    public GuiToggleElement binary;

    public GuiEventNodePanel(Minecraft mc)
    {
        super(mc);

        this.binary = new GuiToggleElement(mc, IKey.lang("mappet.gui.nodes.event.binary"), (b) -> this.node.binary = b.isToggled());
        this.binary.tooltip(IKey.lang("mappet.gui.nodes.event.binary_tooltip"), Direction.TOP);
    }

    public void set(T node)
    {
        super.set(node);

        this.binary.toggled(node.binary);
    }
}