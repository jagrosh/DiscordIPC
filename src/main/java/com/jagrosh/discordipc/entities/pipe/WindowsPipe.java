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
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

public class WindowsPipe extends Pipe
{

    private static final Logger LOGGER = LoggerFactory.getLogger(WindowsPipe.class);

    private final RandomAccessFile file;

    public static Pipe openPipe(IPCClient ipcClient, long clientId, HashMap<String, Callback> callbacks, DiscordBuild... preferredOrder) throws NoDiscordClientException {
        if(preferredOrder == null || preferredOrder.length == 0)
            preferredOrder = new DiscordBuild[]{DiscordBuild.ANY};

        WindowsPipe pipe = null;

        // store some files so we can get the preferred client
        WindowsPipe[] open = new WindowsPipe[DiscordBuild.values().length];
        for(int i = 0; i < 10; i++)
        {
            try
            {
                String location = getIPC(i);
                LOGGER.debug(String.format("Searching for IPC: %s", location));
                pipe = new WindowsPipe(ipcClient, clientId, callbacks, location);

                pipe.send(Packet.OpCode.HANDSHAKE, new JSONObject().put("v", VERSION).put("client_id", Long.toString(clientId)), null);

                Packet p = pipe.read(); // this is a valid client at this point

                pipe.build = DiscordBuild.from(p.getJson().getJSONObject("data")
                        .getJSONObject("config")
                        .getString("api_endpoint"));

                LOGGER.debug(String.format("Found a valid client (%s) with packet: %s", pipe.build.name(), p.toString()));
                // we're done if we found our first choice
                if(pipe.build == preferredOrder[0] || DiscordBuild.ANY == preferredOrder[0]) {
                    LOGGER.info(String.format("Found preferred client: %s", pipe.build.name()));
                    break;
                }

                open[pipe.build.ordinal()] = pipe; // didn't find first choice yet, so store what we have
                open[DiscordBuild.ANY.ordinal()] = pipe; // also store in 'any' for use later

                pipe.build = null;
                pipe = null;
            }
            catch(IOException | JSONException ex)
            {
                pipe = null;
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
                                pipe.build = DiscordBuild.values()[k];
                                open[k] = null; // we don't want to close this
                            }
                        }
                    }
                    else pipe.build = cb;

                    LOGGER.info(String.format("Found preferred client: %s", pipe.build.name()));
                    break;
                }
            }
            if(pipe == null)
            {
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
                    open[i].file.close();
                } catch(IOException ex) {
                    // This isn't really important to applications and better
                    // as debug info
                    LOGGER.debug("Failed to close an open IPC pipe!", ex);
                }
            }
        }

        pipe.status = PipeStatus.CONNECTED;

        return pipe;
    }

    private WindowsPipe(IPCClient ipcClient, long clientId, HashMap<String, Callback> callbacks, String location)
    {
        super(ipcClient, clientId, callbacks);
        try {
            this.file = new RandomAccessFile(location, "rw");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        file.write(b);
    }

    @Override
    public Packet read() throws IOException, JSONException {
        while(file.length() == 0 && status == PipeStatus.CONNECTED)
        {
            try {
                Thread.sleep(50);
            } catch(InterruptedException ignored) {}
        }

        if(status==PipeStatus.DISCONNECTED)
            throw new IOException("Disconnected!");

        if(status==PipeStatus.CLOSED)
            return new Packet(Packet.OpCode.CLOSE, null);

        Packet.OpCode op = Packet.OpCode.values()[Integer.reverseBytes(file.readInt())];
        int len = Integer.reverseBytes(file.readInt());
        byte[] d = new byte[len];

        file.readFully(d);
        Packet p = new Packet(op, new JSONObject(new String(d)));
        LOGGER.debug(String.format("Received packet: %s", p.toString()));
        if(listener != null)
            listener.onPacketReceived(ipcClient, p);
        return p;
    }

    /**
     * Finds the IPC location in the current system.
     *
     * @param i Index to try getting the IPC at.
     *
     * @return The IPC location.
     */
    private static String getIPC(int i)
    {
        return "\\\\?\\pipe\\discord-ipc-"+i;
    }

}
