package com.github.ieatglu3.payloadcrafter

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.PacketEventsAPI
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.netty.channel.ChannelHelper
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.User
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerPluginMessage
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.EnumMap
import java.util.HashMap
import java.util.Optional
import java.util.UUID

/**
 * A wrapper around ByteBuffer for easier serialization and deserialization of data
 */
class WrappedByteBuf(private val buffer: ByteBuffer)
{

  private fun ensureCapacity(size: Int)
  {
    val capacity = this.capacity()
    if (capacity - this.position() < size)
    {
      val newBuffer = ByteBuffer.allocate(capacity * 2 + size)
      this.buffer.flip()
      newBuffer.put(this.buffer)
      this.buffer.clear()
      this.buffer.put(newBuffer)
    }
  }

  /**
   * Gets the underlying ByteBuffer
   * @return the ByteBuffer
   */
  fun handle(): ByteBuffer = this.buffer

  /**
   * Returns the number of bytes that can be read from the buffer
   * @return number of readable bytes
   */
  fun readableBytes(): Int = this.buffer.limit() - this.buffer.position()

  /**
   * Returns the total capacity of the buffer
   * @return capacity of the buffer
   */
  fun capacity(): Int = this.buffer.capacity()

  /**
   * Returns the current position of the buffer
   * @return position of the buffer
   */
  fun position(): Int = this.buffer.position()

  /**
   * Writes a single byte to the buffer
   * @param value
   */
  fun writeByte(value: Byte)
  {
    this.ensureCapacity(1)
    this.buffer.put(value)
  }

  /**
   * Writes a boolean value to the buffer as a single byte
   * @param value
   */
  fun writeBoolean(value: Boolean)
  {
    this.ensureCapacity(1)
    this.buffer.put(if (value) 1 else 0)
  }

  /**
   * Writes a short value to the buffer
   * @param value
   */
  fun writeShort(value: Short)
  {
    this.ensureCapacity(Short.SIZE_BYTES)
    this.buffer.putShort(value)
  }

  /**
   * Writes an int value to the buffer
   * @param value
   */
  fun writeInt(value: Int)
  {
    this.ensureCapacity(Int.SIZE_BYTES)
    this.buffer.putInt(value)
  }

  /**
   * Writes a float value to the buffer
   * @param value
   */
  fun writeFloat(value: Float)
  {
    this.ensureCapacity(Float.SIZE_BYTES)
    this.buffer.putFloat(value)
  }

  /**
   * Writes a double value to the buffer
   * @param value
   */
  fun writeDouble(value: Double)
  {
    this.ensureCapacity(Double.SIZE_BYTES)
    this.buffer.putDouble(value)
  }

  /**
   * Writes a byte array to the buffer
   * @param bytes the byte array to write
   */
  fun writeBytes(bytes: ByteArray)
  {
    this.ensureCapacity(bytes.size)
    this.buffer.put(bytes)
  }

  /**
   * Writes a varint to the buffer
   * @param value the integer to write
   */
  fun writeVarInt(value: Int)
  {
    var value = value
    do
    {
      if ((value and -0x80) == 0)
      {
        this.writeByte(value.toByte())
        return
      }
      this.writeByte((value and 0x7F or 0x80).toByte())
      value = value ushr 7
    } while (true)
  }

  /**
   * Writes a UTF-8 string to the buffer
   * @param value the string to write
   */
  fun writeUTF(value: String)
  {
    val bytes = value.toByteArray(Charsets.UTF_8)
    this.writeVarInt(bytes.size)
    this.writeBytes(bytes)
  }

  /**
   * Writes an optional value to the buffer
   * @param value the value to write, or null if not present
   * @param writer writer if present
   */
  inline fun <T> writeOptional(value: T?, crossinline writer: (WrappedByteBuf) -> Unit)
  {
    if (value != null)
    {
      this.writeBoolean(true)
      writer(this)
    }
    else
      this.writeBoolean(false)
  }

  /**
   * Reads a single byte from the buffer
   * @return the byte read
   */
  fun readByte(): Byte = this.buffer.get()

  /**
   * Reads a boolean value from the buffer
   * @return the boolean value read
   */
  fun readBoolean(): Boolean = this.buffer.get().toInt() != 0

  /**
   * Reads a short value from the buffer
   * @return the short value read
   */
  fun readShort(): Short = this.buffer.getShort()

  /**
   * Reads an int value from the buffer
   * @return the int value read
   */
  fun readInt(): Int = this.buffer.getInt()

  /**
   * Reads a float value from the buffer
   * @return the float value read
   */
  fun readFloat(): Float = this.buffer.getFloat()

  /**
   * Reads a double value from the buffer
   * @return the double value read
   */
  fun readDouble(): Double = this.buffer.getDouble()

  /**
   * Reads a byte array of the given length from the buffer
   * @param length the length of the byte array to read
   * @return the byte array read
   */
  fun readBytes(length: Int): ByteArray
  {
    val bytes = ByteArray(length)
    this.buffer.get(bytes)
    return bytes
  }

  /**
   * Reads a byte array of the remaining readable bytes from the buffer
   * @return the byte array read
   */
  fun readBytes(): ByteArray
  {
    val bytes = ByteArray(this.readableBytes())
    this.buffer.get(bytes)
    return bytes
  }

  /**
   * Reads a varint from the buffer
   * @return the integer read
   */
  fun readVarInt(): Int
  {
    var value = 0
    var length = 0
    var currentByte: Byte
    do
    {
      currentByte = readByte()
      value = value or ((currentByte.toInt() and 0x7F) shl (length * 7))
      length++
      if (length > 5)
      {
        throw RuntimeException("VarInt is too large. Must be smaller than 5 bytes.")
      }
    } while ((currentByte.toInt() and 0x80) == 0x80)
    return value
  }

  /**
   * Reads a UTF-8 string from the buffer
   * @return the string read
   */
  fun readUTF(): String
  {
    val j = readVarInt()
    val maxLen = 32767
    if (j > maxLen * 4)
      throw RuntimeException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + maxLen * 4 + ")")
    else if (j < 0)
      throw RuntimeException("The received encoded string buffer length is less than zero! Weird string!")
    else {
      val s = String(this.readBytes(j), StandardCharsets.UTF_8)
      if (s.length > maxLen)
        throw RuntimeException("The received string length is longer than maximum allowed ($j > $maxLen)")
      else
        return s
    }
  }

  /**
   * Reads an optional value from the buffer
   * @param reader reader if present
   * @return the value read, or null if not present
   */
  inline fun <T: Any> readOptional(crossinline reader: (WrappedByteBuf) -> T): Optional<T>
  {
    val isPresent = this.readBoolean()
    if (!isPresent)
      return Optional.empty<T>()
    return Optional.of(reader(this))
  }

  /**
   * Consumes the buffer and returns the bytes written to it
   * @return the bytes written to the buffer
   */
  fun consume(): ByteArray
  {
    val bytes = ByteArray(this.buffer.position())
    this.buffer.rewind()
    this.buffer.get(bytes)
    return bytes
  }
}

/**
 * The direction of a payload, either serverbound or clientbound
 */
enum class PayloadDirection {
  Serverbound,
  Clientbound
}

/**
 * The state of a payload, either configuration or play
 */
enum class PayloadState {
  Configuration,
  Play
}

/**
 * A deserializer for payloads
 * @param T the type of payload to deserialize
 */
interface Deserializer<T: CustomPayload>
{
  fun read(buf: WrappedByteBuf): T
}

/**
 * An identifier for a payload channel, consisting of a namespace, path and a channel string representation
 */
data class Identifier(val namespace: String, val path: String, private val channel: String)
{
  companion object
  {
    /**
     * Creates an identifier with the given namespace and path
     * @param namespace the namespace of the identifier
     * @param path the path of the identifier
     * @return the created identifier, where the channel string representation is in the format "namespace:path"
     */
    @JvmStatic
    fun of(namespace: String, path: String): Identifier = Identifier(namespace, path, "$namespace:$path")

    /**
     * Creates an identifier with the given namespace, path, and channel
     * @param namespace the namespace of the identifier
     * @param path the path of the identifier
     * @param channel the channel string representation of this identifier
     * @return the created identifier
     */
    @JvmStatic
    fun ofChannel(namespace: String, path: String, channel: String): Identifier = Identifier(namespace, path, channel)
  }

  /**
   * Gets the channel string representation of this identifier
   * @return the channel string
   */
  fun channel(): String = this.channel
}

/**
 * Metadata about a payload type
 */
class CustomPayloadType(
  val clazz: Class<out CustomPayload>,
  val identifier: Identifier,
  val direction: PayloadDirection,
  val stateType: PayloadState,
  val deserializer: Deserializer<*>
)
{

  companion object
  {

    /**
     * Creates a serverbound payload type with the given parameters and a state type of config
     * @param clazz the class of the payload
     * @param identifier the unique identifier of the payload
     * @param deserializer the deserializer for the payload
     */
    @JvmStatic
    fun serverboundConfig(
      clazz: Class<out ServerboundCustomPayload>,
      identifier: Identifier,
      deserializer: Deserializer<out ServerboundCustomPayload>
    ): CustomPayloadType = CustomPayloadType(clazz, identifier, PayloadDirection.Serverbound, PayloadState.Configuration, deserializer)

    /**
     * Creates a clientbound payload type with the given parameters and a state type of config
     * @param clazz the class of the payload
     * @param identifier the unique identifier of the payload
     * @param deserializer the deserializer for the payload
     */
    @JvmStatic
    fun clientboundConfig(
      clazz: Class<out ClientboundCustomPayload>,
      identifier: Identifier,
      deserializer: Deserializer<out ClientboundCustomPayload>
    ): CustomPayloadType = CustomPayloadType(clazz, identifier, PayloadDirection.Clientbound, PayloadState.Configuration, deserializer)

    /**
     * Creates a serverbound payload type with the given parameters and a state type of play
     * @param clazz the class of the payload
     * @param identifier the unique identifier of the payload
     * @param deserializer the deserializer for the payload
     */
    @JvmStatic
    fun serverboundPlay(
      clazz: Class<out ServerboundCustomPayload>,
      identifier: Identifier,
      deserializer: Deserializer<out ServerboundCustomPayload>
    ): CustomPayloadType = CustomPayloadType(clazz, identifier, PayloadDirection.Serverbound, PayloadState.Play, deserializer)

    /**
     * Creates a clientbound payload type with the given parameters and a state type of play
     * @param clazz the class of the payload
     * @param identifier the unique identifier of the payload
     * @param deserializer the deserializer for the payload
     */
    @JvmStatic
    fun clientboundPlay(
      clazz: Class<out ClientboundCustomPayload>,
      identifier: Identifier,
      deserializer: Deserializer<out ClientboundCustomPayload>
    ): CustomPayloadType = CustomPayloadType(clazz, identifier, PayloadDirection.Clientbound, PayloadState.Play, deserializer)
  }

}

/**
 * The base class for all payloads
 */
abstract class CustomPayload(protected val type: CustomPayloadType)
{
  /**
   * Gets the payload type of this payload
   * @return the payload type
   */
  fun type(): CustomPayloadType = this.type
}

/**
 * A clientbound payload
 */
abstract class ClientboundCustomPayload(type: CustomPayloadType) : CustomPayload(type)
{

  /** Writes this payload to the given buffer
   * @param buffer the buffer
   */
  abstract fun write(buffer: WrappedByteBuf)

  /**
   * Sends this payload to the user with the given UUID
   * @param userUUID the UUID of the user to send the payload to
   * @throws IllegalArgumentException if no user is found with the given UUID
   */
  fun send(userUUID: UUID)
  {
    val user = ensurePacketEvents().playerManager.getUser(userUUID) ?: throw IllegalArgumentException("no user found with UUID: $userUUID")
    this.send(user)
  }

  /**
   * Sends this payload to the given user
   * @param user the user to send the payload to
   */
  fun send(user: User)
  {
    ChannelHelper.runInEventLoop(user.channel) {
      val channel = this.type.identifier.channel()
      val payloadBuffer = WrappedByteBuf(ByteBuffer.allocate(256))
      this.write(payloadBuffer)
      val payloadBytes = payloadBuffer.consume()
      val packet = when (this.type.stateType)
      {
        PayloadState.Configuration -> WrapperConfigServerPluginMessage(channel, payloadBytes)
        PayloadState.Play -> WrapperPlayServerPluginMessage(channel, payloadBytes)
      }
      user.sendPacket(packet)
    }
  }
}

/**
 * A serverbound payload
 */
abstract class ServerboundCustomPayload(type: CustomPayloadType) : CustomPayload(type)

class PayloadStateTypeMap<V> : EnumMap<PayloadState, V>(PayloadState::class.java)

/**
 * A builder for creating a custom payload registry
 */
class CustomPayloadRegistryBuilder
{

  private val serverbound = HashMap<String, PayloadStateTypeMap<CustomPayloadType>>()
  private val clientbound = HashMap<String, PayloadStateTypeMap<CustomPayloadType>>()

  private val registeredIds = HashSet<String>()

  /**
   * Registers a payload type with the given parameters
   * @param type the payload type to register
   * @return the registered payload type
   * @throws IllegalArgumentException if a payload with the same identifier is already registered
   */
  fun register(type: CustomPayloadType): CustomPayloadRegistryBuilder
  {
    val channel = type.identifier.channel()
    if (!this.registeredIds.add(channel))
      throw IllegalArgumentException("A payload with channel '${channel}' is already registered")
    when (type.direction)
    {
      PayloadDirection.Serverbound -> this.serverbound.computeIfAbsent(channel) { PayloadStateTypeMap() }[type.stateType] = type
      PayloadDirection.Clientbound -> this.clientbound.computeIfAbsent(channel) { PayloadStateTypeMap() }[type.stateType] = type
    }
    return this
  }

  /**
   * Builds the payload registry with the registered payload types
   * @return the built payload registry
   */
  fun build(): CustomPayloadRegistry = CustomPayloadRegistry(this.serverbound, this.clientbound)
}

open class CustomPayloadRegistry(
  private val serverbound: HashMap<String, PayloadStateTypeMap<CustomPayloadType>>,
  private val clientbound: HashMap<String, PayloadStateTypeMap<CustomPayloadType>>
)
{

  companion object {
    /**
     * Creates a new payload registry builder
     * @return builder
     */
    @JvmStatic
    fun builder(): CustomPayloadRegistryBuilder = CustomPayloadRegistryBuilder()
  }

  /**
   * Gets the payload type for the given direction, state type, and channel
   * @param direction the direction of the payload (serverbound or clientbound)
   * @param stateType the state type of the payload (config or play)
   * @param channel the channel of the payload
   * @return the payload type if found, or null if not found
   */
  fun get(direction: PayloadDirection, stateType: PayloadState, channel: String): CustomPayloadType?
  {
    return when (direction)
    {
      PayloadDirection.Serverbound -> this.serverbound[channel]?.get(stateType)
      PayloadDirection.Clientbound -> this.clientbound[channel]?.get(stateType)
    }
  }
}

class PayloadEvent<P: CustomPayload>(
  private val payload: P,
  private val user: User,
  private val channel: String,
  private val bytes: ByteArray,
  private var cancelled: Boolean
)
{

  /**
   * Gets the payload channel
   * @return the channel
   */
  fun channel(): String = this.channel

  /**
   * Gets the raw payload bytes
   * @return the bytes
   */
  fun bytes(): ByteArray = this.bytes

  /**
   * Gets the PacketEvent's user associated with this event
   * @return the user of this event
   */
  fun user(): User = this.user

  /**
   * Gets the player's name associated with this event
   * @return the player's name
   */
  fun playerName(): String = this.user.name

  /**
   * Gets the player's UUID associated with this event
   * @return the player's UUID
   */
  fun playerUUID(): UUID = this.user.uuid

  /**
   * Gets the payload associated with this event
   * @return the payload of this event
   */
  fun payload(): P = this.payload

  /**
   * Casts the payload of this event to the specified type
   * @return the casted payload
   * @throws ClassCastException if the payload cannot be cast to the specified type
   */
  fun <Cast: P> payloadCasted(): Cast = this.payload as Cast

  /**
   * Gets the payload type of this event's payload
   * @return the payload type
   */
  fun payloadType(): CustomPayloadType = this.payload.type()

  /**
   * Checks if this event is cancelled
   * @return true if cancelled, false otherwise
   */
  fun isCancelled(): Boolean = this.cancelled

  /**
   * Sets the cancelled state of this event
   * @param cancelled cancelled state
   */
  fun setCancelled(cancelled: Boolean) { this.cancelled = cancelled }
}

/**
 * A packet listener that listens for custom payloads and handles them
 * @param registry the payload registry to use for looking up payload types
 * @param priority the priority of this packet listener
 */
abstract class CustomPayloadListener(private val registry: CustomPayloadRegistry, priority: PacketListenerPriority) : PacketListenerAbstract(priority) {

  private class PayloadMessage(val channel: String, val data: ByteArray, val state: PayloadState)

  constructor(registry: CustomPayloadRegistry) : this(registry, PacketListenerPriority.NORMAL)

  /**
   * Starts this packet listener listening for packets
   */
  fun startListening() { ensurePacketEvents().eventManager.registerListener(this) }

  /**
   * Stops this packet listener from listening for packets
   */
  fun stopListening() { ensurePacketEvents().eventManager.unregisterListener(this) }

  final override fun onPacketReceive(event: PacketReceiveEvent)
  {
    val packetType = event.packetType
    val payloadMessage = when (packetType)
    {
      PacketType.Play.Client.PLUGIN_MESSAGE -> {
        val wrapper = WrapperPlayClientPluginMessage(event)
        PayloadMessage(wrapper.channelName, wrapper.data, PayloadState.Play)
      }
      PacketType.Configuration.Client.PLUGIN_MESSAGE -> {
        val wrapper = WrapperConfigClientPluginMessage(event)
        PayloadMessage(wrapper.channelName, wrapper.data, PayloadState.Configuration)
      }
      else -> return
    }

    val payloadType = this.registry.get(PayloadDirection.Serverbound, payloadMessage.state, payloadMessage.channel) ?: return
    val wrappedBuf = WrappedByteBuf(ByteBuffer.wrap(payloadMessage.data))
    val payload = payloadType.deserializer.read(wrappedBuf)

    val event = PayloadEvent(
      payload as ServerboundCustomPayload,
      event.user,
      payloadMessage.channel,
      payloadMessage.data,
      event.isCancelled
    )
    this.onPayloadReceive(event)

    event.setCancelled(event.isCancelled())
  }

  final override fun onPacketSend(event: PacketSendEvent)
  {
    val packetType = event.packetType
    val payloadMessage = when (packetType)
    {
      PacketType.Play.Server.PLUGIN_MESSAGE -> {
        val wrapper = WrapperPlayServerPluginMessage(event)
        PayloadMessage(wrapper.channelName, wrapper.data, PayloadState.Play)
      }
      PacketType.Configuration.Server.PLUGIN_MESSAGE -> {
        val wrapper = WrapperConfigServerPluginMessage(event)
        PayloadMessage(wrapper.channelName, wrapper.data, PayloadState.Configuration)
      }
      else -> return
    }

    val payloadType = this.registry.get(PayloadDirection.Clientbound, payloadMessage.state, payloadMessage.channel) ?: return
    val wrappedBuf = WrappedByteBuf(ByteBuffer.wrap(payloadMessage.data))
    val payload = payloadType.deserializer.read(wrappedBuf)

    val event = PayloadEvent(
      payload as ClientboundCustomPayload,
      event.user,
      payloadMessage.channel,
      payloadMessage.data,
      event.isCancelled
    )
    this.onPayloadSend(event)

    event.setCancelled(event.isCancelled())
  }

  /**
   * Called when a payload is sent to a user
   * @param event the payload event
   */
  open fun onPayloadSend(event: PayloadEvent<ClientboundCustomPayload>) {}

  /**
   * Called when a payload is received from a user
   * @param event the payload event
   */
  open fun onPayloadReceive(event: PayloadEvent<ServerboundCustomPayload>) {}
}

private fun ensurePacketEvents(): PacketEventsAPI<*>
{
  val api = PacketEvents.getAPI() ?: throw IllegalStateException("PacketEvents API is not available: is PacketEvents installed?")
  if (!api.isInitialized)
    throw IllegalStateException("PacketEvents API is not initialized")
  return api
}