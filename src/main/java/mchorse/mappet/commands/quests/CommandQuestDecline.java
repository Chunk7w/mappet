package mchorse.mappet.commands.quests;

import mchorse.mappet.capabilities.character.Character;
import mchorse.mappet.capabilities.character.ICharacter;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

public class CommandQuestDecline extends CommandQuestBase
{
    @Override
    public String getName()
    {
        return "decline";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "mappet.commands.mp.quest.decline";
    }

    @Override
    public String getSyntax()
    {
        return "{l}{6}/{r}mp {8}quest decline{r} {7}<player> <id>{r}";
    }

    @Override
    public void executeCommand(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer player = getPlayer(server, sender, args[0]);
        ICharacter character = Character.get(player);

        if (character != null && character.getQuests().decline(args[1], player))
        {
            this.getL10n().success(sender, "quest.declined", args[1], player.getName());
        }
    }
}
