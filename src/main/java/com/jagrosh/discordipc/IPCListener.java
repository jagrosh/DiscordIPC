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
import com.jagrosh.discordipc.entities.User;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public interface IPCListener
{
    public default void onPacketSent(IPCClient client, Packet packet) {}
    
    public default void onPacketReceived(IPCClient client, Packet packet) {}
    
    public default void onActivityJoin(IPCClient client, String secret) {}
    
    public default void onActivitySpectate(IPCClient client, String secret) {}
    
    public default void onActivityJoinRequest(IPCClient client, String secret, User user) {}
    
    public default void onReady(IPCClient client) {}
    
    public default void onClose(IPCClient client, JSONObject json) {}
    
    public default void onDisconnect(IPCClient client, Throwable t) {}
}
