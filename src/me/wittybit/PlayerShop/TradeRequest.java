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
  public boolean was_received;

  public TradeRequest(String creator, ItemStack offers, ItemStack receives, String buyer, boolean was_received)
  {
    this.creator = creator;
    this.offers = offers;
    this.receives = receives;
    this.buyer = buyer;
    this.was_received = was_received;
  }

  public static TradeRequest deserialize(Map<String, Object> map)
  {
    String creator = (String) map.get("creator");
    ItemStack offers = (ItemStack) map.get("offers");
    ItemStack receives = (ItemStack) map.get("receives");
    String buyer = null;
    boolean was_received = false;
    if (map.containsKey("buyer"))
    {
      buyer = (String) map.get("buyer");
    }
    if (map.containsKey("was_received"))
    {
      was_received = (boolean) map.get("was_received");
    }
    return new TradeRequest(creator, offers, receives, buyer, was_received);
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
    data.put("was_received", this.was_received);

    return data;
  }
}
