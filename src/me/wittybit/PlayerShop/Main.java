package me.wittybit.PlayerShop;

import com.mojang.datafixers.util.Pair;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener
{
  static
  {
    ConfigurationSerialization.registerClass(AccountInfo.class, "AccountInfo");
    ConfigurationSerialization.registerClass(Transaction.class, "Transaction");
    ConfigurationSerialization.registerClass(ShopEntry.class, "ShopEntry");
    ConfigurationSerialization.registerClass(TradeRequest.class, "TradeRequest");
  }

  public TransactionManager transactionManager;
  public TradeManager tradeManager;

  private AccountInfo serverAccount;
  private HashMap<String, AccountInfo> accounts;
  private HashMap<String, Pair<Inventory, Integer>> tradesBrowsingPageState;

  public static String convertItemStackToJsonRegular(ItemStack itemStack)
  {
    // First we convert the item stack into an NMS itemstack
    net.minecraft.server.v1_16_R1.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
    NBTTagCompound compound = new NBTTagCompound();
    compound = nmsItemStack.save(compound);

    return compound.toString();
  }

  public static boolean hasItems(Inventory inventory, Material type, int amount)
  {
    if (amount <= 0) return true;
    if (type == Material.AIR) return true;

    int size = inventory.getSize();
    int totalAmount = 0;
    for (int slot = 0; slot < size; slot++)
    {
      ItemStack is = inventory.getItem(slot);
      if (is == null) continue;
      if (is.getType() != type) continue;
      totalAmount += is.getAmount();
      if (totalAmount >= amount) return true;
    }

    return false;
  }

  public static void removeItems(Inventory inventory, Material type, int amount)
  {
    if (amount <= 0) return;
    int size = inventory.getSize();
    for (int slot = 0; slot < size; slot++)
    {
      ItemStack is = inventory.getItem(slot);
      if (is == null) continue;
      if (type == is.getType())
      {
        int newAmount = is.getAmount() - amount;
        if (newAmount > 0)
        {
          is.setAmount(newAmount);
          break;
        }
        else
        {
          inventory.clear(slot);
          amount = -newAmount;
          if (amount == 0) break;
        }
      }
    }
  }

  public String getPlayerIdByName(String name)
  {
    for (Map.Entry<String, AccountInfo> entry : this.accounts.entrySet())
    {
      if (Objects.equals(name, entry.getValue().ownerName))
      {
        return entry.getKey();
      }
    }

    return null;
  }

  @Override
  public void onEnable()
  {
    Bukkit.getLogger().info("Initializing plugin");

    serverAccount = new AccountInfo(">#", "{|SERVER|}", 0);
    this.transactionManager = new TransactionManager(this);
    this.tradeManager = new TradeManager(this);
    this.tradesBrowsingPageState = new HashMap<>();

    this.saveDefaultConfig();
    if (!this.getConfig().contains("starting_balance"))
    {
      this.getConfig().set("starting_balance", 1000 * 100);
    }

    this.accounts = new HashMap<>();
    try
    {
      Map<String, Object> values = this.getConfig().getConfigurationSection("accounts").getValues(false);

      for (Map.Entry<String, Object> entry : values.entrySet())
      {
        this.accounts.put(entry.getKey(), (AccountInfo) entry.getValue());
      }

      this.getServer().getPluginManager().registerEvents(this, this);
    }
    catch (Exception e)
    {
      this.getLogger().warning("Failed to load accounts info from config");
    }
  }

  @Override
  public void onDisable()
  {
    Bukkit.getLogger().info("Finishing everything up...");
  }

  private void createDefaultAccount(Player player)
  {
    if (!this.accounts.containsKey(player.getUniqueId().toString()))
    {
      this.accounts.put(
          player.getUniqueId().toString(),
          new AccountInfo(player, this.getConfig().getLong("starting_balance"))
      );
      this.getLogger().info("Created a new account for player `" + player.getName() + "`");
      this.getConfig().createSection("accounts", this.accounts);
      this.saveConfig();
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e)
  {
    this.getLogger().info("Player `" + e.getPlayer().getName() + "` has joined. Checking account info");
    Player player = e.getPlayer();
    createDefaultAccount(player);
  }

  public long parseCash(String data)
  {
    try
    {
      if (data.contains("."))
      {
        String[] parts = data.split("\\.");
        if (parts.length > 2) return 0;
        long amount = Long.parseLong(parts[0]) * 100;
        long rest = Long.parseLong(parts[1]);
        if (rest > 99) return 0;
        if (rest < 10) rest *= 10;
        amount += rest;
        return amount;
      }

      return Long.parseLong(data) * 100;
    }
    catch (Exception e)
    {
      return 0;
    }
  }

  @EventHandler
  public void onClick(InventoryClickEvent event)
  {
    Pair<Inventory, Integer> p = this.tradesBrowsingPageState.getOrDefault(event.getWhoClicked()
                                                                               .getUniqueId()
                                                                               .toString(), new Pair<>(null, 0));
    if (!event.getInventory().equals(p.getFirst()))
    {
      return;
    }

    if (event.getCurrentItem() == null) return;
    if (event.getCurrentItem().getItemMeta() == null) return;
    if (event.getCurrentItem().getItemMeta().getDisplayName() == null) return;

    event.setCancelled(true);
    Player player = (Player) event.getWhoClicked();

    int nextPage = p.getSecond();
    if (event.getSlot() == 0 + 9 * 5) nextPage -= 1;
    if (event.getSlot() == 8 + 9 * 5) nextPage += 1;

    if (nextPage != p.getSecond())
    {
      this.getLogger().info("Trying to visit page " + Integer.toString(nextPage));
      this.getLogger()
          .info("Number of pending trade requests: " + Integer.toString(tradeManager.pending_trade_requests.size()));
      if (nextPage < 0) return;
      if (nextPage * 9 >= tradeManager.pending_trade_requests.size())
      {
        return;
      }

      this.tradesBrowsingPageState.put(player.getUniqueId().toString(), new Pair<>(p.getFirst(), nextPage));
      drawTradeBrowsingInventory(player, p.getFirst(), nextPage);
      return;
    }

    if (event.getSlot() / 9 == 4)
    {
      // Removes "id: " prefix from the 3rd line of Lore
      int id = Integer.parseInt(event.getCurrentItem().getItemMeta().getLore().get(2).substring(4));
      Optional<TradeRequest> request = tradeManager.pending_trade_requests.stream()
          .filter(tr -> tr.hashCode() == id)
          .findFirst();
      if (!request.isPresent())
      {
        player.closeInventory();
        player.sendMessage(org.bukkit.ChatColor.RED + "The request does no longer exist!");
        return;
      }

      if (request.get().buyer != null)
      {
        player.closeInventory();
        player.sendMessage(org.bukkit.ChatColor.RED + "Something went wrong! (It was already bought somehow OwO)");
        return;
      }

      if (!hasItems(player.getInventory(), request.get().receives.getType(), request.get().receives.getAmount()))
      {
        player.closeInventory();
        player.sendMessage(org.bukkit.ChatColor.RED + "You do not have enough items for trading!");
        return;
      }

      int slot = player.getInventory().firstEmpty();
      if (slot < 0)
      {
        player.closeInventory();
        player.sendMessage(org.bukkit.ChatColor.RED + "No empty slots to receive items!");
        return;
      }

      removeItems(player.getInventory(), request.get().receives.getType(), request.get().receives.getAmount());
      player.getInventory().setItem(slot, request.get().offers);
      request.get().buyer = player.getUniqueId().toString();
      tradeManager.completeTrade(request.get());
      drawTradeBrowsingInventory(player, p.getFirst(), p.getSecond());
    }
  }

  private void drawTradeBrowsingInventory(Player player, Inventory inv, int page)
  {
    inv.clear();
    if (page * 9 >= tradeManager.pending_trade_requests.size())
      page = (tradeManager.pending_trade_requests.size() - 1) / 9;

    for (int col = 0; col < 9; ++col)
    {
      if (page * 9 + col >= tradeManager.pending_trade_requests.size()) break;

      TradeRequest request = tradeManager.pending_trade_requests.get(page * 9 + col);

      ItemStack ownerHead = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta) ownerHead.getItemMeta();

      meta.setOwner(this.accounts.get(request.creator).ownerName);
      ownerHead.setItemMeta(meta);
      inv.setItem(col + 9 * 0, ownerHead);
      inv.setItem(col + 9 * 2, new ItemStack(Material.CHAIN));
      inv.setItem(col + 9 * 1, request.offers);
      inv.setItem(col + 9 * 3, request.receives);

      if (hasItems(player.getInventory(), request.receives.getType(), request.receives.getAmount()))
      {
        ItemStack doTrade = new ItemStack(Material.FLOWER_BANNER_PATTERN);
        ItemMeta doTradeMeta = doTrade.getItemMeta();
        doTradeMeta.setDisplayName("Perform trade");
        List<String> doTradeLore = new ArrayList<>();
        doTradeLore.add("Will exchange the offered item(s)");
        doTradeLore.add("by the one(s) in the inventory");
        doTradeLore.add("id: " + Integer.toString(request.hashCode()));
        doTradeMeta.setLore(doTradeLore);
        doTrade.setItemMeta(doTradeMeta);
        inv.setItem(col + 9 * 4, doTrade);
      }
    }

    if (page > 0)
    {
      ItemStack prevPage = new ItemStack(Material.STONE_BUTTON);
      ItemMeta prevPageMeta = prevPage.getItemMeta();
      prevPageMeta.setDisplayName(String.format("Previous page %d/%d",
                                                page,
                                                (tradeManager.pending_trade_requests.size() + 8) / 9));
      prevPage.setItemMeta(prevPageMeta);
      inv.setItem(0 + 9 * 5, prevPage);
    }

    if (page + 2 <= (tradeManager.pending_trade_requests.size() + 8) / 9)
    {
      ItemStack nextPage = new ItemStack(Material.STONE_BUTTON);
      ItemMeta nextPageMeta = nextPage.getItemMeta();
      nextPageMeta.setDisplayName(String.format("Next page %d/%d",
                                                page + 2,
                                                (tradeManager.pending_trade_requests.size() + 8) / 9));
      nextPage.setItemMeta(nextPageMeta);
      inv.setItem(8 + 9 * 5, nextPage);
    }

    player.openInventory(inv);
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

      sender.sendMessage(org.bukkit.ChatColor.RED + "Usage: /account info|transactions");
      return true;
    }
    else if ("transfer".equalsIgnoreCase(label))
    {
      if (args.length < 2)
      {
        sender.sendMessage(org.bukkit.ChatColor.RED + "Usage: /transfer <player> <amount> [<comment>]");
        return true;
      }

      String from = (sender instanceof Player) ? ((Player) sender).getUniqueId().toString() : null;
      AccountInfo from_acc = (sender instanceof Player)
                             ? accounts.get(from)
                             : serverAccount;

      String target = getPlayerIdByName(args[0]);
      if (null == target)
      {
        sender.sendMessage(org.bukkit.ChatColor.RED + "Specified recipient (" +
                               org.bukkit.ChatColor.BOLD + args[0] + org.bukkit.ChatColor.RESET + "" +
                               org.bukkit.ChatColor.RED + ") does not exist in the database");
        return true;
      }

      AccountInfo target_acc = accounts.get(target);
      String message = Arrays.stream(args).skip(2).collect(Collectors.joining(" "));

      long amount = parseCash(args[1]);
      if (amount <= 0)
      {
        sender.sendMessage(org.bukkit.ChatColor.RED + "Incorrect amount of money to transfer was specified");
        return true;
      }

      if (transactionManager.transferMoney(from_acc, from, target_acc, target, amount, message))
      {
        String amount_str = String.format("$%d.%02d", amount / 100, amount % 100);

        sender.sendMessage(
            org.bukkit.ChatColor.GREEN +
                "Successfully transferred " + amount_str +
                " to " + target_acc.ownerDisplayName
        );

        Player recipient_player = getServer().getPlayer(args[0]);
        if (null != recipient_player)
        {
          // If player is online - send notification
          recipient_player.sendMessage(
              org.bukkit.ChatColor.GREEN +
                  "You received a transfer of " + amount_str +
                  " from " + from_acc.ownerDisplayName
          );
          if (!"".equals(message))
          {
            recipient_player.sendMessage(
                org.bukkit.ChatColor.GREEN + "  with attached message: " +
                    org.bukkit.ChatColor.RESET + message);
          }
        }

        this.getConfig().createSection("accounts", this.accounts);
        this.saveConfig();
      }
      else
      {
        sender.sendMessage(org.bukkit.ChatColor.RED + "Failed to send the specified amount of money to the recipient");
      }

      return true;
    }
    else if ("setbalance".equalsIgnoreCase(label))
    {
      if ((sender instanceof Player) && !sender.hasPermission("account.change"))
      {
        sender.sendMessage(org.bukkit.ChatColor.RED + "You do not have a permission to use this command!");
        return true;
      }

      sender.sendMessage(org.bukkit.ChatColor.ITALIC + "" + org.bukkit.ChatColor.DARK_GREEN + "// TODO: Implement command");
    }
    else if ("tradeit".equalsIgnoreCase(label))
    {
      if (!(sender instanceof Player))
      {
        sender.sendMessage(org.bukkit.ChatColor.RED + "Available to players only");
        return true;
      }

      if (args.length < 1)
      {
        sender.sendMessage(org.bukkit.ChatColor.RED + "Usage: /tradeit <material> [<amount>(default=1)]");
        return true;
      }

      Player player = (Player) sender;
      ItemStack held_item = player.getInventory().getItemInMainHand();
      if (held_item.getType().equals(Material.AIR))
      {
        player.sendMessage(org.bukkit.ChatColor.RED + "Sorry for inconvenience, but selling air is not supported in the current version");
        return true;
      }

      Material mat = Material.getMaterial(args[0].toUpperCase());
      if (null == mat)
      {
        player.sendMessage(org.bukkit.ChatColor.RED + "Material with name " + org.bukkit.ChatColor.BOLD + args[0].toUpperCase() +
                               org.bukkit.ChatColor.RESET + "" + org.bukkit.ChatColor.RED + " does not exist!");
        return true;
      }

      ItemStack stack = new ItemStack(mat, 1);
      if (args.length > 1)
      {
        try
        {
          final int amount = Integer.parseUnsignedInt(args[1]);
          stack.setAmount(amount);
        }
        catch (Exception e)
        {
          player.sendMessage(org.bukkit.ChatColor.RED + args[1] + " is not a number!");
          return true;
        }
      }

      {
        if (!this.tradeManager.createTradeRequest(
            new TradeRequest(player.getUniqueId().toString(),
                             held_item,
                             stack,
                             null)
        ))
        {
          player.sendMessage(org.bukkit.ChatColor.RED + "Failed to create a trade request!");
          return true;
        }

        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        String offer_item_name = (held_item.getItemMeta() != null && held_item.getItemMeta().hasDisplayName())
                                 ? held_item.getItemMeta().getDisplayName()
                                 : held_item.getType().getKey().toString();
        String request_item_name = (stack.getItemMeta() != null && stack.getItemMeta().hasDisplayName())
                                   ? stack.getItemMeta().getDisplayName()
                                   : stack.getType().getKey().toString();

        TextComponent msg_name = new TextComponent("Created a trade: ");
        msg_name.setColor(ChatColor.GREEN);

        TextComponent msg_offer = new TextComponent(
            String.format("[%dx %s]", held_item.getAmount(), offer_item_name)
        );
        msg_offer.setColor(ChatColor.DARK_GREEN);
        BaseComponent[] offerHoverEventComponents = new BaseComponent[]{
            new TextComponent(convertItemStackToJsonRegular(held_item))
        };
        msg_offer.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, offerHoverEventComponents));

        TextComponent msg_for = new TextComponent(" for ");
        msg_for.setColor(ChatColor.GREEN);

        TextComponent msg_request = new TextComponent(
            String.format("[%dx %s]", stack.getAmount(), request_item_name)
        );
        msg_request.setColor(ChatColor.DARK_GREEN);
        BaseComponent[] requestHoverEventComponents = new BaseComponent[]{
            new TextComponent(convertItemStackToJsonRegular(stack))
        };
        msg_request.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, requestHoverEventComponents));

        player.spigot().sendMessage(msg_name, msg_offer, msg_for, msg_request);
      }

      return true;
    }
    else if ("trades".equalsIgnoreCase(label))
    {
      if (!(sender instanceof Player))
      {
        sender.sendMessage(org.bukkit.ChatColor.RED + "Available to players only");
        return true;
      }

      Player player = (Player) sender;
      Inventory inv = Bukkit.createInventory(null,
                                             9 * 6,
                                             org.bukkit.ChatColor.BLACK + "" + org.bukkit.ChatColor.BOLD + "Active Trades List");

      tradesBrowsingPageState.put(player.getUniqueId().toString(), new Pair<Inventory, Integer>(inv, 0));
      this.drawTradeBrowsingInventory(player, inv, 0);
    }

    return false;
  }
}
