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

import com.jagrosh.discordipc.entities.*;
import com.jagrosh.discordipc.entities.Packet.OpCode;
import com.jagrosh.discordipc.entities.pipe.Pipe;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;

/**
 * Represents a Discord IPC Client that can send and receive
 * Rich Presence data.<p>
 *
 * The ID provided should be the <b>client ID of the particular
 * application providing Rich Presence</b>, which can be found
 * <a href=https://discordapp.com/developers/applications/me>here</a>.<p>
 *
 * When initially created using {@link #IPCClient(long)} the client will
 * be inactive awaiting a call to {@link #connect(DiscordBuild...)}.<br>
 * After the call, this client can send and receive Rich Presence data
 * to and from discord via {@link #sendRichPresence(RichPresence)} and
 * {@link #setListener(IPCListener)} respectively.<p>
 *
 * Please be mindful that the client created is initially unconnected,
 * and calling any methods that exchange data between this client and
 * Discord before a call to {@link #connect(DiscordBuild...)} will cause
 * an {@link IllegalStateException} to be thrown.<br>
 * This also means that the IPCClient cannot tell whether the client ID
 * provided is valid or not before a handshake.
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public final class IPCClient implements Closeable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IPCClient.class);
    private final long clientId;
    private final HashMap<String,Callback> callbacks = new HashMap<>();
    private volatile Pipe pipe;
    private IPCListener listener = null;
    private Thread readThread = null;
    
    /**
     * Constructs a new IPCClient using the provided {@code clientId}.<br>
     * This is initially unconnected to Discord.
     *
     * @param clientId The Rich Presence application's client ID, which can be found
     *                 <a href=https://discordapp.com/developers/applications/me>here</a>
     */
    public IPCClient(long clientId)
    {
        this.clientId = clientId;
    }
    
    /**
     * Sets this IPCClient's {@link IPCListener} to handle received events.<p>
     *
     * A single IPCClient can only have one of these set at any given time.<br>
     * Setting this {@code null} will remove the currently active one.<p>
     *
     * This can be set safely before a call to {@link #connect(DiscordBuild...)}
     * is made.
     *
     * @param listener The {@link IPCListener} to set for this IPCClient.
     *
     * @see IPCListener
     */
    public void setListener(IPCListener listener)
    {
        this.listener = listener;
        if (pipe != null)
            pipe.setListener(listener);
    }
    
    /**
     * Opens the connection between the IPCClient and Discord.<p>
     *
     * <b>This must be called before any data is exchanged between the
     * IPCClient and Discord.</b>
     *
     * @param preferredOrder the priority order of client builds to connect to
     *
     * @throws IllegalStateException
     *         There is an open connection on this IPCClient.
     * @throws NoDiscordClientException
     *         No client of the provided {@link DiscordBuild build type}(s) was found.
     */
    public void connect(DiscordBuild... preferredOrder) throws NoDiscordClientException
    {
        checkConnected(false);
        callbacks.clear();
        pipe = null;

        pipe = Pipe.openPipe(this, clientId, callbacks, preferredOrder);

        LOGGER.debug("Client is now connected and ready!");
        if(listener != null)
            listener.onReady(this);
        startReading();
    }
    
    /**
     * Sends a {@link RichPresence} to the Discord client.<p>
     *
     * This is where the IPCClient will officially display
     * a Rich Presence in the Discord client.<p>
     *
     * Sending this again will overwrite the last provided
     * {@link RichPresence}.
     *
     * @param presence The {@link RichPresence} to send.
     *
     * @throws IllegalStateException
     *         If a connection was not made prior to invoking
     *         this method.
     *
     * @see RichPresence
     */
    public void sendRichPresence(RichPresence presence)
    {
        sendRichPresence(presence, null);
    }
    
    /**
     * Sends a {@link RichPresence} to the Discord client.<p>
     *
     * This is where the IPCClient will officially display
     * a Rich Presence in the Discord client.<p>
     *
     * Sending this again will overwrite the last provided
     * {@link RichPresence}.
     *
     * @param presence The {@link RichPresence} to send.
     * @param callback A {@link Callback} to handle success or error
     *
     * @throws IllegalStateException
     *         If a connection was not made prior to invoking
     *         this method.
     *
     * @see RichPresence
     */
    public void sendRichPresence(RichPresence presence, Callback callback)
    {
        checkConnected(true);
        LOGGER.debug("Sending RichPresence to discord: "+(presence == null ? null : presence.toJson().toString()));
        pipe.send(OpCode.FRAME, new JSONObject()
                            .put("cmd","SET_ACTIVITY")
                            .put("args", new JSONObject()
                                        .put("pid",getPID())
                                        .put("activity",presence == null ? null : presence.toJson())), callback);
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
     *
     * @throws IllegalStateException
     *         If a connection was not made prior to invoking
     *         this method.
     */
    public void subscribe(Event sub)
    {
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
     * @param sub The event {@link Event} to add.
     * @param callback The {@link Callback} to handle success or failure
     *
     * @throws IllegalStateException
     *         If a connection was not made prior to invoking
     *         this method.
     */
    public void subscribe(Event sub, Callback callback)
    {
        checkConnected(true);
        if(!sub.isSubscribable())
            throw new IllegalStateException("Cannot subscribe to "+sub+" event!");
        LOGGER.debug(String.format("Subscribing to Event: %s", sub.name()));
        pipe.send(OpCode.FRAME, new JSONObject()
                            .put("cmd", "SUBSCRIBE")
                            .put("evt", sub.name()), callback);
    }

    /**
     * Gets the IPCClient's current {@link PipeStatus}.
     *
     * @return The IPCClient's current {@link PipeStatus}.
     */
    public PipeStatus getStatus()
    {
        if (pipe == null) return PipeStatus.UNINITIALIZED;

        return pipe.getStatus();
    }

    /**
     * Attempts to close an open connection to Discord.<br>
     * This can be reopened with another call to {@link #connect(DiscordBuild...)}.
     *
     * @throws IllegalStateException
     *         If a connection was not made prior to invoking
     *         this method.
     */
    @Override
    public void close()
    {
        checkConnected(true);

        try {
            pipe.close();
        } catch (IOException e) {
            LOGGER.debug("Failed to close pipe", e);
        }
    }

    /**
     * Gets the IPCClient's {@link DiscordBuild}.<p>
     *
     * This is always the first specified DiscordBuild when
     * making a call to {@link #connect(DiscordBuild...)},
     * or the first one found if none or {@link DiscordBuild#ANY}
     * is specified.<p>
     *
     * Note that specifying ANY doesn't mean that this will return
     * ANY. In fact this method should <b>never</b> return the
     * value ANY.
     *
     * @return The {@link DiscordBuild} of this IPCClient, or null if not connected.
     */
    public DiscordBuild getDiscordBuild()
    {
        if (pipe == null) return null;

        return pipe.getDiscordBuild();
    }

    /**
     * Constants representing events that can be subscribed to
     * using {@link #subscribe(Event)}.<p>
     *
     * Each event corresponds to a different function as a
     * component of the Rich Presence.<br>
     * A full breakdown of each is available
     * <a href=https://discordapp.com/developers/docs/rich-presence/how-to>here</a>.
     */
    public enum Event
    {
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
        
        Event(boolean subscribable)
        {
            this.subscribable = subscribable;
        }
        
        public boolean isSubscribable()
        {
            return subscribable;
        }
        
        static Event of(String str)
        {
            if(str==null)
                return NULL;
            for(Event s : Event.values())
            {
                if(s != UNKNOWN && s.name().equalsIgnoreCase(str))
                    return s;
            }
            return UNKNOWN;
        }
    }


    // Private methods
    
    /**
     * Makes sure that the client is connected (or not) depending on if it should
     * for the current state.
     *
     * @param connected Whether to check in the context of the IPCClient being
     *                  connected or not.
     */
    private void checkConnected(boolean connected)
    {
        if(connected && getStatus() != PipeStatus.CONNECTED)
            throw new IllegalStateException(String.format("IPCClient (ID: %d) is not connected!", clientId));
        if(!connected && getStatus() == PipeStatus.CONNECTED)
            throw new IllegalStateException(String.format("IPCClient (ID: %d) is already connected!", clientId));
    }
    
    /**
     * Initializes this IPCClient's {@link IPCClient#readThread readThread}
     * and calls the first {@link Pipe#read()}.
     */
    private void startReading()
    {
        readThread = new Thread(() -> {
            try
            {
                Packet p;
                while((p = pipe.read()).getOp() != OpCode.CLOSE)
                {
                    JSONObject json = p.getJson();
                    Event event = Event.of(json.optString("evt", null));
                    String nonce = json.optString("nonce", null);
                    switch(event)
                    {
                        case NULL:
                            if(nonce != null && callbacks.containsKey(nonce))
                                callbacks.remove(nonce).succeed(p);
                            break;
                            
                        case ERROR:
                            if(nonce != null && callbacks.containsKey(nonce))
                                callbacks.remove(nonce).fail(json.getJSONObject("data").optString("message", null));
                            break;
                            
                        case ACTIVITY_JOIN:
                            LOGGER.debug("Reading thread received a 'join' event.");
                            break;
                            
                        case ACTIVITY_SPECTATE:
                            LOGGER.debug("Reading thread received a 'spectate' event.");
                            break;
                            
                        case ACTIVITY_JOIN_REQUEST:
                            LOGGER.debug("Reading thread received a 'join request' event.");
                            break;
                            
                        case UNKNOWN:
                            LOGGER.debug("Reading thread encountered an event with an unknown type: " +
                                         json.getString("evt"));
                            break;
                    }
                    if(listener != null && json.has("cmd") && json.getString("cmd").equals("DISPATCH"))
                    {
                        try
                        {
                            JSONObject data = json.getJSONObject("data");
                            switch(Event.of(json.getString("evt")))
                            {
                                case ACTIVITY_JOIN:
                                    listener.onActivityJoin(this, data.getString("secret"));
                                    break;
                                    
                                case ACTIVITY_SPECTATE:
                                    listener.onActivitySpectate(this, data.getString("secret"));
                                    break;
                                    
                                case ACTIVITY_JOIN_REQUEST:
                                    JSONObject u = data.getJSONObject("user");
                                    User user = new User(
                                        u.getString("username"),
                                        u.getString("discriminator"),
                                        Long.parseLong(u.getString("id")),
                                        u.optString("avatar", null)
                                    );
                                    listener.onActivityJoinRequest(this, data.optString("secret", null), user);
                                    break;
                            }
                        }
                        catch(Exception e)
                        {
                            LOGGER.error("Exception when handling event: ", e);
                        }
                    }
                }
                pipe.setStatus(PipeStatus.DISCONNECTED);
                if(listener != null)
                    listener.onClose(this, p.getJson());
            }
            catch(IOException | JSONException ex)
            {
                if(ex instanceof IOException)
                    LOGGER.error("Reading thread encountered an IOException", ex);
                else
                    LOGGER.error("Reading thread encountered an JSONException", ex);

                pipe.setStatus(PipeStatus.DISCONNECTED);
                if(listener != null)
                    listener.onDisconnect(this, ex);
            }
        });

        LOGGER.debug("Starting IPCClient reading thread!");
        readThread.start();
    }
    
    // Private static methods
    
    /**
     * Finds the current process ID.
     *
     * @return The current process ID.
     */
    private static int getPID()
    {
        String pr = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(pr.substring(0,pr.indexOf('@')));
    }
}
