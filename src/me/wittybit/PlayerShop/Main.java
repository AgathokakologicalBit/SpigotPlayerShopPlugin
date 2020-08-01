package me.wittybit.PlayerShop;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class Main extends JavaPlugin implements Listener
{
  HashMap<String, AccountInfo> accounts;

  @Override
  public void onEnable()
  {
    Bukkit.getLogger().info("Initializing plugin");

    this.saveDefaultConfig();
    if (!this.getConfig().contains("starting_balance"))
    {
      this.getConfig().set("starting_balance", 1000 * 100);
    }

    this.accounts = (HashMap<String, AccountInfo>) this.getConfig().get("accounts");
    if (null == this.accounts)
    {
      this.accounts = new HashMap<>();
    }

    this.getServer().getPluginManager().registerEvents(this, this);
  }

  @Override
  public void onDisable()
  {
    Bukkit.getLogger().info("Finishing everything up...");
    this.getConfig().set("accounts", this.accounts);
    this.saveConfig();
    Bukkit.getLogger().info("done.");
  }

  private void createDefaultAccount(Player player)
  {
    if (!accounts.containsKey(player.getUniqueId().toString()))
    {
      accounts.put(
          player.getUniqueId().toString(),
          new AccountInfo(player, this.getConfig().getLong("starting_balance"))
      );
      this.getLogger().info("Created a new account for player `" + player.getName() + "`");
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e)
  {
    this.getLogger().info("Player `" + e.getPlayer().getName() + "` has joined. Checking account info");
    Player player = e.getPlayer();
    createDefaultAccount(player);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    if ("account".equalsIgnoreCase(label))
    {
      if (args.length > 0)
      {
        if ("info".equalsIgnoreCase(args[0]))
        {
          if (sender instanceof Player)
          {
            Player player = (Player) sender;
            createDefaultAccount(player);
            AccountInfo info = this.accounts.get(player.getUniqueId().toString());

            player.sendMessage("=====  ACCOUNT INFO  =====");
            {
              TextComponent msg_name = new TextComponent("Owner: ");
              msg_name.setColor(ChatColor.GOLD);
              TextComponent message = new TextComponent(player.getDisplayName());
              message.setColor(ChatColor.AQUA);
              message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                   new ComponentBuilder(player.getName()).create()));
              player.spigot().sendMessage(msg_name, message);
            }
            {
              TextComponent msg_name = new TextComponent("Balance: ");
              msg_name.setColor(ChatColor.GOLD);
              TextComponent message = new TextComponent(
                  String.format("$%d.%02d", info.balance / 100, info.balance % 100)
              );
              message.setColor(ChatColor.AQUA);
              message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                   new ComponentBuilder("No transactions were made").color(
                                                       ChatColor.GRAY).create()));
              player.spigot().sendMessage(msg_name, message);
            }

            return true;
          }
          else
          {
            sender.sendMessage("=====  ACCOUNT INFO  =====");
            sender.sendMessage("Owner: server");
            sender.sendMessage("Balance: $inf");
            return true;
          }
        }
      }

      sender.sendMessage(ChatColor.RED + "Usage: /account info|transactions");
      return true;
    }

    return false;
  }
}
