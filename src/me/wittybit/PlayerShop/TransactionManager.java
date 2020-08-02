package me.wittybit.PlayerShop;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class TransactionManager
{
  private final Main plugin;
  private FileConfiguration dataConfig = null;
  private File configFile = null;

  private List<Transaction> transactions;

  public TransactionManager(Main plugin)
  {
    this.plugin = plugin;

    this.saveDefaultConfig();
    this.getConfig();

    try
    {
      this.transactions = (List<Transaction>) this.dataConfig.get("transactions");
    }
    catch (Exception e)
    {
      plugin.getLogger().warning("Failed to load transactions from config");
    }

    if (null == this.transactions)
    {
      this.transactions = new ArrayList<>();
    }
  }

  public void reloadConfig()
  {
    if (null == this.configFile)
    {
      this.configFile = new File(this.plugin.getDataFolder(), "transactions.yml");
    }

    this.dataConfig = YamlConfiguration.loadConfiguration(this.configFile);

    InputStream defaultStream = this.plugin.getResource("transactions.yml");
    if (defaultStream != null)
    {
      YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
      this.dataConfig.setDefaults(defaultConfig);
    }

    this.transactions = (ArrayList<Transaction>) this.dataConfig.get("transactions");
    if (null == this.transactions)
    {
      this.transactions = new ArrayList<>();
    }
  }

  public FileConfiguration getConfig()
  {
    if (null == this.dataConfig)
    {
      this.reloadConfig();
    }

    return this.dataConfig;
  }

  public void saveConfig()
  {
    if (null == this.dataConfig || null == this.configFile)
    {
      return;
    }

    try
    {
      this.getConfig().save(this.configFile);
    }
    catch (IOException e)
    {
      plugin.getLogger().log(Level.SEVERE, "Could not save config to " + this.configFile, e);
    }
  }

  public void saveDefaultConfig()
  {
    if (null == this.configFile)
    {
      this.configFile = new File(this.plugin.getDataFolder(), "transactions.yml");
    }

    if (!this.configFile.exists())
    {
      this.plugin.saveResource("transactions.yml", false);
    }
  }

  public boolean transferMoney(AccountInfo sender_acc, String sender, AccountInfo receiver_acc, String receiver, long amount, String comment)
  {
    if (null != sender && sender_acc.balance < amount)
    {
      return false;
    }

    this.transactions.add(new Transaction(sender, receiver, amount, null, comment));
    sender_acc.balance -= amount;
    receiver_acc.balance += amount;
    this.dataConfig.set("transactions", this.transactions);
    this.saveConfig();

    return true;
  }
}
