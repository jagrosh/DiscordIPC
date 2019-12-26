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

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.Callback;
import com.jagrosh.discordipc.entities.Packet;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class UnixPipe extends Pipe {
    private final AFUNIXSocket socket;

    UnixPipe(IPCClient ipcClient, HashMap<String, Callback> callbacks, String location) throws IOException {
        super(ipcClient, callbacks);

        socket = AFUNIXSocket.newInstance();
        socket.connect(new AFUNIXSocketAddress(new File(location)));
    }

    @Override
    public Packet read() throws IOException, JsonParseException {
        InputStream is = socket.getInputStream();

        while ((status == PipeStatus.CONNECTED || status == PipeStatus.CLOSING) && is.available() == 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }

        if (status == PipeStatus.DISCONNECTED)
            throw new IOException("Disconnected!");

        if (status == PipeStatus.CLOSED)
            return new Packet(Packet.OpCode.CLOSE, null, ipcClient.getEncoding());

        // Read the op and length. Both are signed ints
        byte[] d = new byte[8];
        int readResult = is.read(d);
        ByteBuffer bb = ByteBuffer.wrap(d);

        if (ipcClient.isDebugMode()) {
            System.out.println(String.format("Read Byte Data: %s with result %s", new String(d), readResult));
        }

        Packet.OpCode op = Packet.OpCode.values()[Integer.reverseBytes(bb.getInt())];
        d = new byte[Integer.reverseBytes(bb.getInt())];

        int reversedResult = is.read(d);

        if (ipcClient.isDebugMode()) {
            System.out.println(String.format("Read Reversed Byte Data: %s with result %s", new String(d), reversedResult));
        }

        JsonObject packetData = new JsonObject();
        packetData.addProperty("", new String(d));
        Packet p = new Packet(op, packetData, ipcClient.getEncoding());

        if (ipcClient.isDebugMode()) {
            System.out.println(String.format("Received packet: %s", p.toString()));
        }

        if (listener != null)
            listener.onPacketReceived(ipcClient, p);
        return p;
    }

    @Override
    public void write(byte[] b) throws IOException {
        socket.getOutputStream().write(b);
    }

    @Override
    public void close() throws IOException {
        if (ipcClient.isDebugMode()) {
            System.out.println("Closing IPC pipe...");
        }

        status = PipeStatus.CLOSING;
        send(Packet.OpCode.CLOSE, new JsonObject(), null);
        status = PipeStatus.CLOSED;
        socket.close();
    }
}
