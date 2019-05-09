/*
 * Copyright 2015-2019 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dv8tion.jda.api.sharding;

import com.neovisionaries.ws.client.WebSocketFactory;
import net.dv8tion.jda.annotations.DeprecatedSince;
import net.dv8tion.jda.annotations.ReplaceWith;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.IEventManager;
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.SessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.config.sharding.*;
import okhttp3.OkHttpClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * Used to create new instances of JDA's default {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager} implementation.
 *
 * <p>A single DefaultShardManagerBuilder can be reused multiple times. Each call to {@link #build()}
 * creates a new {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager} instance using the same information.
 *
 * @since  3.4
 * @author Aljoscha Grebe
 */
public class  DefaultShardManagerBuilder
{
    protected final List<Object> listeners = new ArrayList<>();
    protected final List<IntFunction<Object>> listenerProviders = new ArrayList<>();
    protected SessionController sessionController = null;
    protected VoiceDispatchInterceptor voiceDispatchInterceptor = null;
    protected EnumSet<CacheFlag> cacheFlags = EnumSet.allOf(CacheFlag.class);
    protected boolean enableContext = true;
    protected boolean enableBulkDeleteSplitting = true;
    protected boolean enableShutdownHook = true;
    protected boolean enableVoice = true;
    protected boolean autoReconnect = true;
    protected boolean retryOnTimeout = true;
    protected boolean useShutdownNow = false;
    protected Compression compression = Compression.ZLIB;
    protected int shardsTotal = -1;
    protected int maxReconnectDelay = 900;
    protected String token = null;
    protected IntFunction<Boolean> idleProvider = null;
    protected IntFunction<OnlineStatus> statusProvider = null;
    protected IntFunction<? extends Activity> activityProvider = null;
    protected IntFunction<? extends ConcurrentMap<String, String>> contextProvider = null;
    protected IntFunction<? extends IEventManager> eventManagerProvider = null;
    protected ThreadPoolProvider<? extends ScheduledExecutorService> rateLimitPoolProvider = null;
    protected ThreadPoolProvider<? extends ScheduledExecutorService> gatewayPoolProvider = null;
    protected ThreadPoolProvider<? extends ExecutorService> callbackPoolProvider = null;
    protected Collection<Integer> shards = null;
    protected OkHttpClient.Builder httpClientBuilder = null;
    protected OkHttpClient httpClient = null;
    protected WebSocketFactory wsFactory = null;
    protected IAudioSendFactory audioSendFactory = null;
    protected ThreadFactory threadFactory = null;

    /**
     * Creates a completely empty DefaultShardManagerBuilder.
     * <br>You need to set the token using
     * {@link net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder#setToken(String) setToken(String)}
     * before calling {@link net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder#build() build()}.
     */
    public DefaultShardManagerBuilder() {}

    /**
     * Creates a DefaultShardManagerBuilder with the given token.
     * <br>This is equivalent to using the constuctor
     * {@link #DefaultShardManagerBuilder() DefaultShardManagerBuilder()}
     * and calling {@link #setToken(String) setToken(String)}
     * directly afterward. You can always change the token later with
     * {@link #setToken(String) setToken(String)}.
     *
     * @param token
     *        The login token
     */
    public DefaultShardManagerBuilder(@Nonnull String token)
    {
        this.setToken(token);
    }

    /**
     * Flags used to enable parts of the JDA cache to reduce the runtime memory footprint.
     * <br><b>It is highly recommended to use {@link #setDisabledCacheFlags(EnumSet)} instead
     * for backwards compatibility</b>. We might add more flags in the future which you then effectively disable
     * when updating and not changing your setting here.
     *
     * @param  flags
     *         EnumSet containing the flags for cache services that should be <b>enabled</b>
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setEnabledCacheFlags(@Nullable EnumSet<CacheFlag> flags)
    {
        this.cacheFlags = flags == null ? EnumSet.noneOf(CacheFlag.class) : EnumSet.copyOf(flags);
        return this;
    }

    /**
     * Flags used to disable parts of the JDA cache to reduce the runtime memory footprint.
     * <br>Shortcut for {@code setEnabledCacheFlags(EnumSet.complementOf(flags))}
     *
     * @param  flags
     *         EnumSet containing the flags for cache services that should be <b>disabled</b>
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setDisabledCacheFlags(@Nullable EnumSet<CacheFlag> flags)
    {
        return setEnabledCacheFlags(flags == null ? EnumSet.allOf(CacheFlag.class) : EnumSet.complementOf(flags));
    }

    /**
     * Sets the {@link net.dv8tion.jda.api.utils.SessionController SessionController}
     * for the resulting ShardManager instance. This can be used to sync behaviour and state between shards
     * of a bot and should be one and the same instance on all builders for the shards.
     *
     * @param  controller
     *         The {@link net.dv8tion.jda.api.utils.SessionController SessionController} to use
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    net.dv8tion.jda.api.utils.SessionControllerAdapter SessionControllerAdapter
     */
    @Nonnull
    public DefaultShardManagerBuilder setSessionController(@Nullable SessionController controller)
    {
        this.sessionController = controller;
        return this;
    }

    /**
     * Configures a custom voice dispatch handler which handles audio connections.
     *
     * @param  interceptor
     *         The new voice dispatch handler, or null to use the default
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    VoiceDispatchInterceptor
     */
    @Nonnull
    public DefaultShardManagerBuilder setVoiceDispatchInterceptor(@Nullable VoiceDispatchInterceptor interceptor)
    {
        this.voiceDispatchInterceptor = interceptor;
        return this;
    }

    /**
     * Sets the {@link org.slf4j.MDC MDC} mappings provider to use in JDA.
     * <br>If sharding is enabled JDA will automatically add a {@code jda.shard} context with the format {@code [SHARD_ID / TOTAL]}
     * where {@code SHARD_ID} and {@code TOTAL} are the shard configuration.
     * Additionally it will provide context for the id via {@code jda.shard.id} and the total via {@code jda.shard.total}.
     *
     * <p><b>The manager will call this with a shardId and it is recommended to provide a different context map for each shard!</b>
     * <br>This automatically switches {@link #setContextEnabled(boolean)} to true if the provided function is not null!
     *
     * @param  provider
     *         The provider for <b>modifiable</b> context maps to use in JDA, or {@code null} to reset
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    <a href="https://www.slf4j.org/api/org/slf4j/MDC.html" target="_blank">MDC Javadoc</a>
     */
    @Nonnull
    public DefaultShardManagerBuilder setContextMap(@Nullable IntFunction<? extends ConcurrentMap<String, String>> provider)
    {
        this.contextProvider = provider;
        if (provider != null)
            this.enableContext = true;
        return this;
    }

    /**
     * Whether JDA should use a synchronized MDC context for all of its controlled threads.
     * <br>Default: {@code true}
     *
     * @param  enable
     *         True, if JDA should provide an MDC context map
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    <a href="https://www.slf4j.org/api/org/slf4j/MDC.html" target="_blank">MDC Javadoc</a>
     * @see    #setContextMap(java.util.function.IntFunction)
     */
    @Nonnull
    public DefaultShardManagerBuilder setContextEnabled(boolean enable)
    {
        this.enableContext = enable;
        return this;
    }

    /**
     * Sets the compression algorithm used with the gateway connection,
     * this will decrease the amount of used bandwidth for the running bot instance
     * for the cost of a few extra cycles for decompression.
     * Compression can be entirely disabled by setting this to {@link net.dv8tion.jda.api.utils.Compression#NONE}.
     * <br><b>Default: {@link net.dv8tion.jda.api.utils.Compression#ZLIB}</b>
     *
     * <p><b>We recommend to keep this on the default unless you have issues with the decompression</b>
     * <br>This mode might become obligatory in a future version, do not rely on this switch to stay.
     *
     * @param  compression
     *         The compression algorithm to use for the gateway connection
     *
     * @throws java.lang.IllegalArgumentException
     *         If provided with null
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    <a href="https://discordapp.com/developers/docs/topics/gateway#transport-compression" target="_blank">Official Discord Documentation - Transport Compression</a>
     */
    @Nonnull
    public DefaultShardManagerBuilder setCompression(@Nonnull Compression compression)
    {
        Checks.notNull(compression, "Compression");
        this.compression = compression;
        return this;
    }

    /**
     * Adds all provided listeners to the list of listeners that will be used to populate the {@link DefaultShardManager DefaultShardManager} object.
     * <br>This uses the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager InterfacedEventListener} by default.
     * <br>To switch to the {@link net.dv8tion.jda.api.hooks.AnnotatedEventManager AnnotatedEventManager},
     * use {@link #setEventManagerProvider(IntFunction) setEventManagerProvider(id -> new AnnotatedEventManager())}.
     *
     * <p><b>Note:</b> When using the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager InterfacedEventListener} (default),
     * given listener(s) <b>must</b> be instance of {@link net.dv8tion.jda.api.hooks.EventListener EventListener}!
     *
     * @param  listeners
     *         The listener(s) to add to the list.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    DefaultShardManager#addEventListener(Object...) JDA.addEventListeners(Object...)
     */
    @Nonnull
    public DefaultShardManagerBuilder addEventListeners(@Nonnull final Object... listeners)
    {
        return this.addEventListeners(Arrays.asList(listeners));
    }

    /**
     * Adds all provided listeners to the list of listeners that will be used to populate the {@link DefaultShardManager DefaultShardManager} object.
     * <br>This uses the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager InterfacedEventListener} by default.
     * <br>To switch to the {@link net.dv8tion.jda.api.hooks.AnnotatedEventManager AnnotatedEventManager},
     * use {@link #setEventManagerProvider(IntFunction) setEventManager(id -> new AnnotatedEventManager())}.
     *
     * <p><b>Note:</b> When using the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager InterfacedEventListener} (default),
     * given listener(s) <b>must</b> be instance of {@link net.dv8tion.jda.api.hooks.EventListener EventListener}!
     *
     * @param  listeners
     *         The listener(s) to add to the list.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    DefaultShardManager#addEventListener(Object...) JDA.addEventListeners(Object...)
     */
    @Nonnull
    public DefaultShardManagerBuilder addEventListeners(@Nonnull final Collection<Object> listeners)
    {
        Checks.noneNull(listeners, "listeners");

        this.listeners.addAll(listeners);
        return this;
    }

    /**
     * Removes all provided listeners from the list of listeners.
     *
     * @param  listeners
     *         The listener(s) to remove from the list.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    net.dv8tion.jda.api.JDA#removeEventListener(Object...) JDA.removeEventListeners(Object...)
     */
    @Nonnull
    public DefaultShardManagerBuilder removeEventListeners(@Nonnull final Object... listeners)
    {
        return this.removeEventListeners(Arrays.asList(listeners));
    }

    /**
     * Removes all provided listeners from the list of listeners.
     *
     * @param  listeners
     *         The listener(s) to remove from the list.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    net.dv8tion.jda.api.JDA#removeEventListener(Object...) JDA.removeEventListeners(Object...)
     */
    @Nonnull
    public DefaultShardManagerBuilder removeEventListeners(@Nonnull final Collection<Object> listeners)
    {
        Checks.noneNull(listeners, "listeners");

        this.listeners.removeAll(listeners);
        return this;
    }

    /**
     * Adds the provided listener provider to the list of listener providers that will be used to create listeners.
     * On shard creation (including shard restarts) the provider will have the shard id applied and must return a listener,
     * which will be used, along all other listeners, to populate the listeners of the JDA object of that shard.
     *
     * <br>This uses the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager InterfacedEventListener} by default.
     * <br>To switch to the {@link net.dv8tion.jda.api.hooks.AnnotatedEventManager AnnotatedEventManager},
     * use {@link #setEventManagerProvider(IntFunction) setEventManager(id -> new AnnotatedEventManager())}.
     *
     * <p><b>Note:</b> When using the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager InterfacedEventListener} (default),
     * given listener(s) <b>must</b> be instance of {@link net.dv8tion.jda.api.hooks.EventListener EventListener}!
     *
     * @param  listenerProvider
     *         The listener provider to add to the list of listener providers.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder addEventListenerProvider(@Nonnull final IntFunction<Object> listenerProvider)
    {
        return this.addEventListenerProviders(Collections.singleton(listenerProvider));
    }

    /**
     * Adds the provided listener providers to the list of listener providers that will be used to create listeners.
     * On shard creation (including shard restarts) each provider will have the shard id applied and must return a listener,
     * which will be used, along all other listeners, to populate the listeners of the JDA object of that shard.
     *
     * <br>This uses the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager InterfacedEventListener} by default.
     * <br>To switch to the {@link net.dv8tion.jda.api.hooks.AnnotatedEventManager AnnotatedEventManager},
     * use {@link #setEventManagerProvider(IntFunction) setEventManager(id -> new AnnotatedEventManager())}.
     *
     * <p><b>Note:</b> When using the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager InterfacedEventListener} (default),
     * given listener(s) <b>must</b> be instance of {@link net.dv8tion.jda.api.hooks.EventListener EventListener}!
     *
     * @param  listenerProviders
     *         The listener provider to add to the list of listener providers.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder addEventListenerProviders(@Nonnull final Collection<IntFunction<Object>> listenerProviders)
    {
        Checks.noneNull(listenerProviders, "listener providers");

        this.listenerProviders.addAll(listenerProviders);
        return this;
    }

    /**
     * Removes the provided listener provider from the list of listener providers.
     *
     * @param  listenerProvider
     *         The listener provider to remove from the list of listener providers.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder removeEventListenerProvider(@Nonnull final IntFunction<Object> listenerProvider)
    {
        return this.removeEventListenerProviders(Collections.singleton(listenerProvider));
    }

    /**
     * Removes all provided listener providers from the list of listener providers.
     *
     * @param  listenerProviders
     *         The listener provider(s) to remove from the list of listener providers.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder removeEventListenerProviders(@Nonnull final Collection<IntFunction<Object>> listenerProviders)
    {
        Checks.noneNull(listenerProviders, "listener providers");

        this.listenerProviders.removeAll(listenerProviders);
        return this;
    }

    /**
     * Enables/Disables Voice functionality.
     * <br>This is useful, if your current system doesn't support Voice and you do not need it.
     *
     * <p>Default: <b>true (enabled)</b>
     *
     * @param  enabled
     *         True - enables voice support.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setAudioEnabled(final boolean enabled)
    {
        this.enableVoice = enabled;
        return this;
    }

    /**
     * Changes the factory used to create {@link net.dv8tion.jda.api.audio.factory.IAudioSendSystem IAudioSendSystem}
     * objects which handle the sending loop for audio packets.
     * <br>By default, JDA uses {@link net.dv8tion.jda.api.audio.factory.DefaultSendFactory DefaultSendFactory}.
     *
     * @param  factory
     *         The new {@link net.dv8tion.jda.api.audio.factory.IAudioSendFactory IAudioSendFactory} to be used
     *         when creating new {@link net.dv8tion.jda.api.audio.factory.IAudioSendSystem} objects.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setAudioSendFactory(@Nullable final IAudioSendFactory factory)
    {
        this.audioSendFactory = factory;
        return this;
    }

    /**
     * Sets whether or not JDA should try to reconnect if a connection-error is encountered.
     * <br>This will use an incremental reconnect (timeouts are increased each time an attempt fails).
     *
     * Default: <b>true (enabled)</b>
     *
     * @param  autoReconnect
     *         If true - enables autoReconnect
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setAutoReconnect(final boolean autoReconnect)
    {
        this.autoReconnect = autoReconnect;
        return this;
    }

    /**
     * If enabled, JDA will separate the bulk delete event into individual delete events, but this isn't as efficient as
     * handling a single event would be. It is recommended that BulkDelete Splitting be disabled and that the developer
     * should instead handle the {@link net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent MessageBulkDeleteEvent}.
     *
     * <p>Default: <b>true (enabled)</b>
     *
     * @param  enabled
     *         True - The MESSAGE_DELETE_BULK will be split into multiple individual MessageDeleteEvents.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setBulkDeleteSplittingEnabled(final boolean enabled)
    {
        this.enableBulkDeleteSplitting = enabled;
        return this;
    }

    /**
     * Enables/Disables the use of a Shutdown hook to clean up the ShardManager and it's JDA instances.
     * <br>When the Java program closes shutdown hooks are run. This is used as a last-second cleanup
     * attempt by JDA to properly close connections.
     *
     * <p>Default: <b>true (enabled)</b>
     *
     * @param  enable
     *         True (default) - use shutdown hook to clean up the ShardManager and it's JDA instances if the Java program is closed.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setEnableShutdownHook(final boolean enable)
    {
        this.enableShutdownHook = enable;
        return this;
    }

    /**
     * Changes the internally used EventManager.
     * <br>There are 2 provided Implementations:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.hooks.InterfacedEventManager InterfacedEventManager} which uses the Interface
     *     {@link net.dv8tion.jda.api.hooks.EventListener EventListener} (tip: use the {@link net.dv8tion.jda.api.hooks.ListenerAdapter ListenerAdapter}).
     *     <br>This is the default EventManager.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.hooks.AnnotatedEventManager AnnotatedEventManager} which uses the Annotation
     *         {@link net.dv8tion.jda.api.hooks.SubscribeEvent @SubscribeEvent} to mark the methods that listen for events.</li>
     * </ul>
     * <br>You can also create your own EventManager (See {@link net.dv8tion.jda.api.hooks.IEventManager}).
     *
     * @param  manager
     *         The new {@link net.dv8tion.jda.api.hooks.IEventManager} to use.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @deprecated Use {@link #setEventManagerProvider(IntFunction)} instead
     */
    @Nonnull
    @Deprecated
    @DeprecatedSince("3.8.1")
    @ReplaceWith("setEventManagerProvider((id) -> manager)")
    public DefaultShardManagerBuilder setEventManager(@Nonnull final IEventManager manager)
    {
        Checks.notNull(manager, "manager");

        return setEventManagerProvider((id) -> manager);
    }

    /**
     * Sets a provider to change the internally used EventManager.
     * <br>There are 2 provided Implementations:
     * <ul>
     *     <li>{@link net.dv8tion.jda.api.hooks.InterfacedEventManager InterfacedEventManager} which uses the Interface
     *     {@link net.dv8tion.jda.api.hooks.EventListener EventListener} (tip: use the {@link net.dv8tion.jda.api.hooks.ListenerAdapter ListenerAdapter}).
     *     <br>This is the default EventManager.</li>
     *
     *     <li>{@link net.dv8tion.jda.api.hooks.AnnotatedEventManager AnnotatedEventManager} which uses the Annotation
     *         {@link net.dv8tion.jda.api.hooks.SubscribeEvent @SubscribeEvent} to mark the methods that listen for events.</li>
     * </ul>
     * <br>You can also create your own EventManager (See {@link net.dv8tion.jda.api.hooks.IEventManager}).
     *
     * @param  eventManagerProvider
     *         A supplier for the new {@link net.dv8tion.jda.api.hooks.IEventManager} to use.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setEventManagerProvider(@Nonnull final IntFunction<? extends IEventManager> eventManagerProvider)
    {
        Checks.notNull(eventManagerProvider, "eventManagerProvider");
        this.eventManagerProvider = eventManagerProvider;
        return this;
    }

    /**
     * Sets the {@link net.dv8tion.jda.api.entities.Activity Activity} for our session.
     * <br>This value can be changed at any time in the {@link net.dv8tion.jda.api.managers.Presence Presence} from a JDA instance.
     *
     * <p><b>Hint:</b> You can create a {@link net.dv8tion.jda.api.entities.Activity Activity} object using
     * {@link net.dv8tion.jda.api.entities.Activity#playing(String) Activity.playing(String)} or
     * {@link net.dv8tion.jda.api.entities.Activity#streaming(String, String)} Activity.streaming(String, String)}.
     *
     * @param  game
     *         An instance of {@link net.dv8tion.jda.api.entities.Activity Activity} (null allowed)
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    net.dv8tion.jda.api.managers.Presence#setActivity(net.dv8tion.jda.api.entities.Activity)
     */
    @Nonnull
    public DefaultShardManagerBuilder setActivity(@Nullable final Activity game)
    {
        return this.setActivityProvider(id -> game);
    }

    /**
     * Sets the {@link net.dv8tion.jda.api.entities.Activity Activity} for our session.
     * <br>This value can be changed at any time in the {@link net.dv8tion.jda.api.managers.Presence Presence} from a JDA instance.
     *
     * <p><b>Hint:</b> You can create a {@link net.dv8tion.jda.api.entities.Activity Activity} object using
     * {@link net.dv8tion.jda.api.entities.Activity#playing(String) Activity.playing(String)} or
     * {@link net.dv8tion.jda.api.entities.Activity#streaming(String, String) Activity.streaming(String, String)}.
     *
     * @param  gameProvider
     *         An instance of {@link net.dv8tion.jda.api.entities.Activity Activity} (null allowed)
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    net.dv8tion.jda.api.managers.Presence#setActivity(net.dv8tion.jda.api.entities.Activity)
     */
    @Nonnull
    public DefaultShardManagerBuilder setActivityProvider(@Nullable final IntFunction<? extends Activity> gameProvider)
    {
        this.activityProvider = gameProvider;
        return this;
    }

    /**
     * Sets whether or not we should mark our sessions as afk
     * <br>This value can be changed at any time using
     * {@link DefaultShardManager#setIdle(boolean) DefaultShardManager#setIdleProvider(boolean)}.
     *
     * @param  idle
     *         boolean value that will be provided with our IDENTIFY packages to mark our sessions as afk or not. <b>(default false)</b>
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    net.dv8tion.jda.api.managers.Presence#setIdle(boolean)
     */
    @Nonnull
    public DefaultShardManagerBuilder setIdle(final boolean idle)
    {
        return this.setIdleProvider(id -> idle);
    }

    /**
     * Sets whether or not we should mark our sessions as afk
     * <br>This value can be changed at any time using
     * {@link DefaultShardManager#setIdle(boolean) DefaultShardManager#setIdleProvider(boolean)}.
     *
     * @param  idleProvider
     *         boolean value that will be provided with our IDENTIFY packages to mark our sessions as afk or not. <b>(default false)</b>
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    net.dv8tion.jda.api.managers.Presence#setIdle(boolean)
     */
    @Nonnull
    public DefaultShardManagerBuilder setIdleProvider(@Nullable final IntFunction<Boolean> idleProvider)
    {
        this.idleProvider = idleProvider;
        return this;
    }

    /**
     * Sets the {@link net.dv8tion.jda.api.OnlineStatus OnlineStatus} our connection will display.
     * <br>This value can be changed at any time in the {@link net.dv8tion.jda.api.managers.Presence Presence} from a JDA instance.
     *
     * <p><b>Note:</b>This will not take affect for {@link net.dv8tion.jda.api.AccountType#CLIENT AccountType.CLIENT}
     * if the statusProvider specified in the user_settings is not "online" as it is overriding our identify statusProvider.
     *
     * @param  status
     *         Not-null OnlineStatus (default online)
     *
     * @throws IllegalArgumentException
     *         if the provided OnlineStatus is null or {@link net.dv8tion.jda.api.OnlineStatus#UNKNOWN UNKNOWN}
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    net.dv8tion.jda.api.managers.Presence#setStatus(OnlineStatus) Presence.setStatusProvider(OnlineStatus)
     */
    @Nonnull
    public DefaultShardManagerBuilder setStatus(@Nullable final OnlineStatus status)
    {
        Checks.notNull(status, "status");
        Checks.check(status != OnlineStatus.UNKNOWN, "OnlineStatus cannot be unknown!");

        return this.setStatusProvider(id -> status);
    }

    /**
     * Sets the {@link net.dv8tion.jda.api.OnlineStatus OnlineStatus} our connection will display.
     * <br>This value can be changed at any time in the {@link net.dv8tion.jda.api.managers.Presence Presence} from a JDA instance.
     *
     * <p><b>Note:</b>This will not take affect for {@link net.dv8tion.jda.api.AccountType#CLIENT AccountType.CLIENT}
     * if the statusProvider specified in the user_settings is not "online" as it is overriding our identify statusProvider.
     *
     * @param  statusProvider
     *         Not-null OnlineStatus (default online)
     *
     * @throws IllegalArgumentException
     *         if the provided OnlineStatus is null or {@link net.dv8tion.jda.api.OnlineStatus#UNKNOWN UNKNOWN}
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    net.dv8tion.jda.api.managers.Presence#setStatus(OnlineStatus) Presence.setStatusProvider(OnlineStatus)
     */
    @Nonnull
    public DefaultShardManagerBuilder setStatusProvider(@Nullable final IntFunction<OnlineStatus> statusProvider)
    {
        this.statusProvider = statusProvider;
        return this;
    }

    /**
     * Sets the {@link java.util.concurrent.ThreadFactory ThreadFactory} that will be used by the internal executor
     * of the ShardManager.
     * <p>Note: This will not affect Threads created by any JDA instance.
     *
     * @param  threadFactory
     *         The ThreadFactory or {@code null} to reset to the default value.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setThreadFactory(@Nullable final ThreadFactory threadFactory)
    {
        this.threadFactory = threadFactory;
        return this;
    }

    /**
     * Sets the {@link okhttp3.OkHttpClient.Builder Builder} that will be used by JDA's requester.
     * This can be used to set things such as connection timeout and proxy.
     *
     * @param  builder
     *         The new {@link okhttp3.OkHttpClient.Builder OkHttpClient.Builder} to use.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setHttpClientBuilder(@Nullable OkHttpClient.Builder builder)
    {
        this.httpClientBuilder = builder;
        return this;
    }

    /**
     * Sets the {@link okhttp3.OkHttpClient OkHttpClient} that will be used by JDAs requester.
     * <br>This can be used to set things such as connection timeout and proxy.
     *
     * @param  client
     *         The new {@link okhttp3.OkHttpClient OkHttpClient} to use
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setHttpClient(@Nullable OkHttpClient client)
    {
        this.httpClient = client;
        return this;
    }

    /**
     * Sets the {@link ScheduledExecutorService ScheduledExecutorService} that should be used in
     * the JDA rate-limit handler. Changing this can drastically change the JDA behavior for RestAction execution
     * and should be handled carefully. <b>Only change this pool if you know what you're doing.</b>
     * <br>This will override the rate-limit pool provider set from {@link #setRateLimitPoolProvider(ThreadPoolProvider)}.
     * <br><b>This automatically disables the automatic shutdown of the rate-limit pool, you can enable
     * it using {@link #setRateLimitPool(ScheduledExecutorService, boolean) setRateLimiPool(executor, true)}</b>
     *
     * <p>This is used mostly by the Rate-Limiter to handle backoff delays by using scheduled executions.
     * Besides that it is also used by planned execution for {@link net.dv8tion.jda.api.requests.RestAction#queueAfter(long, TimeUnit)}
     * and similar methods.
     *
     * <p>Default: {@link ScheduledThreadPoolExecutor} with 5 threads (per shard).
     *
     * @param  pool
     *         The thread-pool to use for rate-limit handling
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setRateLimitPool(@Nullable ScheduledExecutorService pool)
    {
        return setRateLimitPool(pool, pool == null);
    }

    /**
     * Sets the {@link ScheduledExecutorService ScheduledExecutorService} that should be used in
     * the JDA rate-limit handler. Changing this can drastically change the JDA behavior for RestAction execution
     * and should be handled carefully. <b>Only change this pool if you know what you're doing.</b>
     * <br>This will override the rate-limit pool provider set from {@link #setRateLimitPoolProvider(ThreadPoolProvider)}.
     *
     * <p>This is used mostly by the Rate-Limiter to handle backoff delays by using scheduled executions.
     * Besides that it is also used by planned execution for {@link net.dv8tion.jda.api.requests.RestAction#queueAfter(long, TimeUnit)}
     * and similar methods.
     *
     * <p>Default: {@link ScheduledThreadPoolExecutor} with 5 threads (per shard).
     *
     * @param  pool
     *         The thread-pool to use for rate-limit handling
     * @param  automaticShutdown
     *         Whether {@link net.dv8tion.jda.api.JDA#shutdown()} should automatically shutdown this pool
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setRateLimitPool(@Nullable ScheduledExecutorService pool, boolean automaticShutdown)
    {
        return setRateLimitPoolProvider(pool == null ? null : new ThreadPoolProviderImpl<>(pool, automaticShutdown));
    }

    /**
     * Sets the {@link ScheduledExecutorService ScheduledExecutorService} provider that should be used in
     * the JDA rate-limit handler. Changing this can drastically change the JDA behavior for RestAction execution
     * and should be handled carefully. <b>Only change this pool if you know what you're doing.</b>
     *
     * <p>This is used mostly by the Rate-Limiter to handle backoff delays by using scheduled executions.
     * Besides that it is also used by planned execution for {@link net.dv8tion.jda.api.requests.RestAction#queueAfter(long, TimeUnit)}
     * and similar methods.
     *
     * <p>Default: {@link ScheduledThreadPoolExecutor} with 5 threads (per shard).
     *
     * @param  provider
     *         The thread-pool provider to use for rate-limit handling
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setRateLimitPoolProvider(@Nullable ThreadPoolProvider<? extends ScheduledExecutorService> provider)
    {
        this.rateLimitPoolProvider = provider;
        return this;
    }

    /**
     * Sets the {@link ScheduledExecutorService ScheduledExecutorService} that should be used for
     * the JDA main WebSocket workers.
     * <br><b>Only change this pool if you know what you're doing.</b>
     * <br>This will override the worker pool provider set from {@link #setGatewayPoolProvider(ThreadPoolProvider)}.
     * <br><b>This automatically disables the automatic shutdown of the main-ws pools, you can enable
     * it using {@link #setGatewayPool(ScheduledExecutorService, boolean) setGatewayPoolProvider(pool, true)}</b>
     *
     * <p>This is used to send various forms of session updates such as:
     * <ul>
     *     <li>Voice States - (Dis-)Connecting from channels</li>
     *     <li>Presence - Changing current activity or online status</li>
     *     <li>Guild Setup - Requesting Members of newly joined guilds</li>
     *     <li>Heartbeats - Regular updates to keep the connection alive (usually once a minute)</li>
     * </ul>
     * When nothing has to be sent the pool will only be used every 500 milliseconds to check the queue for new payloads.
     * Once a new payload is sent we switch to "rapid mode" which means more tasks will be submitted until no more payloads
     * have to be sent.
     *
     * <p>Default: {@link ScheduledThreadPoolExecutor} with 1 thread (per shard)
     *
     * @param  pool
     *         The thread-pool to use for main WebSocket workers
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setGatewayPool(@Nullable ScheduledExecutorService pool)
    {
        return setGatewayPool(pool, pool == null);
    }

    /**
     * Sets the {@link ScheduledExecutorService ScheduledExecutorService} that should be used for
     * the JDA main WebSocket workers.
     * <br><b>Only change this pool if you know what you're doing.</b>
     * <br>This will override the worker pool provider set from {@link #setGatewayPoolProvider(ThreadPoolProvider)}.
     *
     * <p>This is used to send various forms of session updates such as:
     * <ul>
     *     <li>Voice States - (Dis-)Connecting from channels</li>
     *     <li>Presence - Changing current activity or online status</li>
     *     <li>Guild Setup - Requesting Members of newly joined guilds</li>
     *     <li>Heartbeats - Regular updates to keep the connection alive (usually once a minute)</li>
     * </ul>
     * When nothing has to be sent the pool will only be used every 500 milliseconds to check the queue for new payloads.
     * Once a new payload is sent we switch to "rapid mode" which means more tasks will be submitted until no more payloads
     * have to be sent.
     *
     * <p>Default: {@link ScheduledThreadPoolExecutor} with 1 thread (per shard)
     *
     * @param  pool
     *         The thread-pool to use for main WebSocket workers
     * @param  automaticShutdown
     *         Whether {@link net.dv8tion.jda.api.JDA#shutdown()} should automatically shutdown this pool
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setGatewayPool(@Nullable ScheduledExecutorService pool, boolean automaticShutdown)
    {
        return setGatewayPoolProvider(pool == null ? null : new ThreadPoolProviderImpl<>(pool, automaticShutdown));
    }

    /**
     * Sets the {@link ScheduledExecutorService ScheduledExecutorService} that should be used for
     * the JDA main WebSocket workers.
     * <br><b>Only change this pool if you know what you're doing.</b>
     *
     * <p>This is used to send various forms of session updates such as:
     * <ul>
     *     <li>Voice States - (Dis-)Connecting from channels</li>
     *     <li>Presence - Changing current activity or online status</li>
     *     <li>Guild Setup - Requesting Members of newly joined guilds</li>
     *     <li>Heartbeats - Regular updates to keep the connection alive (usually once a minute)</li>
     * </ul>
     * When nothing has to be sent the pool will only be used every 500 milliseconds to check the queue for new payloads.
     * Once a new payload is sent we switch to "rapid mode" which means more tasks will be submitted until no more payloads
     * have to be sent.
     *
     * <p>Default: {@link ScheduledThreadPoolExecutor} with 1 thread (per shard)
     *
     * @param  provider
     *         The thread-pool provider to use for main WebSocket workers
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setGatewayPoolProvider(@Nullable ThreadPoolProvider<? extends ScheduledExecutorService> provider)
    {
        this.gatewayPoolProvider = provider;
        return this;
    }

    /**
     * Sets the {@link ExecutorService ExecutorService} that should be used in
     * the JDA callback handler which mostly consists of {@link net.dv8tion.jda.api.requests.RestAction RestAction} callbacks.
     * By default JDA will use {@link ForkJoinPool#commonPool()}
     * <br><b>Only change this pool if you know what you're doing.
     * <br>This automatically disables the automatic shutdown of the callback pools, you can enable
     * it using {@link #setCallbackPool(ExecutorService, boolean) setCallbackPool(executor, true)}</b>
     *
     * <p>This is used to handle callbacks of {@link RestAction#queue()}, similarly it is used to
     * finish {@link RestAction#submit()} and {@link RestAction#complete()} tasks which build on queue.
     *
     * <p>Default: {@link ForkJoinPool#commonPool()}
     *
     * @param  executor
     *         The thread-pool to use for callback handling
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setCallbackPool(@Nullable ExecutorService executor)
    {
        return setCallbackPool(executor, executor == null);
    }

    /**
     * Sets the {@link ExecutorService ExecutorService} that should be used in
     * the JDA callback handler which mostly consists of {@link net.dv8tion.jda.api.requests.RestAction RestAction} callbacks.
     * By default JDA will use {@link ForkJoinPool#commonPool()}
     * <br><b>Only change this pool if you know what you're doing.</b>
     *
     * <p>This is used to handle callbacks of {@link RestAction#queue()}, similarly it is used to
     * finish {@link RestAction#submit()} and {@link RestAction#complete()} tasks which build on queue.
     *
     * <p>Default: {@link ForkJoinPool#commonPool()}
     *
     * @param  executor
     *         The thread-pool to use for callback handling
     * @param  automaticShutdown
     *         Whether {@link net.dv8tion.jda.api.JDA#shutdown()} should automatically shutdown this pool
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setCallbackPool(@Nullable ExecutorService executor, boolean automaticShutdown)
    {
        return setCallbackPoolProvider(executor == null ? null : new ThreadPoolProviderImpl<>(executor, automaticShutdown));
    }

    /**
     * Sets the {@link ExecutorService ExecutorService} that should be used in
     * the JDA callback handler which mostly consists of {@link net.dv8tion.jda.api.requests.RestAction RestAction} callbacks.
     * By default JDA will use {@link ForkJoinPool#commonPool()}
     * <br><b>Only change this pool if you know what you're doing.</b>
     *
     * <p>This is used to handle callbacks of {@link RestAction#queue()}, similarly it is used to
     * finish {@link RestAction#submit()} and {@link RestAction#complete()} tasks which build on queue.
     *
     * <p>Default: {@link ForkJoinPool#commonPool()}
     *
     * @param  provider
     *         The thread-pool provider to use for callback handling
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setCallbackPoolProvider(@Nullable ThreadPoolProvider<? extends ExecutorService> provider)
    {
        this.callbackPoolProvider = provider;
        return this;
    }

    /**
     * Sets the maximum amount of time that JDA will back off to wait when attempting to reconnect the MainWebsocket.
     * <br>Provided value must be 32 or greater.
     *
     * @param  maxReconnectDelay
     *         The maximum amount of time that JDA will wait between reconnect attempts in seconds.
     *
     * @throws java.lang.IllegalArgumentException
     *         Thrown if the provided {@code maxReconnectDelay} is less than 32.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setMaxReconnectDelay(final int maxReconnectDelay)
    {
        Checks.check(maxReconnectDelay >= 32, "Max reconnect delay must be 32 seconds or greater. You provided %d.", maxReconnectDelay);

        this.maxReconnectDelay = maxReconnectDelay;
        return this;
    }

    /**
     * Whether the Requester should retry when
     * a {@link java.net.SocketTimeoutException SocketTimeoutException} occurs.
     * <br><b>Default</b>: {@code true}
     *
     * <p>This value can be changed at any time with {@link net.dv8tion.jda.api.JDA#setRequestTimeoutRetry(boolean) JDA.setRequestTimeoutRetry(boolean)}!
     *
     * @param  retryOnTimeout
     *         True, if the Request should retry once on a socket timeout
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setRequestTimeoutRetry(boolean retryOnTimeout)
    {
        this.retryOnTimeout = retryOnTimeout;
        return this;
    }

    /**
     * Sets the list of shards the {@link DefaultShardManager DefaultShardManager} should contain.
     *
     * <p><b>This does not have any effect if the total shard count is set to {@code -1} (get recommended shards from discord).</b>
     *
     * @param  shardIds
     *         The list of shard ids
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setShards(final int... shardIds)
    {
        Checks.notNull(shardIds, "shardIds");
        for (int id : shardIds)
        {
            Checks.notNegative(id, "minShardId");
            Checks.check(id < this.shardsTotal, "maxShardId must be lower than shardsTotal");
        }

        this.shards = Arrays.stream(shardIds).boxed().collect(Collectors.toSet());

        return this;
    }

    /**
     * Sets the range of shards the {@link DefaultShardManager DefaultShardManager} should contain.
     * This is useful if you want to split your shards between multiple JVMs or servers.
     *
     * <p><b>This does not have any effect if the total shard count is set to {@code -1} (get recommended shards from discord).</b>
     *
     * @param  minShardId
     *         The lowest shard id the DefaultShardManager should contain
     *
     * @param  maxShardId
     *         The highest shard id the DefaultShardManager should contain
     *
     * @throws IllegalArgumentException
     *         If either minShardId is negative, maxShardId is lower than shardsTotal or
     *         minShardId is lower than or equal to maxShardId
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setShards(final int minShardId, final int maxShardId)
    {
        Checks.notNegative(minShardId, "minShardId");
        Checks.check(maxShardId < this.shardsTotal, "maxShardId must be lower than shardsTotal");
        Checks.check(minShardId <= maxShardId, "minShardId must be lower than or equal to maxShardId");

        List<Integer> shards = new ArrayList<>(maxShardId - minShardId + 1);
        for (int i = minShardId; i <= maxShardId; i++)
            shards.add(i);

        this.shards = shards;

        return this;
    }

    /**
     * Sets the range of shards the {@link DefaultShardManager DefaultShardManager} should contain.
     * This is useful if you want to split your shards between multiple JVMs or servers.
     *
     * <p><b>This does not have any effect if the total shard count is set to {@code -1} (get recommended shards from discord).</b>
     *
     * @param  shardIds
     *         The list of shard ids
     *
     * @throws IllegalArgumentException
     *         If either minShardId is negative, maxShardId is lower than shardsTotal or
     *         minShardId is lower than or equal to maxShardId
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setShards(@Nonnull Collection<Integer> shardIds)
    {
        Checks.notNull(shardIds, "shardIds");
        for (Integer id : shardIds)
        {
            Checks.notNegative(id, "minShardId");
            Checks.check(id < this.shardsTotal, "maxShardId must be lower than shardsTotal");
        }

        this.shards = new ArrayList<>(shardIds);

        return this;
    }

    /**
     * This will set the total amount of shards the {@link DefaultShardManager DefaultShardManager} should use.
     * <p> If this is set to {@code -1} JDA will automatically retrieve the recommended amount of shards from discord (default behavior).
     *
     * @param  shardsTotal
     *         The number of overall shards or {@code -1} if JDA should use the recommended amount from discord.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see    #setShards(int, int)
     */
    @Nonnull
    public DefaultShardManagerBuilder setShardsTotal(final int shardsTotal)
    {
        Checks.check(shardsTotal == -1 || shardsTotal > 0, "shardsTotal must either be -1 or greater than 0");
        this.shardsTotal = shardsTotal;

        return this;
    }

    /**
     * Sets the token that will be used by the {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager} instance to log in when
     * {@link net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder#build() build()} is called.
     *
     * <p>To get a bot token:
     * <ol>
     *     <li>Go to your <a href="https://discordapp.com/developers/applications/me">Discord Applications</a></li>
     *     <li>Create or select an already existing application</li>
     *     <li>Verify that it has already been turned into a Bot. If you see the "Create a Bot User" button, click it.</li>
     *     <li>Click the <i>click to reveal</i> link beside the <b>Token</b> label to show your Bot's {@code token}</li>
     * </ol>
     *
     * @param  token
     *         The token of the account that you would like to login with.
     *
     * @throws java.lang.IllegalArgumentException
     *         If the token is either null or empty
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setToken(@Nonnull final String token)
    {
        Checks.notBlank(token, "token");

        this.token = token;
        return this;
    }

    /**
     * Whether the {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager} should use
     * {@link net.dv8tion.jda.api.JDA#shutdownNow() JDA#shutdownNow()} instead of
     * {@link net.dv8tion.jda.api.JDA#shutdown() JDA#shutdown()} to shutdown it's shards.
     * <br><b>Default</b>: {@code false}
     *
     * @param  useShutdownNow
     *         Whether the ShardManager should use JDA#shutdown() or not
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     *
     * @see net.dv8tion.jda.api.JDA#shutdown()
     * @see net.dv8tion.jda.api.JDA#shutdownNow()
     */
    @Nonnull
    public DefaultShardManagerBuilder setUseShutdownNow(final boolean useShutdownNow)
    {
        this.useShutdownNow = useShutdownNow;
        return this;
    }

    /**
     * Sets the {@link com.neovisionaries.ws.client.WebSocketFactory WebSocketFactory} that will be used by JDA's websocket client.
     * This can be used to set things such as connection timeout and proxy.
     *
     * @param  factory
     *         The new {@link com.neovisionaries.ws.client.WebSocketFactory WebSocketFactory} to use.
     *
     * @return The DefaultShardManagerBuilder instance. Useful for chaining.
     */
    @Nonnull
    public DefaultShardManagerBuilder setWebsocketFactory(@Nullable WebSocketFactory factory)
    {
        this.wsFactory = factory;
        return this;
    }

    /**
     * Builds a new {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager} instance and uses the provided token to start the login process.
     * <br>The login process runs in a different thread, so while this will return immediately, {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager} has not
     * finished loading, thus many {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager} methods have the chance to return incorrect information.
     * <br>The main use of this method is to start the JDA connect process and do other things in parallel while startup is
     * being performed like database connection or local resource loading.
     *
     * <p>Note that this method is async and as such will <b>not</b> block until all shards are started.
     *
     * @throws  LoginException
     *          If the provided token is invalid.
     * @throws  IllegalArgumentException
     *          If the provided token is empty or null.
     *
     * @return A {@link net.dv8tion.jda.api.sharding.ShardManager ShardManager} instance that has started the login process. It is unknown as
     *         to whether or not loading has finished when this returns.
     */
    @Nonnull
    public ShardManager build() throws LoginException, IllegalArgumentException
    {
        final ShardingConfig shardingConfig = new ShardingConfig(shardsTotal, useShutdownNow);
        final EventConfig eventConfig = new EventConfig(eventManagerProvider);
        listeners.forEach(eventConfig::addEventListener);
        listenerProviders.forEach(eventConfig::addEventListenerProvider);
        final PresenceProviderConfig presenceConfig = new PresenceProviderConfig();
        presenceConfig.setActivityProvider(activityProvider);
        presenceConfig.setStatusProvider(statusProvider);
        presenceConfig.setIdleProvider(idleProvider);
        final ThreadingProviderConfig threadingConfig = new ThreadingProviderConfig(rateLimitPoolProvider, gatewayPoolProvider, callbackPoolProvider, threadFactory);
        final ShardingSessionConfig sessionConfig = new ShardingSessionConfig(sessionController, voiceDispatchInterceptor, httpClient, httpClientBuilder, wsFactory, audioSendFactory, enableVoice, retryOnTimeout, autoReconnect, enableBulkDeleteSplitting, maxReconnectDelay);
        final ShardingMetaConfig metaConfig = new ShardingMetaConfig(contextProvider, cacheFlags, enableContext, useShutdownNow, compression);
        final DefaultShardManager manager = new DefaultShardManager(this.token, this.shards, shardingConfig, eventConfig, presenceConfig, threadingConfig, sessionConfig, metaConfig);

        manager.login();

        return manager;
    }

    //Avoid having multiple anonymous classes
    private static class ThreadPoolProviderImpl<T extends ExecutorService> implements ThreadPoolProvider<T>
    {
        private final boolean autoShutdown;
        private final T pool;

        public ThreadPoolProviderImpl(T pool, boolean autoShutdown)
        {
            this.autoShutdown = autoShutdown;
            this.pool = pool;
        }

        @Override
        public T provide(int shardId)
        {
            return pool;
        }

        @Override
        public boolean shouldShutdownAutomatically(int shardId)
        {
            return autoShutdown;
        }
    }
}