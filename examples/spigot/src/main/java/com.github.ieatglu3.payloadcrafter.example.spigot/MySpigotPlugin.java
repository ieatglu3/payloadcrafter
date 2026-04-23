package com.github.ieatglu3.payloadcrafter.example.spigot;

import com.github.ieatglu3.payloadcrafter.CustomPayloadListener;
import com.github.ieatglu3.payloadcrafter.CustomPayloadRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public class MySpigotPlugin extends JavaPlugin {

  private CustomPayloadListener payloadListener;

  // if a payload type is not registered, it will be ignored by any listener using this registry!
  public static final CustomPayloadRegistry PAYLOAD_REGISTRY = CustomPayloadRegistry.builder()
    .register(MyModHandshake.TYPE)
    .register(MyModHandshakeResponse.TYPE)
    .build();

  @Override
  public void onEnable() {
    this.payloadListener = new MyModPayloadListener();
    this.payloadListener.startListening();
  }

  @Override
  public void onDisable() {
    if (this.payloadListener != null)
      this.payloadListener.stopListening();
  }
}