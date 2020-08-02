package me.wittybit.PlayerShop;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class AccountInfo implements ConfigurationSerializable
{
  public String ownerName;
  public String ownerDisplayName;
  public long balance;

  public AccountInfo(String name, String displayName, long balance)
  {
    this.ownerName = name;
    this.ownerDisplayName = displayName;
    this.balance = balance;
  }

  public AccountInfo(Player player, long balance)
  {
    this.ownerName = player.getName();
    this.ownerDisplayName = player.getDisplayName();
    this.balance = balance;
  }

  public static AccountInfo deserialize(Map<String, Object> map)
  {
    try
    {
      String name = (String) map.get("ownerName");
      String displayName = (String) map.get("ownerName");
      long balance = ((Number) map.get("balance")).longValue();
      return new AccountInfo(name, displayName, balance);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public Map<String, Object> serialize()
  {
    HashMap<String, Object> data = new HashMap<>();
    data.put("ownerName", this.ownerName);
    data.put("ownerDisplayName", this.ownerDisplayName);
    data.put("balance", this.balance);
    return data;
  }
}
