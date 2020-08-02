package me.wittybit.PlayerShop;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class TradeRequest implements ConfigurationSerializable
{
  public String creator;
  public ItemStack offers;
  public ItemStack receives;
  public String buyer;

  public TradeRequest(String creator, ItemStack offers, ItemStack receives, String buyer)
  {
    this.creator = creator;
    this.offers = offers;
    this.receives = receives;
    this.buyer = buyer;
  }

  public static TradeRequest deserialize(Map<String, Object> map)
  {
    String creator = (String) map.get("creator");
    ItemStack offers = (ItemStack) map.get("offers");
    ItemStack receives = (ItemStack) map.get("receives");
    String buyer = null;
    if (map.containsKey("buyer"))
    {
      buyer = (String) map.get("buyer");
    }
    return new TradeRequest(creator, offers, receives, buyer);
  }

  @Override
  public Map<String, Object> serialize()
  {
    HashMap<String, Object> data = new HashMap<>();
    data.put("creator", this.creator);
    data.put("offers", this.offers);
    data.put("receives", this.receives);
    if (null != this.buyer)
    {
      data.put("buyer", this.buyer);
    }
    return data;
  }
}
