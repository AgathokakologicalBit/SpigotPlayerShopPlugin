package me.wittybit.PlayerShop;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ShopEntry implements ConfigurationSerializable
{
  public String ownerUUID;
  public ItemStack stack;
  public long price;

  public ShopEntry(String owner, ItemStack stack, long price)
  {
    this.ownerUUID = owner;
    this.stack = stack;
    this.price = price;
  }

  public static ShopEntry deserialize(Map<String, Object> map)
  {
    try
    {
      String owner = (String) map.get("owner");
      ItemStack stack = (ItemStack) map.get("stack");
      long price = ((Number) map.get("price")).longValue();
      return new ShopEntry(owner, stack, price);
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
    data.put("owner", this.ownerUUID);
    data.put("stack", this.stack);
    data.put("price", this.price);
    return data;
  }
}
