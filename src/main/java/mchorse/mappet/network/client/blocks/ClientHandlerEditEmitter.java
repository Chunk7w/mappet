package mchorse.mappet.network.client.blocks;

import mchorse.mappet.api.utils.Checker;
import mchorse.mappet.client.gui.GuiEmitterBlockScreen;
import mchorse.mappet.network.common.blocks.PacketEditEmitter;
import mchorse.mclib.network.ClientMessageHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ClientHandlerEditEmitter extends ClientMessageHandler<PacketEditEmitter>
{
    @Override
    @SideOnly(Side.CLIENT)
    public void run(EntityPlayerSP player, PacketEditEmitter message)
    {
        Checker checker = new Checker();

        checker.deserializeNBT(message.checker);
        Minecraft.getMinecraft().displayGuiScreen(new GuiEmitterBlockScreen(message.pos, checker, message.radius));
    }
}