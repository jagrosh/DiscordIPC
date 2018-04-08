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
import com.jagrosh.discordipc.entities.Packet;
import org.json.JSONException;
import org.json.JSONObject;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class UnixPipe extends Pipe
{

    private static final Logger LOGGER = LoggerFactory.getLogger(UnixPipe.class);
    private final AFUNIXSocket socket;

    UnixPipe(IPCClient ipcClient, HashMap<String, Callback> callbacks, String location) throws IOException
    {
        super(ipcClient, callbacks);

        socket = AFUNIXSocket.newInstance();
        socket.connect(new AFUNIXSocketAddress(new File(location)));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public Packet read() throws IOException, JSONException
    {
        InputStream is = socket.getInputStream();

        while(is.available() == 0 && status == PipeStatus.CONNECTED)
        {
            try {
                Thread.sleep(50);
            } catch(InterruptedException ignored) {}
        }

        /*byte[] buf = new byte[is.available()];
        is.read(buf, 0, buf.length);
        LOGGER.info(new String(buf));

        if (true) return null;*/

        if(status==PipeStatus.DISCONNECTED)
            throw new IOException("Disconnected!");

        if(status==PipeStatus.CLOSED)
            return new Packet(Packet.OpCode.CLOSE, null);

        // Read the op and length. Both are signed ints
        byte[] d = new byte[8];
        is.read(d);
        ByteBuffer bb = ByteBuffer.wrap(d);

        Packet.OpCode op = Packet.OpCode.values()[Integer.reverseBytes(bb.getInt())];
        d = new byte[Integer.reverseBytes(bb.getInt())];

        is.read(d);
        Packet p = new Packet(op, new JSONObject(new String(d)));
        LOGGER.debug(String.format("Received packet: %s", p.toString()));
        if(listener != null)
            listener.onPacketReceived(ipcClient, p);
        return p;
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        socket.getOutputStream().write(b);
    }

    @Override
    public void close() throws IOException
    {
        LOGGER.debug("Closing IPC pipe...");
        send(Packet.OpCode.CLOSE, new JSONObject(), null);
        status = PipeStatus.CLOSED;
        socket.close();
    }
}
