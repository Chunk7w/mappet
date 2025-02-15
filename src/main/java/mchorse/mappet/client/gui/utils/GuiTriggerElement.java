package mchorse.mappet.client.gui.utils;

import mchorse.mappet.ClientProxy;
import mchorse.mappet.api.utils.ContentType;
import mchorse.mappet.api.utils.Trigger;
import mchorse.mappet.client.gui.utils.overlays.GuiContentNamesOverlayPanel;
import mchorse.mappet.client.gui.utils.overlays.GuiOverlay;
import mchorse.mappet.client.gui.utils.overlays.GuiResourceLocationOverlayPanel;
import mchorse.mappet.client.gui.utils.overlays.GuiSoundOverlayPanel;
import mchorse.mclib.client.gui.framework.GuiBase;
import mchorse.mclib.client.gui.framework.elements.GuiElement;
import mchorse.mclib.client.gui.framework.elements.buttons.GuiIconElement;
import mchorse.mclib.client.gui.framework.elements.input.GuiTextElement;
import mchorse.mclib.client.gui.framework.elements.utils.GuiContext;
import mchorse.mclib.client.gui.utils.Icons;
import mchorse.mclib.client.gui.utils.keys.IKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class GuiTriggerElement extends GuiElement
{
    public GuiTextElement command;
    public GuiIconElement soundEvent;
    public GuiIconElement triggerEvent;
    public GuiIconElement dialogue;

    private Trigger trigger;

    public GuiTriggerElement(Minecraft mc)
    {
        this(mc, null);
    }

    public GuiTriggerElement(Minecraft mc, Trigger trigger)
    {
        super(mc);

        this.command = GuiMappetUtils.fullWindowContext(
            new GuiTextElement(mc, 10000, (text) -> this.trigger.command = text),
            IKey.lang("mappet.gui.trigger.command")
        );
        this.soundEvent = new GuiIconElement(mc, Icons.SOUND, (b) -> this.openPickSoundOverlay());
        this.soundEvent.tooltip(IKey.lang("mappet.gui.trigger.sound"));
        this.triggerEvent = new GuiIconElement(mc, Icons.FILE, (text) -> this.openEventsOverlay());
        this.triggerEvent.tooltip(IKey.lang("mappet.gui.trigger.event"));
        this.dialogue = new GuiIconElement(mc, Icons.BUBBLE, (b) -> this.openDialoguesOverlay());
        this.dialogue.tooltip(IKey.lang("mappet.gui.trigger.dialogue"));

        this.flex().h(32);

        GuiElement element = new GuiElement(mc);

        element.flex().relative(this).y(12).w(1F).h(20).row(0).preferred(0);
        element.add(this.command, this.soundEvent, this.triggerEvent, this.dialogue);
        this.add(element);

        this.set(trigger);
    }

    private void openPickSoundOverlay()
    {
        GuiResourceLocationOverlayPanel overlay = new GuiSoundOverlayPanel(this.mc, this::setSound).set(this.trigger.soundEvent);

        GuiOverlay.addOverlay(GuiBase.getCurrent(), overlay, 0.5F, 0.9F);
    }

    private void openEventsOverlay()
    {
        ClientProxy.requestNames(ContentType.EVENT, (names) ->
        {
            GuiContentNamesOverlayPanel overlay = new GuiContentNamesOverlayPanel(this.mc, IKey.lang("mappet.gui.overlays.event"), ContentType.EVENT, names, this::setEvent);

            overlay.set(this.trigger.triggerEvent);
            GuiOverlay.addOverlay(GuiBase.getCurrent(), overlay, 0.5F, 0.7F);
        });
    }

    private void openDialoguesOverlay()
    {
        ClientProxy.requestNames(ContentType.DIALOGUE, (names) ->
        {
            GuiContentNamesOverlayPanel overlay = new GuiContentNamesOverlayPanel(this.mc, IKey.lang("mappet.gui.overlays.dialogue"), ContentType.DIALOGUE, names, this::setDialogue);

            overlay.set(this.trigger.dialogue);
            GuiOverlay.addOverlay(GuiBase.getCurrent(), overlay, 0.5F, 0.7F);
        });
    }

    private void setSound(ResourceLocation location)
    {
        this.trigger.soundEvent = location == null ? "" : location.toString();
        this.updateColors();
    }

    private void setEvent(String name)
    {
        this.trigger.triggerEvent = name;
        this.updateColors();
    }

    private void setDialogue(String name)
    {
        this.trigger.dialogue = name;
        this.updateColors();
    }

    public Trigger get()
    {
        return this.trigger;
    }

    public void set(Trigger trigger)
    {
        this.trigger = trigger;

        if (trigger != null)
        {
            this.command.setText(trigger.command);
            this.updateColors();
        }
    }

    private void updateColors()
    {
        int active = 0xffffffff;
        int inactive = 0xaa888888;

        this.soundEvent.iconColor(trigger.soundEvent.isEmpty() ? inactive : active);
        this.triggerEvent.iconColor(trigger.triggerEvent.isEmpty() ? inactive : active);
        this.dialogue.iconColor(trigger.dialogue.isEmpty() ? inactive : active);
    }

    @Override
    public void draw(GuiContext context)
    {
        super.draw(context);

        this.font.drawStringWithShadow(I18n.format("mappet.gui.trigger.command"), this.command.area.x, this.command.area.y - 12, 0xffffff);
    }
}