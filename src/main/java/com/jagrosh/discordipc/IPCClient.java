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

import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.Packet.OpCode;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class IPCClient
{
    private final int version = 1;
    private final long clientId;
    private Status status = Status.CREATED;
    private ClientBuild build = null;
    private IPCListener listener = null;
    private RandomAccessFile pipe = null;
    private Thread readThread = null;
    
    /**
     * Constructor
     * @param clientId application's client ID
     */
    public IPCClient(long clientId)
    {
        this.clientId = clientId;
    }
    
    /**
     * Sets the IPCListener to listen for events
     * @param listener the listener
     */
    public final void setListener(IPCListener listener)
    {
        this.listener = listener;
    }
    
    /**
     * Connects to the Discord client
     * @param preferredOrder the priority order of client builds to connect to
     * @throws NoDiscordClientException no client of the provided type(s) were found
     */
    public final void connect(ClientBuild... preferredOrder) throws NoDiscordClientException
    {
        checkConnected(false);
        if(preferredOrder.length==0)
            preferredOrder = new ClientBuild[]{ClientBuild.ANY};
        // store some files so we can get the preferred client
        RandomAccessFile[] open = new RandomAccessFile[ClientBuild.values().length];
        for(int i=0; i<10; i++)
        {
            try
            {
                pipe = new RandomAccessFile(getIPC(i), "rw");
                send(OpCode.HANDSHAKE, new JSONObject().put("v",version).put("client_id", Long.toString(clientId)));
                Packet p = read(); // this is a valid client at this point
                build = ClientBuild.from(p.getJson().getJSONObject("data").getJSONObject("config").getString("api_endpoint"));
                if(build==preferredOrder[0] || ClientBuild.ANY==preferredOrder[0]) // we're done if we found our first choice
                    break;
                open[build.ordinal()] = pipe; // didn't find first choice yet, so store what we have
                open[0] = pipe; // also store in 'any' for use later
                build = null;
                pipe = null;
            }
            catch(IOException | JSONException ex)
            {
                pipe = null;
                build = null;
            }
        }
        if(pipe==null)
        {
            // we already know we dont have our first pick
            // check each of the rest to see if we have that
            for(int i=1; i<preferredOrder.length; i++)
            {
                ClientBuild cb = preferredOrder[i];
                if(open[cb.ordinal()]!=null)
                {
                    pipe = open[cb.ordinal()];
                    open[cb.ordinal()] = null;
                    if(cb==ClientBuild.ANY) // if we pulled this from the 'any' slot, we need to figure out which build it was
                    {
                        for(int k=1; k<open.length; k++)
                            if(open[k] == pipe)
                                build = ClientBuild.values()[k];
                    }
                    else build = cb;
                    break;
                }
            }
            if(pipe==null)
                throw new NoDiscordClientException();
        }
        // close unused files, except skip 'any' because its always a duplicate
        for (int i=1; i<open.length; i++)
        {
            if (open[i] != null)
                try {
                    open[i].close();
                } catch(IOException e){}
        }
        status = Status.CONNECTED;
        if(listener!=null)
            listener.onReady(this);
        startReading();
    }
    
    /**
     * Sends a rich presence to the Discord client
     * @param rp the presence to send
     */
    public final void sendRichPresence(RichPresence rp)
    {
        checkConnected(true);
        send(OpCode.FRAME, new JSONObject()
                            .put("cmd","SET_ACTIVITY")
                            .put("args", new JSONObject()
                                        .put("pid",getPID())
                                        .put("activity",rp.toJson())));
                                
    }
    
    /**
     * Attempts to close the connection
     */
    public final void close()
    {
        checkConnected(true);
        send(OpCode.CLOSE, new JSONObject());
    }
    
    /**
     * Subscribes to an event
     * @param sub the event to subscribe to
     */
    public final void subscribe(Subscription sub)
    {
        checkConnected(true);
        send(OpCode.FRAME, new JSONObject()
                            .put("cmd", "SUBSCRIBE")
                            .put("evt", sub.name()));
    }
    
    public final Status getStatus()
    {
        return status;
    }
    
    public final ClientBuild getClientBuild()
    {
        return build;
    }
    
    
    // Enums
    
    /**
     * Client status
     */
    public enum Status
    {
        CREATED, CONNECTED, CLOSED, DISCONNECTED
    }
    
    /**
     * Discord builds
     */
    public static enum ClientBuild
    {
        ANY, STABLE, PTB, CANARY;
        
        public static ClientBuild from(String endpoint)
        {
            switch(endpoint)
            {
                case "//canary.discordapp.com/api": return CANARY;
                case "//ptb.discordapp.com/api": return PTB;
                case "//discordapp.com/api": return STABLE;
                default: return ANY;
            }
        }
    }
    
    /**
     * Available subscriptions
     */
    public static enum Subscription
    {
        ACTIVITY_JOIN,
        ACTIVITY_SPECTATE,
        ACTIVITY_JOIN_REQUEST;
        
        public static Subscription of(String str)
        {
            for(Subscription s: Subscription.values())
                if(s.name().equalsIgnoreCase(str))
                    return s;
            return null;
        }
    }
    
    
    // Private methods
    
    /**
     * Makes sure that the client is connected (or not) depending on if it should
     * for the current state
     * @param connected 
     */
    private void checkConnected(boolean connected)
    {
        if(connected && status != Status.CONNECTED)
            throw new IllegalStateException("Not connected!");
        if(!connected && status == Status.CONNECTED)
            throw new IllegalStateException("Already connected!");
    }
    
    /**
     * Starts a reading thread, which automatically ends when disconnected or closed
     */
    private void startReading()
    {
        readThread = new Thread(() ->
        {
            try
            {
                Packet p;
                while((p = read()).getOp() != OpCode.CLOSE)
                {
                    if(listener!=null && p.getJson().has("cmd") && p.getJson().getString("cmd").equals("DISPATCH"))
                    {
                        switch(Subscription.of(p.getJson().getString("evt")))
                        {
                            case ACTIVITY_JOIN:
                                listener.onActivityJoin(this, p.getJson().getJSONObject("data").getString("secret"));
                                break;
                            case ACTIVITY_SPECTATE:
                                listener.onActivitySpectate(this, p.getJson().getJSONObject("data").getString("secret"));
                                break;
                            case ACTIVITY_JOIN_REQUEST:
                                JSONObject u = p.getJson().getJSONObject("data").getJSONObject("user");
                                listener.onActivityJoinRequest(this, p.getJson().getJSONObject("data").has("secret") ? p.getJson().getJSONObject("data").getString("secret") : null, 
                                        new User(u.getString("username"), u.getString("discriminator"), Long.parseLong(u.getString("id")), u.isNull("avatar") ? null : u.getString("avatar")));
                                break;
                        }
                    }
                }
                if(listener!=null)
                {
                    status = Status.CLOSED;
                    listener.onClose(this, p.getJson());
                }
            }
            catch(IOException | JSONException ex)
            {
                if(listener!=null)
                {
                    status = Status.DISCONNECTED;
                    listener.onDisconnect(this, ex);
                }
            }
        });
        readThread.start();
    }
    
    /**
     * Sends json with the given op
     * @param op
     * @param data
     * @throws IOException 
     */
    private void send(OpCode op, JSONObject data)
    {
        try
        {
            Packet p = new Packet(op, data.put("nonce",generateNonce()));
            pipe.write(p.toBytes());
            if(listener!=null)
                listener.onPacketSent(this, p);
        }
        catch(IOException ex)
        {
            status = Status.DISCONNECTED;
        }
    }
    
    /**
     * Blocks until reading a packet or bad data
     * @return a valid packet
     * @throws IOException pipe breaks
     * @throws JSONException bad data
     */
    private Packet read() throws IOException, JSONException
    {
        while(pipe.length()==0 && status==Status.CONNECTED) try{Thread.sleep(50);}catch(InterruptedException e){}
        if(status==Status.DISCONNECTED)
            throw new IOException("Disconnected!");
        OpCode op = OpCode.values()[Integer.reverseBytes(pipe.readInt())];
        int len = Integer.reverseBytes(pipe.readInt());
        byte[] d = new byte[len];
        pipe.readFully(d);
        Packet p = new Packet(op, new JSONObject(new String(d)));
        if(listener!=null)
            listener.onPacketReceived(this, p);
        return p;
    }
    
    // Private static methods
    
    /**
     * Finds the current process ID
     * @return the process ID
     */
    private static int getPID()
    {
        String pr = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(pr.substring(0,pr.indexOf('@')));
    }
    
    private final static String[] paths = {"XDG_RUNTIME_DIR","TMPDIR","TMP","TEMP"};
    /**
     * Finds the IPC location
     * @param i index to try
     * @return the IPC location
     */
    private static String getIPC(int i)
    {
        if(System.getProperty("os.name").contains("Win"))
            return "\\\\?\\pipe\\discord-ipc-"+i;
        String tmppath = null;
        for(String str: paths)
        {
            tmppath = System.getenv(str);
            if(tmppath!=null)
                break;
        }
        if(tmppath==null)
            tmppath = "/tmp";
        return tmppath+"/discord-ipc-"+i;
    }
    
    /**
     * Generates a nonce
     * @return a unique string
     */
    private static String generateNonce()
    {
        return UUID.randomUUID().toString();
    }
}
