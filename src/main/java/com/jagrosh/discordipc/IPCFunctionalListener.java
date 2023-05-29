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
import java.util.Objects;
import org.json.JSONObject;

/**
 * An {@link IPCListener} listener that uses functional interfaces {@link IPCClient}.<p>
 *
 * Can be attached to an IPCClient using {@link IPCClient#setListener(IPCListener)}.
 *
 * @author gudenau
 */
public class IPCFunctionalListener implements IPCListener{
    public PacketSentHandler packetSentHandler;
    public PacketReceivedHandler packetReceivedHandler;
    public ActivityJoinHandler activityJoinHandler;
    public ActivitySpectateHandler activitySpectateHandler;
    public ActivityJoinRequestHandler activityJoinRequestHandler;
    public ReadyHandler readyHandler;
    public CloseHandler closeHandler;
    public DisconnectHandler disconnectHandler;
    
    @Override
    public void onPacketSent(IPCClient client, Packet packet){
        if(Objects.nonNull(packetSentHandler)){
            packetSentHandler.onPacketSent(client, packet);
        }
    }
    
    @Override
    public void onPacketReceived(IPCClient client, Packet packet){
        if(Objects.nonNull(packetReceivedHandler)){
            packetReceivedHandler.onPacketReceived(client, packet);
        }
    }
    
    @Override
    public void onActivityJoin(IPCClient client, String secret){
        if(Objects.nonNull(activityJoinHandler)){
            activityJoinHandler.onActivityJoin(client, secret);
        }
    }
    
    @Override
    public void onActivitySpectate(IPCClient client, String secret){
        if(Objects.nonNull(activitySpectateHandler)){
            activitySpectateHandler.onActivitySpectate(client, secret);
        }
    }
    
    @Override
    public void onActivityJoinRequest(IPCClient client, String secret, User user){
        if(Objects.nonNull(activityJoinRequestHandler)){
            activityJoinRequestHandler.onActivityJoinRequest(client, secret, user);
        }
    }
    
    @Override
    public void onReady(IPCClient client){
        if(Objects.nonNull(readyHandler)){
            readyHandler.onReady(client);
        }
    }
    
    @Override
    public void onClose(IPCClient client, JSONObject json){
        if(Objects.nonNull(closeHandler)){
            closeHandler.onClose(client, json);
        }
    }
    
    @Override
    public void onDisconnect(IPCClient client, Throwable t){
        if(Objects.nonNull(disconnectHandler)){
            disconnectHandler.onDisconnect(client, t);
        }
    }
    
    /**
     * Fired whenever an {@link IPCClient} sends a {@link Packet} to Discord.
     */
    @FunctionalInterface
    public interface PacketSentHandler{
        /**
         * Fired whenever an {@link IPCClient} sends a {@link Packet} to Discord.
         *
         * @param client The IPCClient sending the Packet.
         * @param packet The Packet being sent.
         */
        void onPacketSent(IPCClient client, Packet packet);
    }
    
    /**
     * Fired whenever an {@link IPCClient} receives a {@link Packet} to Discord.
     */
    @FunctionalInterface
    public interface PacketReceivedHandler{
        /**
         * Fired whenever an {@link IPCClient} receives a {@link Packet} to Discord.
         *
         * @param client The IPCClient receiving the Packet.
         * @param packet The Packet being received.
         */
        void onPacketReceived(IPCClient client, Packet packet);
    }
    
    /**
     * Fired whenever a RichPresence activity informs us that
     * a user has clicked a "join" button.
     */
    @FunctionalInterface
    public interface ActivityJoinHandler{
        /**
         * Fired whenever a RichPresence activity informs us that
         * a user has clicked a "join" button.
         *
         * @param client The IPCClient receiving the event.
         * @param secret The secret of the event, determined by the implementation and specified by the user.
         */
        void onActivityJoin(IPCClient client, String secret);
    }
    
    /**
     * Fired whenever a RichPresence activity informs us that
     * a user has clicked a "spectate" button.
     */
    @FunctionalInterface
    public interface ActivitySpectateHandler{
        /**
         * Fired whenever a RichPresence activity informs us that
         * a user has clicked a "spectate" button.
         *
         * @param client The IPCClient receiving the event.
         * @param secret The secret of the event, determined by the implementation and specified by the user.
         */
        void onActivitySpectate(IPCClient client, String secret);
    }
    
    /**
     * Fired whenever a RichPresence activity informs us that
     * a user has clicked a "ask to join" button.<p>
     *
     * As opposed to {@link #onActivityJoin(IPCClient, String)},
     * this also provides packaged {@link User} data.
     */
    @FunctionalInterface
    public interface ActivityJoinRequestHandler{
        /**
         * Fired whenever a RichPresence activity informs us that
         * a user has clicked a "ask to join" button.<p>
         *
         * As opposed to {@link #onActivityJoin(IPCClient, String)},
         * this also provides packaged {@link User} data.
         *
         * @param client The IPCClient receiving the event.
         * @param secret The secret of the event, determined by the implementation and specified by the user.
         * @param user The user who clicked the clicked the event, containing data on the account.
         */
        void onActivityJoinRequest(IPCClient client, String secret, User user);
    }
    
    /**
     * Fired whenever an {@link IPCClient} is ready and connected to Discord.
     */
    @FunctionalInterface
    public interface ReadyHandler{
        /**
         * Fired whenever an {@link IPCClient} is ready and connected to Discord.
         *
         * @param client The now ready IPCClient.
         */
        void onReady(IPCClient client);
    }
    
    /**
     * Fired whenever an {@link IPCClient} has closed.
     */
    @FunctionalInterface
    public interface CloseHandler{
        /**
         * Fired whenever an {@link IPCClient} has closed.
         *
         * @param client The now closed IPCClient.
         * @param json A {@link JSONObject} with close data.
         */
        void onClose(IPCClient client, JSONObject json);
    }
    
    /**
     * Fired whenever an {@link IPCClient} has disconnected,
     * either due to bad data or an exception.
     */
    @FunctionalInterface
    public interface DisconnectHandler{
        /**
         * Fired whenever an {@link IPCClient} has disconnected,
         * either due to bad data or an exception.
         *
         * @param client The now closed IPCClient.
         * @param t A {@link Throwable} responsible for the disconnection.
         */
        void onDisconnect(IPCClient client, Throwable t);
    }
}
