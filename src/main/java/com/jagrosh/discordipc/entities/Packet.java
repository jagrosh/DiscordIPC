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
package com.jagrosh.discordipc.entities;

import java.nio.ByteBuffer;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Packet
{
    private final OpCode op;
    private final JSONObject data;
    
    public Packet(OpCode op, JSONObject data)
    {
        this.op = op;
        this.data = data;
    }
    
    public byte[] toBytes()
    {
        byte[] d = data.toString().getBytes();
        ByteBuffer packet = ByteBuffer.allocate(d.length + 2*Integer.BYTES);
        packet.putInt(Integer.reverseBytes(op.ordinal()));
        packet.putInt(Integer.reverseBytes(d.length));
        packet.put(d);
        return packet.array();
    }
    
    public OpCode getOp()
    {
        return op;
    }
    
    public JSONObject getJson()
    {
        return data;
    }
    
    @Override
    public String toString()
    {
        return "Pkt:"+getOp()+getJson().toString();
    }
    
    public static enum OpCode
    {
        HANDSHAKE, FRAME, CLOSE, PING, PONG
    }
}
