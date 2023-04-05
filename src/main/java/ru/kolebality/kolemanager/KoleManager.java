package ru.kolebality.kolemanager;

import java.time.*;
import org.bukkit.event.*;
import org.bukkit.plugin.java.*;
import dev.jorel.commandapi.*;
import dev.jorel.commandapi.arguments.*;
import net.md_5.bungee.api.*;

public class KoleManager extends JavaPlugin implements Listener {
    final String PluginPrefix = "[" + ChatColor.AQUA + "Kolebality" + ChatColor.RESET + "]";
    boolean AutoRestart = true;

    @Override
    public final void onEnable() {
        getLogger().info("Adding commands...");
        addCommands();

        startChecker();

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("KoleManager is enabled");
    }

    void startChecker() {
        final var enabledTime = System.currentTimeMillis();

        getServer().getScheduler().runTaskTimer(this, task -> {
            if (!AutoRestart)
                return;

            final var time = System.currentTimeMillis();
            if (time - enabledTime > (24 * 2 * 60 * 60 * 1000)) {
                restartServer(30);
                task.cancel();
                return;
            }

            if (getServer().getOnlinePlayers().size() == 0) {
                final var ltime = LocalTime.now();
                if (ltime.getHour() == 6 && ltime.getMinute() == 0 && ltime.getSecond() < 5) {
                    restartServer(0);
                    task.cancel();
                }
            }
        }, 20, 20);
    }

    @Override
    public final void onDisable() {
        getLogger().info("KoleManager is disabled");
    }


    private final void addCommands() {
        new CommandAPICommand("km") //
                .withFullDescription("Включить/выключить авторестарт (изменения не сохраняются)") //
                .withArguments(new LiteralArgument("autorestart")) //
                .withArguments(new BooleanArgument("autorestart")) //
                .withPermission("kolemanager.admin") //
                .executes((executor, args) -> {
                    AutoRestart = (boolean) (Boolean) args[0];
                    executor.sendMessage(PluginPrefix + " Авторестарт " + (AutoRestart ? "включен" : "выключен"));
                }).register();

        new CommandAPICommand("km") //
                .withArguments(new LiteralArgument("autorestart")) //
                .withPermission("kolemanager.admin") //
                .executes((executor, args) -> {
                    executor.sendMessage(PluginPrefix + " Авторестарт " + (AutoRestart ? "включен" : "выключен"));
                }).register();

        new CommandAPICommand("km") //
                .withArguments(new LiteralArgument("restart")) //
                .withArguments(new IntegerArgument("seconds", 0, 60 * 60 - 1) //
                        .replaceSuggestions(ArgumentSuggestions.strings("0", "60")) //
                ) //
                .withPermission("kolemanager.admin") //
                .executes((executor, args) -> {
                    final var timeout = (int) (Integer) args[0];
                    restartServer(timeout);
                }).register();
    }

    String secToString(int seconds) {
        if (seconds >= 60 * 60 - 1)
            return "час";
        if (seconds == 60)
            return "минуту";

        if (seconds >= 60 * 60)
            seconds = 60 * 60 - 1;

        var minutes = seconds / 60;
        seconds = seconds % 60;

        String minutesstr;
        if (minutes > 5 && minutes <= 20)
            minutesstr = " минут ";
        else {
            minutesstr = switch (minutes % 10) {
            case 1 -> " минуту ";
            case 2, 3, 4 -> " минуты ";
            case 5, 6, 7, 8, 9, 0 -> " минут ";
            default -> " минут ";
            };
        }

        String secondsstr;
        if (seconds <= 5)
            secondsstr = "";
        else if (seconds > 5 && seconds <= 20)
            secondsstr = " секунд ";
        else {
            secondsstr = switch (seconds % 10) {
            case 1 -> " секунду";
            case 2, 3, 4 -> " секунды";
            case 5, 6, 7, 8, 9, 0 -> " секунд";
            default -> " секунд";
            };
        }


        return (minutes == 0 ? "" : (minutes + minutesstr)) + seconds + secondsstr;
    }

    void broadcastRestart(final int timeleft) {
        getServer().broadcastMessage(PluginPrefix + " Рестарт сервера через " + secToString(timeleft));
    }

    /**
     * Does not actually restart the server, just stops
     */
    void restartServer(final int timeoutsec) {
        getLogger().info("Restarting server in " + timeoutsec);
        broadcastRestart(timeoutsec);

        if (timeoutsec <= 0) {
            getServer().shutdown();
            return;
        }

        final var startTime = getServer().getWorlds().get(0).getFullTime() / 20;

        final Runnable callback = () -> {
            final int timeleft = (int) (timeoutsec - (getServer().getWorlds().get(0).getFullTime() / 20 - startTime));

            if (timeleft <= 5 || timeleft == 30 || timeleft == 60)
                broadcastRestart(timeleft);

            if (timeleft <= 0)
                getServer().shutdown();
        };

        getServer().getScheduler().runTaskTimer(this, callback, 20, 20);
    }

    public static KoleManager getPlugin() {
        return getPlugin(KoleManager.class);
    }
}
