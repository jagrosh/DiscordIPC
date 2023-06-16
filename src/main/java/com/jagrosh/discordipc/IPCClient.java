/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jagrosh.discordipc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.jagrosh.discordipc.entities.*;
import com.jagrosh.discordipc.entities.Packet.OpCode;
import com.jagrosh.discordipc.entities.pipe.Pipe;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;

/**
 * Represents a Discord IPC Client that can send and receive
 * Rich Presence data.
 * <p>
 * The ID provided should be the <b>client ID of the particular
 * application providing Rich Presence</b>, which can be found
 * <a href=https://discord.com/developers/applications/me>here</a>.
 * <p>
 * When initially created using {@link #IPCClient(long, boolean, String)} the client will
 * be inactive awaiting a call to {@link #connect(DiscordBuild...)}.<br>
 * After the call, this client can send and receive Rich Presence data
 * to and from discord via {@link #sendRichPresence(RichPresence)} and
 * {@link #setListener(IPCListener)} respectively.
 * <p>
 * Please be mindful that the client created is initially unconnected,
 * and calling any methods that exchange data between this client and
 * Discord before a call to {@link #connect(DiscordBuild...)} will cause
 * an {@link IllegalStateException} to be thrown.<br>
 * This also means that the IPCClient cannot tell whether the client ID
 * provided is valid or not before a handshake.
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public final class IPCClient implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(IPCClient.class);
    private Logger FORCED_LOGGER = null;
    private final long clientId;
    private final boolean autoRegister;
    private final HashMap<String, Callback> callbacks = new HashMap<>();
    private final String applicationId, optionalSteamId;
    private volatile Pipe pipe;
    private IPCListener listener = null;
    private Thread readThread = null;
    private String encoding = "UTF-8";
    private boolean debugMode;
    private boolean verboseLogging;

    /**
     * Constructs a new IPCClient using the provided {@code clientId}.<br>
     * This is initially unconnected to Discord.
     *
     * @param clientId        The Rich Presence application's client ID, which can be found
     *                        <a href=https://discord.com/developers/applications/me>here</a>
     * @param debugMode       Whether Debug Logging should be shown for this client
     * @param verboseLogging  Whether excess/deeper-rooted logging should be shown
     * @param autoRegister    Whether to register as an application with discord
     * @param applicationId   The application id to register with, usually the client id in string form
     * @param optionalSteamId The steam id to register with, registers as a steam game if present
     */
    public IPCClient(long clientId, boolean debugMode, boolean verboseLogging, boolean autoRegister, String applicationId, String optionalSteamId) {
        this.clientId = clientId;
        this.debugMode = debugMode;
        this.verboseLogging = verboseLogging;
        this.applicationId = applicationId;
        this.autoRegister = autoRegister;
        this.optionalSteamId = optionalSteamId;
    }

    /**
     * Constructs a new IPCClient using the provided {@code clientId}.<br>
     * This is initially unconnected to Discord.
     *
     * @param clientId       The Rich Presence application's client ID, which can be found
     *                       <a href=https://discord.com/developers/applications/me>here</a>
     * @param debugMode      Whether Debug Logging should be shown for this client
     * @param verboseLogging Whether excess/deeper-rooted logging should be shown
     * @param autoRegister   Whether to register as an application with discord
     * @param applicationId  The application id to register with, usually the client id in string form
     */
    public IPCClient(long clientId, boolean debugMode, boolean verboseLogging, boolean autoRegister, String applicationId) {
        this(clientId, debugMode, verboseLogging, autoRegister, applicationId, null);
    }

    /**
     * Constructs a new IPCClient using the provided {@code clientId}.<br>
     * This is initially unconnected to Discord.
     *
     * @param clientId       The Rich Presence application's client ID, which can be found
     *                       <a href=https://discord.com/developers/applications/me>here</a>
     * @param debugMode      Whether Debug Logging should be shown for this client
     * @param verboseLogging Whether excess/deeper-rooted logging should be shown
     */
    public IPCClient(long clientId, boolean debugMode, boolean verboseLogging) {
        this(clientId, debugMode, verboseLogging, false, null);
    }

    /**
     * Constructs a new IPCClient using the provided {@code clientId}.<br>
     * This is initially unconnected to Discord.
     *
     * @param clientId        The Rich Presence application's client ID, which can be found
     *                        <a href=https://discord.com/developers/applications/me>here</a>
     * @param debugMode       Whether Debug Logging should be shown for this client
     * @param autoRegister    Whether to register as an application with discord
     * @param applicationId   The application id to register with, usually the client id in string form
     * @param optionalSteamId The steam id to register with, registers as a steam game if present
     */
    public IPCClient(long clientId, boolean debugMode, boolean autoRegister, String applicationId, String optionalSteamId) {
        this(clientId, debugMode, false, autoRegister, applicationId, optionalSteamId);
    }

    /**
     * Constructs a new IPCClient using the provided {@code clientId}.<br>
     * This is initially unconnected to Discord.
     *
     * @param clientId      The Rich Presence application's client ID, which can be found
     *                      <a href=https://discord.com/developers/applications/me>here</a>
     * @param debugMode     Whether Debug Logging should be shown for this client
     * @param autoRegister  Whether to register as an application with discord
     * @param applicationId The application id to register with, usually the client id in string form
     */
    public IPCClient(long clientId, boolean debugMode, boolean autoRegister, String applicationId) {
        this(clientId, debugMode, autoRegister, applicationId, null);
    }

    /**
     * Constructs a new IPCClient using the provided {@code clientId}.<br>
     * This is initially unconnected to Discord.
     *
     * @param clientId  The Rich Presence application's client ID, which can be found
     *                  <a href=https://discord.com/developers/applications/me>here</a>
     * @param debugMode Whether Debug Logging should be shown for this client
     */
    public IPCClient(long clientId, boolean debugMode) {
        this(clientId, debugMode, false, null);
    }

    /**
     * Constructs a new IPCClient using the provided {@code clientId}.<br>
     * This is initially unconnected to Discord.
     *
     * @param clientId        The Rich Presence application's client ID, which can be found
     *                        <a href=https://discord.com/developers/applications/me>here</a>
     * @param autoRegister    Whether to register as an application with discord
     * @param applicationId   The application id to register with, usually the client id in string form
     * @param optionalSteamId The steam id to register with, registers as a steam game if present
     */
    public IPCClient(long clientId, boolean autoRegister, String applicationId, String optionalSteamId) {
        this(clientId, false, autoRegister, applicationId, optionalSteamId);
    }

    /**
     * Constructs a new IPCClient using the provided {@code clientId}.<br>
     * This is initially unconnected to Discord.
     *
     * @param clientId      The Rich Presence application's client ID, which can be found
     *                      <a href=https://discord.com/developers/applications/me>here</a>
     * @param autoRegister  Whether to register as an application with discord
     * @param applicationId The application id to register with, usually the client id in string form
     */
    public IPCClient(long clientId, boolean autoRegister, String applicationId) {
        this(clientId, autoRegister, applicationId, null);
    }

    /**
     * Constructs a new IPCClient using the provided {@code clientId}.<br>
     * This is initially unconnected to Discord.
     *
     * @param clientId The Rich Presence application's client ID, which can be found
     *                 <a href=https://discord.com/developers/applications/me>here</a>
     */
    public IPCClient(long clientId) {
        this(clientId, false, null);
    }

    /**
     * Finds the current process ID.
     *
     * @return The current process ID.
     */
    private static int getPID() {
        String pr = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(pr.substring(0, pr.indexOf('@')));
    }

    /**
     * Retrieves the current logger that should be used
     *
     * @param instance The logger instance
     * @return the current logger to use
     */
    public Logger getCurrentLogger(final Logger instance) {
        return FORCED_LOGGER != null ? FORCED_LOGGER : instance;
    }

    /**
     * Sets the current logger that should be used
     *
     * @param forcedLogger The logger instance to be used
     */
    public void setForcedLogger(Logger forcedLogger) {
        FORCED_LOGGER = forcedLogger;
    }

    /**
     * Sets this IPCClient's {@link IPCListener} to handle received events.
     * <p>
     * A single IPCClient can only have one of these set at any given time.<br>
     * Setting this {@code null} will remove the currently active one.
     * <p>
     * This can be set safely before a call to {@link #connect(DiscordBuild...)}
     * is made.
     *
     * @param listener The {@link IPCListener} to set for this IPCClient.
     * @see IPCListener
     */
    public void setListener(IPCListener listener) {
        this.listener = listener;
        if (pipe != null)
            pipe.setListener(listener);
    }

    /**
     * Gets the application id associated with this IPCClient
     * <p>
     * This must be set upon initialization and is a required variable
     *
     * @return applicationId
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Gets the steam id associated with this IPCClient, if any
     * <p>
     * This must be set upon initialization and is an optional variable<br>
     * If set and autoRegister is true, then this client will register as a steam game
     *
     * @return optionalSteamId
     */
    public String getOptionalSteamId() {
        return optionalSteamId;
    }

    /**
     * Gets whether the client will register a run command with discord
     *
     * @return autoRegister
     */
    public boolean isAutoRegister() {
        return autoRegister;
    }

    /**
     * Gets encoding to send packets in.<p>
     * Default: UTF-8
     *
     * @return encoding
     */
    public String getEncoding() {
        return this.encoding;
    }

    /**
     * Sets the encoding to send packets in.
     * <p>
     * This can be set safely before a call to {@link #connect(DiscordBuild...)}
     * is made.
     * <p>
     * Default: UTF-8
     *
     * @param encoding for this IPCClient.
     */
    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    /**
     * Gets the client ID associated with this IPCClient
     *
     * @return the client id
     */
    public long getClientID() {
        return this.clientId;
    }

    /**
     * Gets whether this IPCClient is in Debug Mode
     * Default: False
     *
     * @return The Debug Mode Status
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Sets whether this IPCClient is in Debug Mode
     *
     * @param debugMode The Debug Mode Status
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Gets whether this IPCClient will show verbose logging
     * Default: False
     *
     * @return The Verbose Logging Status
     */
    public boolean isVerboseLogging() {
        return verboseLogging;
    }

    /**
     * Sets whether this IPCClient will show verbose logging
     *
     * @param verboseLogging The Verbose Mode Status
     */
    public void setVerboseLogging(boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
    }

    /**
     * Opens the connection between the IPCClient and Discord.<p>
     *
     * <b>This must be called before any data is exchanged between the
     * IPCClient and Discord.</b>
     *
     * @param preferredOrder the priority order of client builds to connect to
     * @throws IllegalStateException    There is an open connection on this IPCClient.
     * @throws NoDiscordClientException No client of the provided {@link DiscordBuild build type}(s) was found.
     */
    public void connect(DiscordBuild... preferredOrder) throws NoDiscordClientException {
        checkConnected(false);
        callbacks.clear();
        pipe = null;

        pipe = Pipe.openPipe(this, clientId, callbacks, preferredOrder);

        if (isAutoRegister()) {
            try {
                if (optionalSteamId != null && !optionalSteamId.isEmpty())
                    this.registerSteamGame(getApplicationId(), optionalSteamId);
                else
                    this.registerApp(getApplicationId(), null);
            } catch (Exception | Error ex) {
                if (debugMode) {
                    ex.printStackTrace();
                } else {
                    getCurrentLogger(LOGGER).error("Unable to register application, enable debug mode for trace...");
                }
            }
        }

        if (debugMode) {
            getCurrentLogger(LOGGER).info("[DEBUG] Client is now connected and ready!");
        }

        if (listener != null) {
            listener.onReady(this);
            pipe.setListener(listener);
        }
        startReading();
    }

    /**
     * Sends a {@link RichPresence} to the Discord client.
     * <p>
     * This is where the IPCClient will officially display
     * a Rich Presence in the Discord client.
     * <p>
     * Sending this again will overwrite the last provided
     * {@link RichPresence}.
     *
     * @param presence The {@link RichPresence} to send.
     * @throws IllegalStateException If a connection was not made prior to invoking
     *                               this method.
     * @see RichPresence
     */
    public void sendRichPresence(RichPresence presence) {
        sendRichPresence(presence, null);
    }

    /**
     * Sends a {@link RichPresence} to the Discord client.
     * <p>
     * This is where the IPCClient will officially display
     * a Rich Presence in the Discord client.
     * <p>
     * Sending this again will overwrite the last provided
     * {@link RichPresence}.
     *
     * @param presence The {@link RichPresence} to send.
     * @param callback A {@link Callback} to handle success or error
     * @throws IllegalStateException If a connection was not made prior to invoking
     *                               this method.
     * @see RichPresence
     */
    public void sendRichPresence(RichPresence presence, Callback callback) {
        checkConnected(true);

        if (debugMode) {
            getCurrentLogger(LOGGER).info("[DEBUG] Sending RichPresence to discord: " + (presence == null ? null : presence.toDecodedJson(encoding)));
        }

        // Setup and Send JsonObject Data Representing an RPC Update
        JsonObject finalObject = new JsonObject(),
                args = new JsonObject();

        finalObject.addProperty("cmd", "SET_ACTIVITY");

        args.addProperty("pid", getPID());
        args.add("activity", presence == null ? new JsonObject() : presence.toJson());

        finalObject.add("args", args);
        pipe.send(OpCode.FRAME, finalObject, callback);
    }

    /**
     * Manually register a steam game
     *
     * @param applicationId   Application ID
     * @param optionalSteamId Application Steam ID
     */
    public void registerSteamGame(String applicationId, String optionalSteamId) {
        if (this.pipe != null)
            this.pipe.registerSteamGame(applicationId, optionalSteamId);
    }

    /**
     * Manually register an application
     *
     * @param applicationId Application ID
     * @param command       Command to run the application
     */
    public void registerApp(String applicationId, String command) {
        if (this.pipe != null)
            this.pipe.registerApp(applicationId, command);
    }

    /**
     * Adds an event {@link Event} to this IPCClient.<br>
     * If the provided {@link Event} is added more than once,
     * it does nothing.
     * Once added, there is no way to remove the subscription
     * other than {@link #close() closing} the connection
     * and creating a new one.
     *
     * @param sub The event {@link Event} to add.
     * @throws IllegalStateException If a connection was not made prior to invoking
     *                               this method.
     */
    public void subscribe(Event sub) {
        subscribe(sub, null);
    }

    /**
     * Adds an event {@link Event} to this IPCClient.<br>
     * If the provided {@link Event} is added more than once,
     * it does nothing.
     * Once added, there is no way to remove the subscription
     * other than {@link #close() closing} the connection
     * and creating a new one.
     *
     * @param sub      The event {@link Event} to add.
     * @param callback The {@link Callback} to handle success or failure
     * @throws IllegalStateException If a connection was not made prior to invoking
     *                               this method.
     */
    public void subscribe(Event sub, Callback callback) {
        checkConnected(true);
        if (!sub.isSubscribable())
            throw new IllegalStateException("Cannot subscribe to " + sub + " event!");

        if (debugMode) {
            getCurrentLogger(LOGGER).info(String.format("[DEBUG] Subscribing to Event: %s", sub.name()));
        }

        JsonObject pipeData = new JsonObject();
        pipeData.addProperty("cmd", "SUBSCRIBE");
        pipeData.addProperty("evt", sub.name());

        pipe.send(OpCode.FRAME, pipeData, callback);
    }

    /**
     * Responds to a {@link Event#ACTIVITY_JOIN_REQUEST} from a requester {@link User}.
     *
     * @param user         The {@link User} to respond to
     * @param approvalMode The {@link ApprovalMode} to respond to the requester with
     * @param callback     The {@link Callback} to handle success or failure
     */
    public void respondToJoinRequest(User user, ApprovalMode approvalMode, Callback callback) {
        checkConnected(true);

        if (user != null) {
            if (debugMode) {
                getCurrentLogger(LOGGER).info(String.format("[DEBUG] Sending response to %s as %s", user.getName(), approvalMode.name()));
            }

            JsonObject pipeData = new JsonObject();
            pipeData.addProperty("cmd", approvalMode == ApprovalMode.ACCEPT ? "SEND_ACTIVITY_JOIN_INVITE" : "CLOSE_ACTIVITY_JOIN_REQUEST");

            JsonObject args = new JsonObject();
            args.addProperty("user_id", user.getId());

            pipeData.add("args", args);

            pipe.send(OpCode.FRAME, pipeData, callback);
        }
    }

    /**
     * Responds to a {@link Event#ACTIVITY_JOIN_REQUEST} from a requester {@link User}.
     *
     * @param user         The {@link User} to respond to
     * @param approvalMode The {@link ApprovalMode} to respond to the requester with
     */
    public void respondToJoinRequest(User user, ApprovalMode approvalMode) {
        respondToJoinRequest(user, approvalMode, null);
    }

    /**
     * Gets the IPCClient's current {@link PipeStatus}.
     *
     * @return The IPCClient's current {@link PipeStatus}.
     */
    public PipeStatus getStatus() {
        if (pipe == null) return PipeStatus.UNINITIALIZED;

        return pipe.getStatus();
    }

    /**
     * Attempts to close an open connection to Discord.<br>
     * This can be reopened with another call to {@link #connect(DiscordBuild...)}.
     *
     * @throws IllegalStateException If a connection was not made prior to invoking
     *                               this method.
     */
    @Override
    public void close() {
        checkConnected(true);

        try {
            pipe.close();
        } catch (IOException e) {
            if (debugMode) {
                getCurrentLogger(LOGGER).info(String.format("[DEBUG] Failed to close pipe: %s", e));
            }
        }
    }

    /**
     * Gets the IPCClient's {@link DiscordBuild}.
     * <p>
     * This is always the first specified DiscordBuild when
     * making a call to {@link #connect(DiscordBuild...)},
     * or the first one found if none or {@link DiscordBuild#ANY}
     * is specified.
     * <p>
     * Note that specifying ANY doesn't mean that this will return
     * ANY. In fact this method should <b>never</b> return the
     * value ANY.
     *
     * @return The {@link DiscordBuild} of this IPCClient, or null if not connected.
     */
    public DiscordBuild getDiscordBuild() {
        if (pipe == null) return null;

        return pipe.getDiscordBuild();
    }

    /**
     * Gets the IPCClient's current {@link User} attached to the target {@link DiscordBuild}.
     * <p>
     * This is always the User Data attached to the DiscordBuild found when
     * making a call to {@link #connect(DiscordBuild...)}
     * <p>
     * Note that this value should NOT return null under any circumstances.
     *
     * @return The current {@link User} of this IPCClient from the target {@link DiscordBuild}, or null if not found.
     */
    public User getCurrentUser() {
        if (pipe == null) return null;

        return pipe.getCurrentUser();
    }


    // Private methods

    /**
     * Makes sure that the client is connected (or not) depending on if it should
     * for the current state.
     *
     * @param connected Whether to check in the context of the IPCClient being
     *                  connected or not.
     */
    private void checkConnected(boolean connected) {
        if (connected && getStatus() != PipeStatus.CONNECTED)
            throw new IllegalStateException(String.format("IPCClient (ID: %d) is not connected!", clientId));
        if (!connected && getStatus() == PipeStatus.CONNECTED)
            throw new IllegalStateException(String.format("IPCClient (ID: %d) is already connected!", clientId));
    }

    /**
     * Initializes this IPCClient's {@link IPCClient#readThread readThread}
     * and calls the first {@link Pipe#read()}.
     */
    private void startReading() {
        final IPCClient localInstance = this;

        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                IPCClient.this.readPipe(localInstance);
            }
        }, "IPCClient-Reader");
        readThread.setDaemon(true);

        if (debugMode) {
            getCurrentLogger(LOGGER).info("[DEBUG] Starting IPCClient reading thread!");
        }
        readThread.start();
    }

    /**
     * Call the first {@link Pipe#read()} via try-catch
     *
     * @param instance The {@link IPCClient} instance
     */
    private void readPipe(final IPCClient instance) {
        try {
            Packet p;
            while ((p = pipe.read()).getOp() != OpCode.CLOSE) {
                JsonObject json = p.getJson();

                if (json != null) {
                    Event event = Event.of(json.has("evt") && !json.get("evt").isJsonNull() ? json.getAsJsonPrimitive("evt").getAsString() : null);
                    String nonce = json.has("nonce") && !json.get("nonce").isJsonNull() ? json.getAsJsonPrimitive("nonce").getAsString() : null;

                    switch (event) {
                        case NULL:
                            if (nonce != null && callbacks.containsKey(nonce))
                                callbacks.remove(nonce).succeed(p);
                            break;

                        case ERROR:
                            if (nonce != null && callbacks.containsKey(nonce))
                                callbacks.remove(nonce).fail(json.has("data") && json.getAsJsonObject("data").has("message") ? json.getAsJsonObject("data").getAsJsonObject("message").getAsString() : null);
                            break;

                        case ACTIVITY_JOIN:
                            if (debugMode) {
                                getCurrentLogger(LOGGER).info("[DEBUG] Reading thread received a 'join' event.");
                            }
                            break;

                        case ACTIVITY_SPECTATE:
                            if (debugMode) {
                                getCurrentLogger(LOGGER).info("[DEBUG] Reading thread received a 'spectate' event.");
                            }
                            break;

                        case ACTIVITY_JOIN_REQUEST:
                            if (debugMode) {
                                getCurrentLogger(LOGGER).info("[DEBUG] Reading thread received a 'join request' event.");
                            }
                            break;

                        case UNKNOWN:
                            if (debugMode) {
                                getCurrentLogger(LOGGER).info("[DEBUG] Reading thread encountered an event with an unknown type: " +
                                        json.getAsJsonPrimitive("evt").getAsString());
                            }
                            break;
                        default:
                            break;
                    }

                    if (listener != null && json.has("cmd") && json.getAsJsonPrimitive("cmd").getAsString().equals("DISPATCH")) {
                        try {
                            JsonObject data = json.getAsJsonObject("data");
                            switch (Event.of(json.getAsJsonPrimitive("evt").getAsString())) {
                                case ACTIVITY_JOIN:
                                    listener.onActivityJoin(instance, data.getAsJsonPrimitive("secret").getAsString());
                                    break;

                                case ACTIVITY_SPECTATE:
                                    listener.onActivitySpectate(instance, data.getAsJsonPrimitive("secret").getAsString());
                                    break;

                                case ACTIVITY_JOIN_REQUEST:
                                    final JsonObject u = data.getAsJsonObject("user");
                                    final User user = new User(
                                            u.getAsJsonPrimitive("username").getAsString(),
                                            u.has("discriminator") ? u.getAsJsonPrimitive("discriminator").getAsString() : "0",
                                            Long.parseLong(u.getAsJsonPrimitive("id").getAsString()),
                                            u.has("avatar") && u.get("avatar").isJsonPrimitive() ? u.getAsJsonPrimitive("avatar").getAsString() : null
                                    );
                                    listener.onActivityJoinRequest(instance, data.has("secret") ? data.getAsJsonObject("secret").getAsString() : null, user);
                                    break;
                                default:
                                    break;
                            }
                        } catch (Exception e) {
                            getCurrentLogger(LOGGER).error(String.format("Exception when handling event: %s", e));
                        }
                    }
                }
            }
            pipe.setStatus(PipeStatus.DISCONNECTED);
            if (listener != null)
                listener.onClose(instance, p.getJson());
        } catch (IOException | JsonParseException ex) {
            getCurrentLogger(LOGGER).error(String.format("Reading thread encountered an Exception: %s", ex));

            pipe.setStatus(PipeStatus.DISCONNECTED);
            if (listener != null)
                listener.onDisconnect(instance, ex);
        }
    }

    // Private static methods

    /**
     * Constants representing a Response to an Ask to Join or Spectate Request
     */
    public enum ApprovalMode {
        ACCEPT, DENY
    }

    /**
     * Constants representing events that can be subscribed to
     * using {@link #subscribe(Event)}.
     * <p>
     * Each event corresponds to a different function as a
     * component of the Rich Presence.<br>
     * A full breakdown of each is available
     * <a href=https://discord.com/developers/docs/rich-presence/how-to>here</a>.
     */
    public enum Event {
        NULL(false), // used for confirmation
        READY(false),
        ERROR(false),
        ACTIVITY_JOIN(true),
        ACTIVITY_SPECTATE(true),
        ACTIVITY_JOIN_REQUEST(true),
        /**
         * A backup key, only important if the
         * IPCClient receives an unknown event
         * type in a JSON payload.
         */
        UNKNOWN(false);

        private final boolean subscribable;

        Event(boolean subscribable) {
            this.subscribable = subscribable;
        }

        static Event of(String str) {
            if (str == null)
                return NULL;
            for (Event s : Event.values()) {
                if (s != UNKNOWN && s.name().equalsIgnoreCase(str))
                    return s;
            }
            return UNKNOWN;
        }

        public boolean isSubscribable() {
            return subscribable;
        }
    }
}
