package com.fantasticsource.servershutdowntimer;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import scala.actors.threadpool.Arrays;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.util.text.TextFormatting.AQUA;
import static net.minecraft.util.text.TextFormatting.WHITE;

public class Commands extends CommandBase
{
    private static int timer = -1, timerStart = -1;

    static
    {
        MinecraftForge.EVENT_BUS.register(Commands.class);
    }

    @Override
    public String getName()
    {
        return "stop";
    }

    @Override
    public List<String> getAliases()
    {
        ArrayList<String> names = new ArrayList<>();

        names.add("shutdown");

        return names;
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        return Arrays.asList(new String[]{"cancel"});
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return AQUA + "/" + getName() + " [n]" + WHITE + " - " + I18n.translateToLocalFormatted(ServerShutdownTimer.MODID + ".cmd.comment")
                + "\n" + AQUA + "/" + getName() + " cancel" + WHITE + " - " + I18n.translateToLocalFormatted(ServerShutdownTimer.MODID + ".cmd.cancel.comment");
    }

    public void execute(MinecraftServer server, ICommandSender sender, String[] args)
    {
        int time = 60;
        if (args.length > 0)
        {
            if (args[0].equals("cancel"))
            {
                if (timer <= -1) notifyCommandListener(sender, this, ServerShutdownTimer.MODID + ".error.notInProgress");
                else
                {
                    timer = -1;
                    tellAll(TextFormatting.GREEN + "Server shutdown cancelled");
                }
                return;
            }

            try
            {
                time = Integer.parseInt(args[0].trim());
            }
            catch (NumberFormatException e)
            {
            }
        }


        timer = time * 20;
        timerStart = timer;


        int seconds = time;

        int minutes = seconds / 60;
        seconds -= minutes * 60;

        int hours = minutes / 60;
        minutes -= hours * 60;

        String message = TextFormatting.RED + "SERVER IS SHUTTING DOWN IN ";
        if (hours > 0) message += hours == 1 ? hours + " HOUR, " : hours + " HOURS, ";
        if (hours > 0 || minutes > 0) message += minutes == 1 ? minutes + " MINUTE" + (hours > 0 ? "," : "") + " AND " : minutes + " MINUTES" + (hours > 0 ? "," : "") + " AND ";
        if (hours > 0 || minutes > 0 || seconds > 0) message += seconds == 1 ? seconds + " SECOND" : seconds + " SECONDS";
        tellAll(message + "!");
    }

    public static void tellAll(String message)
    {
        System.out.println(message);
        for (EntityPlayerMP player : FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers())
        {
            player.sendMessage(new TextComponentString(message));
        }
    }

    @SubscribeEvent
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        EntityPlayer player = event.player;
        if (player instanceof EntityPlayerMP && timer > -1)
        {
            int seconds = timer / 20;
            if (seconds < 10)
            {
                ((EntityPlayerMP) player).connection.disconnect(new TextComponentString("Connection denied: the server is shutting down in " + seconds + " second" + (seconds == 1 ? "" : "s") + "!"));
            }
            else
            {
                int minutes = seconds / 60;
                seconds -= minutes * 60;

                int hours = minutes / 60;
                minutes -= hours * 60;

                String message = TextFormatting.RED + "SERVER IS SHUTTING DOWN IN ";
                if (hours > 0) message += hours == 1 ? hours + " HOUR, " : hours + " HOURS, ";
                if (hours > 0 || minutes > 0) message += minutes == 1 ? minutes + " MINUTE" + (hours > 0 ? "," : "") + " AND " : minutes + " MINUTES" + (hours > 0 ? "," : "") + " AND ";
                if (hours > 0 || minutes > 0 || seconds > 0) message += seconds == 1 ? seconds + " SECOND" : seconds + " SECONDS";
                player.sendMessage(new TextComponentString(message + "!"));
            }
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event)
    {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        if (timer > -1 && event.phase == TickEvent.Phase.END)
        {
            if (timer == 0) server.initiateShutdown();
            else if (timer != timerStart && timer % 20 == 0)
            {
                int seconds = timer / 20;

                if (seconds <= 5)
                {
                    tellAll(TextFormatting.RED + "" + seconds);
                }
                else if (seconds == 10 || seconds == 20 || seconds == 30)
                {
                    tellAll(TextFormatting.RED + "SERVER IS SHUTTING DOWN IN " + seconds + " SECONDS!");
                }
                else if (seconds % 60 == 0)
                {
                    int minutes = seconds / 60;
                    if (minutes <= 30) tellAll(TextFormatting.RED + "SERVER IS SHUTTING DOWN IN " + minutes + " MINUTE" + (minutes == 1 ? "" : "S") + "!");

                    if (minutes % 60 == 0)
                    {
                        int hours = minutes / 60;
                        tellAll(TextFormatting.RED + "SERVER IS SHUTTING DOWN IN " + hours + " HOUR" + (hours == 1 ? "" : "S") + "!");
                    }
                }
            }

            timer--;
        }
    }
}
