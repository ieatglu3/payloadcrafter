package com.github.ieatglu3.payloadcrafter.example.spigot;

import com.github.ieatglu3.payloadcrafter.ClientboundCustomPayload;
import com.github.ieatglu3.payloadcrafter.CustomPayloadType;
import com.github.ieatglu3.payloadcrafter.Identifier;
import com.github.ieatglu3.payloadcrafter.WrappedByteBuf;
import org.jetbrains.annotations.NotNull;

public class MyModHandshakeResponse extends ClientboundCustomPayload {

  public static final CustomPayloadType TYPE = CustomPayloadType.clientboundConfig(
    MyModHandshakeResponse.class,
    Identifier.of("my_mod", "handshake_response"),
    buf ->
    {
      String version = buf.readUTF();
      return new MyModHandshakeResponse(version);
    });

  private final String version;
  public MyModHandshakeResponse(String version) {
    super(TYPE);
    this.version = version;
  }

  @Override
  public void write(@NotNull WrappedByteBuf buffer)
  {
    buffer.writeUTF(this.version);
  }

  public String getVersion()
  {
    return this.version;
  }
}
