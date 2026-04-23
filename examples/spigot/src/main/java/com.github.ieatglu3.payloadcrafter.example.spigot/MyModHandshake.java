package com.github.ieatglu3.payloadcrafter.example.spigot;

import com.github.ieatglu3.payloadcrafter.*;

public class MyModHandshake extends ServerboundCustomPayload {

  public static final CustomPayloadType TYPE = CustomPayloadType.serverboundConfig(
    MyModHandshake.class,
    Identifier.of("my_mod", "handshake"),
    buf ->
    {
      String version = buf.readUTF();
      return new MyModHandshake(version);
    });

  private final String version;
  public MyModHandshake(String version) {
    super(TYPE);
    this.version = version;
  }

  public String getModVersion()
  {
    return this.version;
  }
}
