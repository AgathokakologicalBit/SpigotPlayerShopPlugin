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

public class TradeManager
{
  private final Main plugin;
  public List<TradeRequest> completed_trade_requests;
  public List<TradeRequest> pending_trade_requests;
  private FileConfiguration dataConfig = null;
  private File configFile = null;

  public TradeManager(Main plugin)
  {
    this.plugin = plugin;

    this.saveDefaultConfig();
    this.getConfig();

    try
    {
      this.completed_trade_requests = (List<TradeRequest>) this.dataConfig.get("completed_trade_requests");
      this.pending_trade_requests = (List<TradeRequest>) this.dataConfig.get("pending_trade_requests");
    }
    catch (Exception e)
    {
      plugin.getLogger().warning("Failed to load trades list from config");
    }

    if (null == this.completed_trade_requests)
    {
      this.completed_trade_requests = new ArrayList<>();
    }
    if (null == this.pending_trade_requests)
    {
      this.pending_trade_requests = new ArrayList<>();
    }
  }

  public void reloadConfig()
  {
    if (null == this.configFile)
    {
      this.configFile = new File(this.plugin.getDataFolder(), "trades.yml");
    }

    this.dataConfig = YamlConfiguration.loadConfiguration(this.configFile);

    InputStream defaultStream = this.plugin.getResource("trades.yml");
    if (defaultStream != null)
    {
      YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
      this.dataConfig.setDefaults(defaultConfig);
    }

    try
    {
      this.completed_trade_requests = (List<TradeRequest>) this.dataConfig.get("completed_trade_requests");
      this.pending_trade_requests = (List<TradeRequest>) this.dataConfig.get("pending_trade_requests");
    }
    catch (Exception e)
    {
      plugin.getLogger().warning("Failed to load trades list from config");
    }

    if (null == this.completed_trade_requests)
    {
      this.completed_trade_requests = new ArrayList<>();
    }
    if (null == this.pending_trade_requests)
    {
      this.pending_trade_requests = new ArrayList<>();
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
      this.configFile = new File(this.plugin.getDataFolder(), "trades.yml");
    }

    if (!this.configFile.exists())
    {
      this.plugin.saveResource("trades.yml", false);
    }
  }

  public boolean createTradeRequest(TradeRequest request)
  {
    try
    {
      this.pending_trade_requests.add(request);
      this.dataConfig.set("pending_trade_requests", this.pending_trade_requests);
      this.saveConfig();
    }
    catch (Exception e)
    {
      return false;
    }

    return true;
  }

  public void completeTrade(TradeRequest request)
  {
    try
    {
      this.pending_trade_requests.remove(request);
      this.completed_trade_requests.add(request);
      this.dataConfig.set("pending_trade_requests", this.pending_trade_requests);
      this.dataConfig.set("completed_trade_requests", this.completed_trade_requests);
      this.saveConfig();
    }
    catch (Exception e)
    {
      plugin.getLogger().log(Level.SEVERE, "Fatal error occurred!", e);
    }
  }
}
