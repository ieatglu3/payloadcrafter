payloadcrafter
==============

A lightweight library for handling custom network payloads in Minecraft servers, with built-in serialization and deserialization support.

Features
--------

- Serverbound and Clientbound payloads ✅
- Configuration and Play state payload support ✅
- Automatic payload routing and deserialization ✅
- Type-safe payload registry system ✅
- Platform agonistic ✅

### Basic Usage

###### Defining a _Serverbound_ Payload

```java
public class MyModHandshake extends ServerboundCustomPayload {
  public static final CustomPayloadType TYPE = CustomPayloadType.serverboundConfig(
    MyModHandshake.class, // payload class
    Identifier.of("my_mod", "handshake"), // unique identifier
    buf -> { // deserializer function
      String version = buf.readUTF();
      return new MyModHandshake(version);
    });

  private final String version;

  public MyModHandshake(String version) {
    super(TYPE);
    this.version = version;
  }

  public String getModVersion() {
    return this.version;
  }
}
```

###### Defining a _Clientbound_ Payload

```java
public class MyModUpdate extends ClientboundCustomPayload {
  public static final CustomPayloadType TYPE = CustomPayloadType.clientboundPlay(
    MyModUpdate.class, // payload class
    Identifier.of("my_mod", "update"), // unique identifier
    buf -> { // deserializer function
      int value = buf.readInt();
      return new MyModUpdate(value);
    });
  
    private final int value;
    
    public MyModUpdate(int value) {
      super(TYPE);
      this.value = value;
    }
    
    public int getValue() {
      return this.value;
    }
    
    @Override
    public void write(WrappedByteBuf buffer) {
      buffer.writeInt(value);
    }
}
```

###### Defining a Payload Registry

```java
  public static final CustomPayloadRegistry REGISTRY = CustomPayloadRegistry.builder()
    .register(MyModHandshake.TYPE)
    .build();
```

###### Handling Payloads

```java

public final CustomPayloadListener listener = new MyModPayloadListener();

public class MyModPayloadListener extends CustomPayloadListener {
  public MyModPayloadListener() {
    super(REGISTRY);
  }

  @Override
  public void onPayloadReceive(PayloadEvent<ServerboundCustomPayload> event) {
    if (event.payloadType() == MyModHandshake.TYPE) {
      MyModHandshake payload = event.payloadCasted();
      String version = payload.getModVersion();
      String player = event.playerName();
      System.out.println("Received handshake from " + player);
    }
  }
}

public void onEnable() {
  listener.startListening();
}

public void onDisable() {
  listener.stopListening();
}
```

###### Sending _Clientbound_ Payloads

```java
public static void sendPayload(ClientboundCustomPayload payload, User player) {
  payload.send(player);
}

public static void sendPayload(ClientboundCustomPayload payload, UUID playerID) {
  payload.send(playerID);
}
```
-------------

### Payload Types

Payloads are categorized by direction and state:

- **Direction**: Serverbound (client to server) or Clientbound (server to client)
- **State**: Configuration or Play

Use the appropriate factory method when defining your payload type:
- `CustomPayloadType.serverboundConfig(...)`
- `CustomPayloadType.serverboundPlay(...)`
- `CustomPayloadType.clientboundConfig(...)`
- `CustomPayloadType.clientboundPlay(...)`

### Registries

The `CustomPayloadRegistry` acts as a router for incoming payloads. All payloads must be registered before the listener starts to ensure they are properly recognized and deserialized.