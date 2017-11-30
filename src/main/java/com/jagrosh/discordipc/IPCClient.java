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

import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.Packet.OpCode;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final int version = 1;
    private final long clientId;
    private final HashMap<String,Callback> callbacks = new HashMap<>();
    private Status status = Status.CREATED;
    private DiscordBuild build = null;
    private IPCListener listener = null;
    private RandomAccessFile pipe = null;
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
        this.status = Status.CONNECTING;
        if(preferredOrder == null || preferredOrder.length == 0)
            preferredOrder = new DiscordBuild[]{DiscordBuild.ANY};
        callbacks.clear();
        pipe = null;
        build = null;

        // store some files so we can get the preferred client
        RandomAccessFile[] open = new RandomAccessFile[DiscordBuild.values().length];
        for(int i = 0; i < 10; i++)
        {
            try
            {
                String ipc = getIPC(i);
                LOGGER.debug(String.format("Searching for IPC: %s", ipc));
                pipe = new RandomAccessFile(ipc, "rw");

                send(OpCode.HANDSHAKE, new JSONObject().put("v",version).put("client_id", Long.toString(clientId)), null);

                Packet p = read(); // this is a valid client at this point

                build = DiscordBuild.from(p.getJson().getJSONObject("data")
                                           .getJSONObject("config")
                                           .getString("api_endpoint"));

                LOGGER.debug(String.format("Found a valid client (%s) with packet: %s", build.name(), p.toString()));
                // we're done if we found our first choice
                if(build == preferredOrder[0] || DiscordBuild.ANY == preferredOrder[0]) {
                    LOGGER.info(String.format("Found preferred client: %s", build.name()));
                    break;
                }

                open[build.ordinal()] = pipe; // didn't find first choice yet, so store what we have
                open[DiscordBuild.ANY.ordinal()] = pipe; // also store in 'any' for use later

                build = null;
                pipe = null;
            }
            catch(IOException | JSONException ex)
            {
                pipe = null;
                build = null;
            }
        }

        if(pipe == null)
        {
            // we already know we don't have our first pick
            // check each of the rest to see if we have that
            for(int i = 1; i < preferredOrder.length; i++)
            {
                DiscordBuild cb = preferredOrder[i];
                LOGGER.debug(String.format("Looking for client build: %s", cb.name()));
                if(open[cb.ordinal()] != null)
                {
                    pipe = open[cb.ordinal()];
                    open[cb.ordinal()] = null;
                    if(cb == DiscordBuild.ANY) // if we pulled this from the 'any' slot, we need to figure out which build it was
                    {
                        for(int k = 0; k < open.length; k++)
                        {
                            if(open[k] == pipe)
                            {
                                build = DiscordBuild.values()[k];
                                open[k] = null; // we don't want to close this
                            }
                        }
                    }
                    else build = cb;

                    LOGGER.info(String.format("Found preferred client: %s", build.name()));
                    break;
                }
            }
            if(pipe == null)
            {
                this.status = Status.DISCONNECTED;
                throw new NoDiscordClientException();
            }
        }
        // close unused files, except skip 'any' because its always a duplicate
        for(int i = 0; i < open.length; i++)
        {
            if(i == DiscordBuild.ANY.ordinal())
                continue;
            if(open[i] != null)
            {
                try {
                    open[i].close();
                } catch(IOException ex) {
                    // This isn't really important to applications and better
                    // as debug info
                    LOGGER.debug("Failed to close an open IPC Pipe!", ex);
                }
            }
        }
        status = Status.CONNECTED;
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
        send(OpCode.FRAME, new JSONObject()
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
        send(OpCode.FRAME, new JSONObject()
                            .put("cmd", "SUBSCRIBE")
                            .put("evt", sub.name()), callback);
    }

    /**
     * Gets the IPCClient's current {@link Status}.
     *
     * @return The IPCClient's current {@link Status}.
     */
    public Status getStatus()
    {
        return status;
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
        LOGGER.debug("Closing IPC Pipe...");
        send(OpCode.CLOSE, new JSONObject(), null);
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
     * @return The {@link DiscordBuild} of this IPCClient.
     */
    public DiscordBuild getDiscordBuild()
    {
        return build;
    }
    
    
    // Enums
    
    /**
     * Constants representing various status that an {@link IPCClient} can have.
     */
    public enum Status
    {
        /**
         * Status for when the IPCClient has been created.<p>
         *
         * All IPCClients are created starting with this status,
         * and it never returns for the lifespan of the client.
         */
        CREATED,

        /**
         * Status for when the IPCClient is attempting to connect.<p>
         *
         * This will become set whenever the #connect() method is called.
         */
        CONNECTING,

        /**
         * Status for when the IPCClient is connected with Discord.<p>
         *
         * This is only present when the connection is healthy, stable,
         * and reading good data without exception.<br>
         * If the environment becomes out of line with these principles
         * in any way, the IPCClient in question will become
         * {@link Status#DISCONNECTED}.
         */
        CONNECTED,

        /**
         * Status for when the IPCClient has received an {@link OpCode#CLOSE}.<p>
         *
         * This signifies that the reading thread has safely and normally shut
         * and the client is now inactive.
         */
        CLOSED,

        /**
         * Status for when the IPCClient has unexpectedly disconnected, either because
         * of an exception, and/or due to bad data.<p>
         *
         * When the status of an IPCClient becomes this, a call to
         * {@link IPCListener#onDisconnect(IPCClient, Throwable)} will be made if one
         * has been provided to the IPCClient.<p>
         *
         * Note that the IPCClient will be inactive with this status, after which a
         * call to {@link #connect(DiscordBuild...)} can be made to "reconnect" the
         * IPCClient.
         */
        DISCONNECTED
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
        if(connected && status != Status.CONNECTED)
            throw new IllegalStateException(String.format("IPCClient (ID: %d) is not connected!", clientId));
        if(!connected && status == Status.CONNECTED)
            throw new IllegalStateException(String.format("IPCClient (ID: %d) is already connected!", clientId));
    }
    
    /**
     * Initializes this IPCClient's {@link IPCClient#readThread readThread}
     * and calls the first {@link #read()}.
     */
    private void startReading()
    {
        readThread = new Thread(() -> {
            try
            {
                Packet p;
                while((p = read()).getOp() != OpCode.CLOSE)
                {
                    JSONObject json = p.getJson();
                    Event event = Event.of(json.optString("evt", null));
                    String nonce = json.optString("nonce", null);
                    switch(event)
                    {
                        case NULL:
                            if(nonce != null && callbacks.containsKey(nonce))
                                callbacks.remove(nonce).succeed();
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
                status = Status.CLOSED;
                if(listener != null)
                    listener.onClose(this, p.getJson());
            }
            catch(IOException | JSONException ex)
            {
                if(ex instanceof IOException)
                    LOGGER.error("Reading thread encountered an IOException", ex);
                else
                    LOGGER.error("Reading thread encountered an JSONException", ex);

                status = Status.DISCONNECTED;
                if(listener != null)
                    listener.onDisconnect(this, ex);
            }
        });

        LOGGER.debug("Starting IPCClient reading thread!");
        readThread.start();
    }
    
    /**
     * Sends json with the given {@link OpCode}.
     *
     * @param op The {@link OpCode} to send data with.
     * @param data The data to send.
     * @param callback callback for the response
     */
    private void send(OpCode op, JSONObject data, Callback callback)
    {
        try
        {
            String nonce = generateNonce();
            Packet p = new Packet(op, data.put("nonce",nonce));
            if(callback!=null && !callback.isEmpty())
                callbacks.put(nonce, callback);
            pipe.write(p.toBytes());
            LOGGER.debug(String.format("Sent packet: %s", p.toString()));
            if(listener != null)
                listener.onPacketSent(this, p);
        }
        catch(IOException ex)
        {
            LOGGER.error("Encountered an IOException while sending a packet and disconnected!");
            status = Status.DISCONNECTED;
        }
    }
    
    /**
     * Blocks until reading a {@link Packet} or until the
     * read thread encounters bad data.
     *
     * @return A valid {@link Packet}.
     *
     * @throws IOException
     *         If the pipe breaks.
     * @throws JSONException
     *         If the read thread receives bad data.
     */
    private Packet read() throws IOException, JSONException
    {
        while(pipe.length() == 0 && status == Status.CONNECTED)
        {
            try {
                Thread.sleep(50);
            } catch(InterruptedException ignored) {}
        }

        if(status==Status.DISCONNECTED)
            throw new IOException("Disconnected!");

        OpCode op = OpCode.values()[Integer.reverseBytes(pipe.readInt())];
        int len = Integer.reverseBytes(pipe.readInt());
        byte[] d = new byte[len];

        pipe.readFully(d);
        Packet p = new Packet(op, new JSONObject(new String(d)));
        LOGGER.debug(String.format("Received packet: %s", p.toString()));
        if(listener != null)
            listener.onPacketReceived(this, p);
        return p;
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

    // a list of system property keys to get IPC file from different unix systems.
    private final static String[] paths = {"XDG_RUNTIME_DIR","TMPDIR","TMP","TEMP"};

    /**
     * Finds the IPC location in the current system.
     *
     * @param i Index to try getting the IPC at.
     *
     * @return The IPC location.
     */
    private static String getIPC(int i)
    {
        if(System.getProperty("os.name").contains("Win"))
            return "\\\\?\\pipe\\discord-ipc-"+i;
        String tmppath = null;
        for(String str : paths)
        {
            tmppath = System.getenv(str);
            if(tmppath != null)
                break;
        }
        if(tmppath == null)
            tmppath = "/tmp";
        return tmppath+"/discord-ipc-"+i;
    }
    
    /**
     * Generates a nonce.
     *
     * @return A random {@link UUID}.
     */
    private static String generateNonce()
    {
        return UUID.randomUUID().toString();
    }
}
