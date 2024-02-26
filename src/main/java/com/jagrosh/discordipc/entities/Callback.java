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

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.impl.DataConsumer;

/**
 * A callback for asynchronous logic when dealing processes that
 * would normally block the calling thread.
 * <p>
 * This is most visibly implemented in {@link IPCClient IPCClient}.
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Callback {
    private final DataConsumer<Packet> success;
    private final DataConsumer<String> failure;

    /**
     * Constructs an empty Callback.
     */
    public Callback() {
        this(null, null);
    }

    /**
     * Constructs a Callback with a success {@link DataConsumer} that
     * occurs when the process it is attached to executes without
     * error.
     *
     * @param success The Consumer to launch after a successful process.
     */
    public Callback(DataConsumer<Packet> success) {
        this(success, null);
    }

    /**
     * Constructs a Callback with a success {@link DataConsumer} <i>and</i>
     * a failure {@link DataConsumer} that occurs when the process it is
     * attached to executes without or with error (respectively).
     *
     * @param success The Consumer to launch after a successful process.
     * @param failure The Consumer to launch if the process has an error.
     */
    public Callback(DataConsumer<Packet> success, DataConsumer<String> failure) {
        this.success = success;
        this.failure = failure;
    }

    /**
     * Gets whether or not this Callback is "empty" which is more precisely
     * defined as not having a specified success {@link DataConsumer} and/or a
     * failure {@link DataConsumer}.<br>
     * This is only true if the Callback is constructed with the parameter-less
     * constructor ({@link #Callback()}) or another constructor that leaves
     * one or both parameters {@code null}.
     *
     * @return {@code true} if and only if the Callback is "empty"
     */
    public boolean isEmpty() {
        return success == null && failure == null;
    }

    /**
     * Launches the success {@link DataConsumer}.
     *
     * @param packet The packet to execute after success
     */
    public void succeed(Packet packet) {
        if (success != null)
            success.accept(packet);
    }

    /**
     * Launches the failure {@link DataConsumer} with the
     * provided message.
     *
     * @param message The message to launch the failure consumer with.
     */
    public void fail(String message) {
        if (failure != null)
            failure.accept(message);
    }
}
