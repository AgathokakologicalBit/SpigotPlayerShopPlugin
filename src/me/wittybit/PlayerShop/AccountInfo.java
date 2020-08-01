package me.wittybit.PlayerShop;

import com.sun.istack.internal.NotNull;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class AccountInfo implements ConfigurationSerializable
{
  public String ownerName;
  public String ownerDisplayName;
  public long balance;

  public AccountInfo(Player player, long balance)
  {
    this.ownerName = player.getName();
    this.ownerDisplayName = player.getDisplayName();
    this.balance = balance;
  }

  @Override
  public Map<String, Object> serialize()
  {
    HashMap<String, Object> data = new HashMap<>();
    data.put("ownerName", ownerName);
    data.put("ownerDisplayName", ownerDisplayName);
    data.put("balance", balance);
    return data;
  }
}
