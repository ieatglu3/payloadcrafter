package com.github.ieatglu3.payloadcrafter.example.spigot;

import com.github.ieatglu3.payloadcrafter.ClientboundCustomPayload;
import com.github.ieatglu3.payloadcrafter.CustomPayloadListener;
import com.github.ieatglu3.payloadcrafter.PayloadEvent;
import com.github.ieatglu3.payloadcrafter.ServerboundCustomPayload;

public class MyModPayloadListener extends CustomPayloadListener {

  public MyModPayloadListener() {
    super(MySpigotPlugin.PAYLOAD_REGISTRY);
  }

  @Override
  public void onPayloadReceive(PayloadEvent<ServerboundCustomPayload> event) {
    if (event.payloadType() == MyModHandshake.TYPE) {
      MyModHandshake payload = event.payloadCasted();
      String myModVersion = payload.getModVersion();
      String playerName = event.playerName();
      System.out.println("Received handshake from player " + playerName + " with mod version " + myModVersion);
    }
  }

  @Override
  public void onPayloadSend(PayloadEvent<ClientboundCustomPayload> event) {
    if (event.payloadType() == MyModHandshakeResponse.TYPE) {
      MyModHandshakeResponse payload = event.payloadCasted();
      String version = payload.getVersion();
      String playerName = event.playerName();
      System.out.println("Sending handshake response to player " + playerName + " with version " + version);
    }
  }
}