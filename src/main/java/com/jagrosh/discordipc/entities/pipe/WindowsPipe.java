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
import com.jagrosh.discordipc.entities.Packet;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WindowsPipe extends Pipe {

    private final RandomAccessFile file;

    WindowsPipe(IPCClient ipcClient, String location) {
        super(ipcClient);
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
        while (file.length() == 0 && status == PipeStatus.CONNECTED) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }

        if (status == PipeStatus.DISCONNECTED)
            throw new IOException("Disconnected!");

        if (status == PipeStatus.CLOSED)
            return new Packet(Packet.OpCode.CLOSE, null);

        Packet.OpCode op = Packet.OpCode.values()[Integer.reverseBytes(file.readInt())];
        int len = Integer.reverseBytes(file.readInt());
        byte[] d = new byte[len];

        file.readFully(d);
        Packet p = new Packet(op, new JSONObject(new String(d)));
        if (listener != null)
            listener.onPacketReceived(ipcClient, p);
        return p;
    }

    @Override
    public void close() throws IOException {
        send(Packet.OpCode.CLOSE, new JSONObject());
        status = PipeStatus.CLOSED;
        file.close();
    }

}
