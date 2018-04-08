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
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.Packet;

/**
 * Constants representing various status that an {@link IPCClient} can have.
 */
public enum PipeStatus
{
    /**
     * Status for when the IPCClient when no attempt to connect has been made.<p>
     *
     * All IPCClients are created starting with this status,
     * and it never returns for the lifespan of the client.
     */
    UNINITIALIZED,

    /**
     * Status for when the Pipe is attempting to connect.<p>
     *
     * This will become set whenever the #connect() method is called.
     */
    CONNECTING,

    /**
     * Status for when the Pipe is connected with Discord.<p>
     *
     * This is only present when the connection is healthy, stable,
     * and reading good data without exception.<br>
     * If the environment becomes out of line with these principles
     * in any way, the IPCClient in question will become
     * {@link PipeStatus#DISCONNECTED}.
     */
    CONNECTED,

    /**
     * Status for when the Pipe has received an {@link Packet.OpCode#CLOSE}.<p>
     *
     * This signifies that the reading thread has safely and normally shut
     * and the client is now inactive.
     */
    CLOSED,

    /**
     * Status for when the Pipe has unexpectedly disconnected, either because
     * of an exception, and/or due to bad data.<p>
     *
     * When the status of an Pipe becomes this, a call to
     * {@link IPCListener#onDisconnect(IPCClient, Throwable)} will be made if one
     * has been provided to the IPCClient.<p>
     *
     * Note that the IPCClient will be inactive with this status, after which a
     * call to {@link IPCClient#connect(DiscordBuild...)} can be made to "reconnect" the
     * IPCClient.
     */
    DISCONNECTED
}