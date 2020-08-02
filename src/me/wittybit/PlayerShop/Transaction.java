package me.wittybit.PlayerShop;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class Transaction implements ConfigurationSerializable
{
  public String from;
  public String to;
  public long money_amount;
  public ShopEntry entry;
  public String comment;

  public Transaction(String from, String to, long amount, ShopEntry entry, String comment)
  {
    this.from = from;
    this.to = to;
    this.money_amount = amount;
    this.entry = entry;
    this.comment = comment;
  }

  public static Transaction deserialize(Map<String, Object> map)
  {
    try
    {
      String from = (String) map.get("from");
      String to = (String) map.get("to");
      long amount = ((Number) map.get("money_amount")).longValue();
      ShopEntry entry = (ShopEntry) map.get("entry");
      String comment = (String) map.get("comment");
      return new Transaction(from, to, amount, entry, comment);
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
    data.put("from", this.from);
    data.put("to", this.to);
    data.put("money_amount", this.money_amount);
    data.put("entry", this.entry);
    data.put("comment", this.comment);
    return data;
  }
}
