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

package com.jagrosh.discordipc.entities.pipe;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import com.sun.javafx.PlatformUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public abstract class Pipe {

    private static final Logger LOGGER = LoggerFactory.getLogger(Pipe.class);
    static final int VERSION = 1;
    PipeStatus status = PipeStatus.CONNECTING;
    IPCListener listener;
    DiscordBuild build;
    final IPCClient ipcClient;
    final long clientId;
    final HashMap<String,Callback> callbacks;

    Pipe(IPCClient ipcClient, long clientId, HashMap<String, Callback> callbacks)
    {
        this.ipcClient = ipcClient;
        this.clientId = clientId;
        this.callbacks = callbacks;
    }

    public static Pipe openPipe(IPCClient ipcClient, long clientId, HashMap<String,Callback> callbacks,
                                DiscordBuild... preferredOrder) throws NoDiscordClientException
    {
        if (PlatformUtil.isWindows())
        {
            return WindowsPipe.openPipe(ipcClient, clientId, callbacks, preferredOrder);
        }
        else if (PlatformUtil.isUnix())
        {
            return null; //TODO
        }
        else
        {
            throw new RuntimeException("Unsupported OS: " + System.getProperty("os.name"));
        }
    }

    /**
     * Sends json with the given {@link Packet.OpCode}.
     *
     * @param op The {@link Packet.OpCode} to send data with.
     * @param data The data to send.
     * @param callback callback for the response
     */
    public void send(Packet.OpCode op, JSONObject data, Callback callback)
    {
        try
        {
            String nonce = generateNonce();
            Packet p = new Packet(op, data.put("nonce",nonce));
            if(callback!=null && !callback.isEmpty())
                callbacks.put(nonce, callback);
            write(p.toBytes());
            LOGGER.debug(String.format("Sent packet: %s", p.toString()));
            if(listener != null)
                listener.onPacketSent(ipcClient, p);
        }
        catch(IOException ex)
        {
            LOGGER.error("Encountered an IOException while sending a packet and disconnected!");
            status = PipeStatus.DISCONNECTED;
        }
    }

    public abstract void write(byte[] b) throws IOException;

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
    public abstract Packet read() throws IOException, JSONException;

    /**
     * Generates a nonce.
     *
     * @return A random {@link UUID}.
     */
    private static String generateNonce()
    {
        return UUID.randomUUID().toString();
    }

    public PipeStatus getStatus() {
        return status;
    }

    public void setStatus(PipeStatus status) {
        this.status = status;
    }

    public void setListener(IPCListener listener)
    {
        this.listener = listener;
    }

    public void close()
    {
        LOGGER.debug("Closing IPC pipe...");
        send(Packet.OpCode.CLOSE, new JSONObject(), null);
        status = PipeStatus.CLOSED;
    }

    public DiscordBuild getDiscordBuild()
    {
        return build;
    }
}
