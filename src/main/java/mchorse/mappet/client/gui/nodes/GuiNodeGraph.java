package mchorse.mappet.client.gui.nodes;

import mchorse.mappet.Mappet;
import mchorse.mappet.api.utils.factory.IFactory;
import mchorse.mappet.api.utils.nodes.Node;
import mchorse.mappet.api.utils.nodes.NodeRelation;
import mchorse.mappet.api.utils.nodes.NodeSystem;
import mchorse.mclib.McLib;
import mchorse.mclib.client.gui.framework.GuiBase;
import mchorse.mclib.client.gui.framework.elements.context.GuiSimpleContextMenu;
import mchorse.mclib.client.gui.framework.elements.utils.GuiCanvas;
import mchorse.mclib.client.gui.framework.elements.utils.GuiContext;
import mchorse.mclib.client.gui.framework.elements.utils.GuiDraw;
import mchorse.mclib.client.gui.utils.Area;
import mchorse.mclib.client.gui.utils.Icons;
import mchorse.mclib.client.gui.utils.Keybind;
import mchorse.mclib.client.gui.utils.keys.IKey;
import mchorse.mclib.utils.Color;
import mchorse.mclib.utils.ColorUtils;
import mchorse.mclib.utils.Interpolations;
import mchorse.mclib.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Vector2d;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GuiNodeGraph <T extends Node> extends GuiCanvas
{
    public static final IKey KEYS_CATEGORY = IKey.lang("mappet.gui.nodes.keys.editor");
    public static final IKey ADD_CATEGORY = IKey.lang("mappet.gui.nodes.keys.add");

    public static final int ACTIVE = 0x0088ff;
    public static final int IF = 0x00ff44;
    public static final int ELSE = 0xff0044;
    public static final int INACTIVE = 0xffbb00;

    public NodeSystem<T> system;

    private List<T> selected = new ArrayList<T>();
    private boolean lastSelected;
    private boolean selecting;
    private int lastNodeX;
    private int lastNodeY;

    private T output;
    private T input;

    private Color a = new Color();
    private Color b = new Color();

    private boolean notifyAboutMain;

    private Consumer<T> callback;

    public GuiNodeGraph(Minecraft mc, IFactory<T> factory, Consumer<T> callback)
    {
        super(mc);

        this.callback = callback;

        this.context(() ->
        {
            GuiSimpleContextMenu menu = new GuiSimpleContextMenu(this.mc);

            int x = (int) this.fromX(GuiBase.getCurrent().mouseX);
            int y = (int) this.fromY(GuiBase.getCurrent().mouseY);

            menu.action(Icons.ADD, IKey.lang("mappet.gui.nodes.context.add"), () ->
            {
                GuiSimpleContextMenu adds = new GuiSimpleContextMenu(this.mc);

                for (String key : this.system.getFactory().getKeys())
                {
                    IKey label = IKey.format("mappet.gui.nodes.context.add_node", IKey.lang("mappet.gui.node_types." + key));
                    int color = this.system.getFactory().getColor(key);

                    adds.action(Icons.ADD, label, () -> this.addNode(key, x, y), color);
                }

                GuiBase.getCurrent().replaceContextMenu(adds);
            });

            if (!this.selected.isEmpty())
            {
                menu.action(Icons.COPY, IKey.lang("mappet.gui.nodes.context.copy"), this::copyNodes);
            }

            try
            {
                this.addPaste(menu, x, y);
            }
            catch (Exception e)
            {}

            if (!this.selected.isEmpty())
            {
                menu.action(Icons.DOWNLOAD, IKey.lang("mappet.gui.nodes.context.main"), this::markMain);
                menu.action(Icons.REVERSE, IKey.lang("mappet.gui.nodes.context.sort"), this::sortInputs);
                menu.action(Icons.MINIMIZE, IKey.lang("mappet.gui.nodes.context.tie"), this::tieSelected);
                menu.action(Icons.MAXIMIZE, IKey.lang("mappet.gui.nodes.context.untie"), this::untieSelected);
                menu.action(Icons.REMOVE, IKey.lang("mappet.gui.nodes.context.remove"), this::removeSelected, 0xff0022);
            }

            return menu;
        });

        this.keys().register(IKey.lang("mappet.gui.nodes.context.tie"), Keyboard.KEY_F, this::tieSelected).inside().category(KEYS_CATEGORY);
        this.keys().register(IKey.lang("mappet.gui.nodes.context.untie"), Keyboard.KEY_U, this::untieSelected).inside().category(KEYS_CATEGORY);
        this.keys().register(IKey.lang("mappet.gui.nodes.context.main"), Keyboard.KEY_M, this::markMain).inside().category(KEYS_CATEGORY);
        this.keys().register(IKey.lang("mappet.gui.nodes.context.sort"), Keyboard.KEY_C, this::sortInputs).inside().category(KEYS_CATEGORY);

        int keycode = Keyboard.KEY_1;

        for (String key : factory.getKeys())
        {
            Keybind keybind = this.keys().register(IKey.format("mappet.gui.nodes.context.add_node", IKey.lang("mappet.gui.node_types." + key)), keycode, () ->
            {
                GuiContext context = GuiBase.getCurrent();

                this.addNode(key, (int) this.fromX(context.mouseX), (int) this.fromY(context.mouseY));
            });

            keybind.inside().held(Keyboard.KEY_LCONTROL).category(ADD_CATEGORY);
            keycode += 1;
        }
    }

    public GuiNodeGraph<T> notifyAboutMain()
    {
        this.notifyAboutMain = true;

        return this;
    }

    /* Copy/paste */

    private void copyNodes()
    {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        NBTTagCompound relations = new NBTTagCompound();

        for (T node : this.selected)
        {
            NBTTagCompound nodeTag = node.serializeNBT();

            nodeTag.setString("Type", this.system.getFactory().getType(node));
            list.appendTag(nodeTag);

            List<T> children = this.system.getChildren(node);

            for (T child : children)
            {
                if (this.selected.contains(child))
                {
                    NBTTagList relation;
                    String key = node.getId().toString();

                    if (relations.hasKey(key))
                    {
                        relation = relations.getTagList(key, Constants.NBT.TAG_STRING);
                    }
                    else
                    {
                        relation = new NBTTagList();
                        relations.setTag(key, relation);
                    }

                    relation.appendTag(new NBTTagString(child.getId().toString()));
                }
            }
        }

        tag.setTag("Nodes", list);
        tag.setTag("Relations", relations);
        GuiScreen.setClipboardString(tag.toString());
    }

    private void addPaste(GuiSimpleContextMenu menu, int x, int y) throws NBTException
    {
        String json = GuiScreen.getClipboardString();

        NBTTagCompound tag = JsonToNBT.getTagFromJson(json);
        NBTTagList nodesTag = tag.getTagList("Nodes", Constants.NBT.TAG_COMPOUND);
        NBTTagCompound relationsTag = tag.getCompoundTag("Relations");

        List<T> nodes = new ArrayList<T>();
        Map<String, T> mapping = new HashMap<String, T>();

        for (int i = 0; i < nodesTag.tagCount(); i++)
        {
            NBTTagCompound nodeTag = nodesTag.getCompoundTagAt(i);
            T node = this.system.getFactory().create(nodeTag.getString("Type"));

            mapping.put(nodeTag.getString("Id"), node);
            nodeTag.removeTag("Id");
            node.deserializeNBT(nodeTag);
            nodes.add(node);
        }

        int nx = nodes.get(0).x;
        int ny = nodes.get(0).y;

        menu.action(Icons.PASTE, IKey.lang("mappet.gui.nodes.context.paste"), () ->
        {
            this.selected.clear();

            for (T node : nodes)
            {
                this.system.add(node);

                node.x = node.x - nx + x;
                node.y = node.y - ny + y;

                this.select(node, true);
            }

            for (String key : relationsTag.getKeySet())
            {
                NBTTagList relations = relationsTag.getTagList(key, Constants.NBT.TAG_STRING);
                T output = mapping.get(key);

                for (NBTBase base : relations)
                {
                    T input = mapping.get(((NBTTagString) base).getString());

                    if (output != null && input != null)
                    {
                        this.system.tie(output, input);
                    }
                }
            }
        });
    }

    /* CRUD */

    private void addNode(String key, int x, int y)
    {
        T node = this.system.getFactory().create(key);

        if (node != null)
        {
            node.x = x;
            node.y = y;

            this.system.add(node);
            this.select(node);
        }
    }

    private void removeSelected()
    {
        for (T selected : this.selected)
        {
            this.system.remove(selected);
        }

        if (this.system.main != null && this.selected.contains(this.system.main))
        {
            this.system.main = null;
        }

        this.select(null);
    }

    private void tieSelected()
    {
        if (this.selected.size() <= 1)
        {
            return;
        }

        T last = this.selected.get(this.selected.size() - 1);
        List<T> nodes = new ArrayList<T>(this.selected);

        nodes.remove(last);
        nodes.sort(Comparator.comparingInt(a -> a.x));

        for (T node : nodes)
        {
            this.system.tie(last, node);
        }
    }

    private void untieSelected()
    {
        if (this.selected.isEmpty())
        {
            return;
        }

        if (this.selected.size() == 1)
        {
            this.system.relations.remove(this.selected.get(0).getId());
        }
        else if (this.selected.size() == 2)
        {
            /* Untying from both sides */
            T a = this.selected.get(0);
            T b = this.selected.get(1);

            this.system.untie(a, b);
            this.system.untie(b, a);
        }
        else
        {
            T last = this.selected.get(this.selected.size() - 1);

            for (int i = 0; i < this.selected.size() - 1; i++)
            {
                this.system.untie(last, this.selected.get(i));
            }
        }
    }

    private void markMain()
    {
        if (this.selected.isEmpty())
        {
            return;
        }

        this.system.main = this.selected.get(this.selected.size() - 1);
    }

    private void sortInputs()
    {
        if (this.selected.size() != 1)
        {
            return;
        }

        T node = this.selected.get(0);
        List<NodeRelation<T>> relations = this.system.relations.get(node.getId());

        if (relations != null)
        {
            relations.sort(Comparator.comparingInt(a -> a.input.x));
        }
    }

    public void setNode(T node)
    {
        if (this.callback != null)
        {
            this.callback.accept(node);
        }
    }

    public void select(T node)
    {
        this.select(node, false);
    }

    public void select(T node, boolean add)
    {
        if (!add)
        {
            this.selected.clear();
        }

        if (node != null)
        {
            this.selected.add(node);
        }

        this.setNode(node);
    }

    public Area getNodeArea(T node)
    {
        int x1 = this.toX(node.x - 60);
        int y1 = this.toY(node.y - 35);
        int x2 = this.toX(node.x + 60);
        int y2 = this.toY(node.y + 35);

        Area.SHARED.setPoints(x1, y1, x2, y2);

        return Area.SHARED;
    }

    public Area getNodeOutletArea(Area nodeArea, boolean output)
    {
        int y = output ? 7 : -7;

        int x1 = nodeArea.mx() - 4;
        int y1 = nodeArea.y(output ? 1F : 0F) - 4 + y;
        int x2 = nodeArea.mx() + 4;
        int y2 = nodeArea.y(output ? 1F : 0F) + 4 + y;

        Area area = new Area();

        area.setPoints(x1, y1, x2, y2);

        return area;
    }

    public boolean isConnecting()
    {
        return this.output != null || this.input != null;
    }

    public void set(NodeSystem<T> system)
    {
        this.system = system;

        this.selected.clear();

        if (system != null)
        {
            int x = system.main == null ? 0 : system.main.x;
            int y = system.main == null ? 0 : system.main.y;

            if (system.main == null && !system.nodes.isEmpty())
            {
                for (T node : system.nodes.values())
                {
                    x += node.x;
                    y += node.y;
                }

                x /= system.nodes.size();
                y /= system.nodes.size();
            }

            this.scaleX.setShift(x);
            this.scaleY.setShift(y);
            this.scaleX.setZoom(0.5F);
            this.scaleY.setZoom(0.5F);
        }
    }

    @Override
    public boolean mouseClicked(GuiContext context)
    {
        if (super.mouseClicked(context) && context.mouseButton == 2)
        {
            return true;
        }

        if (this.system == null)
        {
            return false;
        }

        if (context.mouseButton == 0)
        {
            this.lastNodeX = (int) this.fromX(context.mouseX);
            this.lastNodeY = (int) this.fromY(context.mouseY);
            boolean shift = GuiScreen.isShiftKeyDown();
            List<T> nodes = new ArrayList<T>(this.system.nodes.values());

            Collections.reverse(nodes);

            for (T node : nodes)
            {
                Area nodeArea = this.getNodeArea(node);

                if (nodeArea.isInside(context))
                {
                    if (shift)
                    {
                        if (!this.selected.contains(node))
                        {
                            this.select(node, true);
                        }
                        else
                        {
                            this.selected.remove(node);
                            this.select(node, true);
                        }
                    }
                    else if (!this.selected.contains(node))
                    {
                        this.select(node);
                    }

                    this.lastSelected = true;

                    return true;
                }
                else
                {
                    Area output = this.getNodeOutletArea(nodeArea, true);
                    Area input = this.getNodeOutletArea(nodeArea, false);

                    if (output.isInside(context))
                    {
                        this.output = node;
                    }
                    else if (input.isInside(context) && this.system.main != node)
                    {
                        this.input = node;
                    }

                    if (this.isConnecting())
                    {
                        return false;
                    }
                }
            }

            if (shift)
            {
                this.selecting = true;
            }
            else
            {
                this.select(null);
            }
        }

        return false;
    }

    @Override
    public void mouseReleased(GuiContext context)
    {
        super.mouseReleased(context);

        if (this.isConnecting())
        {
            boolean output = this.output != null;

            for (T node : this.system.nodes.values())
            {
                Area nodeArea = this.getNodeArea(node);
                Area outlet = this.getNodeOutletArea(nodeArea, !output);

                if (outlet.isInside(context))
                {
                    if (output)
                    {
                        this.input = node;
                    }
                    else
                    {
                        this.output = node;
                    }

                    break;
                }
            }
        }

        if (this.selecting)
        {
            Area area = new Area();
            boolean wasSelected = !this.selected.isEmpty();

            area.setPoints(this.lastX, this.lastY, context.mouseX, context.mouseY);

            for (T node : this.system.nodes.values())
            {
                Area nodeArea = this.getNodeArea(node);

                if (nodeArea.intersects(area) && !this.selected.contains(node))
                {
                    this.selected.add(0, node);
                }
            }

            if (!wasSelected && !this.selected.isEmpty())
            {
                this.setNode(this.selected.get(this.selected.size() - 1));
            }
        }
        else if (this.output != null && this.input != null && this.input != this.output)
        {
            this.system.tie(this.output, this.input);
        }

        this.lastSelected = false;
        this.selecting = false;
        this.output = this.input = null;
    }

    @Override
    protected void dragging(GuiContext context)
    {
        super.dragging(context);

        if (this.dragging && this.mouse == 0 && this.lastSelected && !this.selected.isEmpty())
        {
            int lastNodeX = (int) this.fromX(context.mouseX);
            int lastNodeY = (int) this.fromY(context.mouseY);

            for (T node : this.selected)
            {
                node.x += lastNodeX - this.lastNodeX;
                node.y += lastNodeY - this.lastNodeY;
            }

            this.lastNodeX = lastNodeX;
            this.lastNodeY = lastNodeY;
        }
    }

    @Override
    public void draw(GuiContext context)
    {
        super.draw(context);

        if (this.system.nodes.isEmpty())
        {
            int w = this.area.w / 2;

            GlStateManager.enableTexture2D();
            GuiDraw.drawMultiText(this.font, I18n.format("mappet.gui.nodes.info.empty_nodes"), this.area.mx(w), this.area.my(), 0xffffff, w, 12, 0.5F, 0.5F);
        }
        else if (this.notifyAboutMain && this.system.main == null)
        {
            String label = I18n.format("mappet.gui.nodes.info.empty_main");
            int w = this.font.getStringWidth(label);

            Gui.drawRect(this.area.x + 4, this.area.y + 4, this.area.x + 24 + w, this.area.y + 20, ColorUtils.HALF_BLACK);
            GlStateManager.color(1F, 0F, 0.1F, 1F);
            Icons.EXCLAMATION.render(this.area.x + 4, this.area.y + 4);
            this.font.drawStringWithShadow(label, this.area.x + 20, this.area.y + 8, 0xff0010);
        }
    }

    @Override
    protected void drawCanvas(GuiContext context)
    {
        super.drawCanvas(context);

        if (this.system == null)
        {
            return;
        }

        int thickness = Mappet.nodeThickness.get();

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.glLineWidth(thickness);

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        T lastSelected = this.selected.isEmpty() ? null : this.selected.get(this.selected.size() - 1);
        List<Vector2d> positions = new ArrayList<Vector2d>();

        /* Draw connections */
        if (thickness > 0)
        {
            builder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

            this.renderConnections(context, builder, positions, lastSelected);

            Tessellator.getInstance().draw();
        }

        /* Draw node boxes */
        Area main = null;

        for (T node : this.system.nodes.values())
        {
            Area nodeArea = this.getNodeArea(node);

            if (nodeArea.w > 25)
            {
                this.renderOutlets(context, node, nodeArea);
            }

            boolean hover = Area.SHARED.isInside(context);
            int index = this.selected.indexOf(node);

            int colorBg = hover ? 0xff080808 : 0xff000000;
            int colorFg = 0xaa000000 + this.system.getFactory().getColor(node);

            if (index >= 0)
            {
                int colorSh = index == this.selected.size() - 1 ? 0x0088ff : 0x0022aa;

                GuiDraw.drawDropShadow(nodeArea.x + 4, nodeArea.y + 4, nodeArea.ex() - 4, nodeArea.ey() - 4, 8, 0xff000000 + colorSh, colorSh);
            }

            Gui.drawRect(nodeArea.x + 1, nodeArea.y, nodeArea.ex() - 1, nodeArea.ey(), colorBg);
            Gui.drawRect(nodeArea.x, nodeArea.y + 1, nodeArea.ex(), nodeArea.ey() - 1, colorBg);
            GuiDraw.drawOutline(nodeArea.x + 3, nodeArea.y + 3, nodeArea.ex() - 3, nodeArea.ey() - 3, colorFg);

            if (node == this.system.main)
            {
                main = new Area();
                main.copy(nodeArea);
            }
        }

        for (T node : this.system.nodes.values())
        {
            Area nodeArea = this.getNodeArea(node);
            String title = node.getTitle();

            if (!title.isEmpty() && nodeArea.w > 40)
            {
                if (title.length() > 37)
                {
                    title = title.substring(0, 37) + "§r...";
                }

                GuiDraw.drawTextBackground(this.font, title, nodeArea.mx() - this.font.getStringWidth(title) / 2, nodeArea.my() - 4, 0xffffff, 0x88000000);
            }
        }

        /* Draw selected node's indices */
        for (int i = 0; i < positions.size(); i++)
        {
            Vector2d pos = positions.get(i);
            String label = String.valueOf(i);

            this.font.drawStringWithShadow(label, (int) pos.x - this.font.getStringWidth(label) / 2, (int) pos.y - 4, this.getIndexLabelColor(lastSelected, i));
        }

        /* Draw main entry node icon */
        if (main != null)
        {
            GlStateManager.color(1F, 1F, 1F, 1F);
            GuiDraw.drawOutlinedIcon(Icons.DOWNLOAD, main.mx(), main.y - 4, 0xffffffff, 0.5F, 1F);
        }

        GlStateManager.glLineWidth(1);

        /* Draw selection */
        if (this.selecting)
        {
            Gui.drawRect(this.lastX, this.lastY, context.mouseX, context.mouseY, 0x440088ff);
        }
    }

    private void renderOutlets(GuiContext context, T node, Area nodeArea)
    {
        Area output = this.getNodeOutletArea(nodeArea, true);
        Area input = this.getNodeOutletArea(nodeArea, false);

        boolean insideO = output.isInside(context);
        boolean insideI = input.isInside(context);

        int colorO = ColorUtils.multiplyColor(0xffffff, insideO ? 1F : 0.6F);
        int colorI = ColorUtils.multiplyColor(0xffffff, insideI ? 1F : 0.6F);

        if (this.output == node)
        {
            colorO = ACTIVE;

            if (insideI)
            {
                colorI = ELSE;
            }
        }
        else if (this.output != null)
        {
            if (insideO)
            {
                colorO = ELSE;
            }
            else if (insideI)
            {
                colorI = IF;
            }
        }

        if (this.input == node)
        {
            colorI = ACTIVE;

            if (insideO)
            {
                colorO = ELSE;
            }
        }
        else if (this.input != null)
        {
            if (insideI)
            {
                colorI = ELSE;
            }
            else if (insideO)
            {
                colorO = IF;
            }
        }

        GuiDraw.drawOutline(output.x, output.y, output.ex(), output.ey(), 0xff000000 + colorO);

        if (this.system.main != node)
        {
            GuiDraw.drawOutline(input.x, input.y, input.ex(), input.ey(), 0xff000000 + colorI);
        }
    }

    private void renderConnections(GuiContext context, BufferBuilder builder, List<Vector2d> positions, T lastSelected)
    {
        for (List<NodeRelation<T>> relations : this.system.relations.values())
        {
            for (int r = 0; r < relations.size(); r++)
            {
                NodeRelation<T> relation = relations.get(r);

                Area output = this.getNodeOutletArea(this.getNodeArea(relation.output), true);
                Area input = this.getNodeOutletArea(this.getNodeArea(relation.input), false);

                int x1 = input.mx();
                int y1 = input.my();
                int x2 = output.mx();
                int y2 = output.my();

                this.drawConnection(builder, context, relation.output, r, x1, y1, x2, y2, false);

                if (relation.output == lastSelected)
                {
                    positions.add(new Vector2d((x1 + x2) / 2F, (y1 + y2) / 2F));
                }
            }
        }

        if (this.isConnecting())
        {
            T node = this.output == null ? this.input : this.output;
            Area area = this.getNodeArea(node);
            Area outlet = this.getNodeOutletArea(area, node == this.output);

            int x1 = context.mouseX;
            int y1 = context.mouseY;
            int x2 = outlet.mx();
            int y2 = outlet.my();

            List<NodeRelation<T>> list = this.system.relations.get(node.getId());

            this.drawConnection(builder, context, node, list == null ? 0 : list.size(), x1, y1, x2, y2, true);
        }
    }

    /**
     * Draw the connection line
     */
    private void drawConnection(BufferBuilder builder, GuiContext context, T node, int r, int x1, int y1, int x2, int y2, boolean forceLine)
    {
        float factor = (context.tick + context.partialTicks) / 60F;
        final float segments = 8F;

        float opacity = this.getNodeActiveColorOpacity(node, r);
        int c1 = Mappet.nodePulseBackgroundMcLibPrimary.get() ? McLib.primaryColor.get() : Mappet.nodePulseBackgroundColor.get();
        int c2 = this.getNodeActiveColor(node, r);

        for (int i = 0; i < segments; i ++)
        {
            float factor1 = i / segments;
            float factor2 = (i + 1) / segments;
            float color1 = 1 - MathUtils.clamp(Math.abs((1 - factor1) - (factor % 1)) / 0.2F, 0F, 1F);
            float color2 = 1 - MathUtils.clamp(Math.abs((1 - factor2) - (factor % 1)) / 0.2F, 0F, 1F);

            color1 = Math.max(color1, 1 - MathUtils.clamp(Math.abs(((1 - factor1) + 1) - (factor % 1)) / 0.2F, 0F, 1F));
            color2 = Math.max(color2, 1 - MathUtils.clamp(Math.abs(((1 - factor2) + 1) - (factor % 1)) / 0.2F, 0F, 1F));

            color1 = Math.max(color1, 1 - MathUtils.clamp(Math.abs(((1 - factor1) - 1) - (factor % 1)) / 0.2F, 0F, 1F));
            color2 = Math.max(color2, 1 - MathUtils.clamp(Math.abs(((1 - factor2) - 1) - (factor % 1)) / 0.2F, 0F, 1F));

            ColorUtils.interpolate(this.a, c1, c2, color1, false);
            ColorUtils.interpolate(this.b, c1, c2, color2, false);

            this.a.a = opacity;
            this.b.a = opacity;

            if (y2 <= y1 || forceLine)
            {
                builder.pos(Interpolations.lerp(x1, x2, factor1), Interpolations.lerp(y1, y2, factor1), 0).color(a.r, a.g, a.b, a.a).endVertex();
                builder.pos(Interpolations.lerp(x1, x2, factor2), Interpolations.lerp(y1, y2, factor2), 0).color(b.r, b.g, b.b, b.a).endVertex();
            }
            else
            {
                if (i == segments / 2)
                {
                    builder.pos(Interpolations.lerp(x1, x2, 0.5F), y1, 0).color(a.r, a.g, a.b, a.a).endVertex();
                    builder.pos(Interpolations.lerp(x1, x2, 0.5F), y2, 0).color(b.r, b.g, b.b, b.a).endVertex();
                }
                else
                {
                    int y = i < segments / 2 ? y1 : y2;

                    builder.pos(Interpolations.lerp(x1, x2, i == segments / 2 + 1 ? 0.5F : factor1), y, 0).color(a.r, a.g, a.b, a.a).endVertex();
                    builder.pos(Interpolations.lerp(x1, x2, i == segments / 2 - 1 ? 0.5F : factor2), y, 0).color(b.r, b.g, b.b, b.a).endVertex();
                }
            }
        }
    }

    protected int getIndexLabelColor(T lastSelected, int i)
    {
        return 0xffffff;
    }

    protected int getNodeActiveColor(T output, int r)
    {
        return ACTIVE;
    }

    protected float getNodeActiveColorOpacity(T output, int r)
    {
        return 0.75F;
    }
}