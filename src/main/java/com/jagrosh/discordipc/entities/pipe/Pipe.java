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
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.entities.pipe.listener.PipeCreationListener;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public abstract class Pipe {

    private static final int VERSION = 1;
    // a list of system property keys to get IPC file from different unix systems.
    private final static String[] unixPaths = {"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"};
    final IPCClient ipcClient;
    private final HashMap<String, Callback> callbacks;
    PipeStatus status = PipeStatus.CONNECTING;
    IPCListener listener;
    private DiscordBuild build;

    Pipe(IPCClient ipcClient, HashMap<String, Callback> callbacks) {
        this.ipcClient = ipcClient;
        this.callbacks = callbacks;
    }

    public static Pipe openPipe(IPCClient ipcClient, PipeCreationListener pipeCreationListener, long clientId, HashMap<String, Callback> callbacks,
                                DiscordBuild... preferredOrder) throws NoDiscordClientException {

        if (preferredOrder == null || preferredOrder.length == 0)
            preferredOrder = new DiscordBuild[]{DiscordBuild.ANY};

        Pipe pipe = null;

        // store some files so we can get the preferred client
        Pipe[] open = new Pipe[DiscordBuild.values().length];
        for (int i = 0; i < 10; i++) {
            try {
                String location = getPipeLocation(i);
                pipe = createPipe(ipcClient, callbacks, location);

                pipe.send(Packet.OpCode.HANDSHAKE, new JSONObject().put("v", VERSION).put("client_id", Long.toString(clientId)), null);

                Packet p = pipe.read();

                JSONObject data = p.getJson().getJSONObject("data");
                JSONObject userObject = data.getJSONObject("user");

                pipe.build = DiscordBuild.from(data.getJSONObject("config").getString("api_endpoint"));
                if (pipe.build == preferredOrder[0] || DiscordBuild.ANY == preferredOrder[0]) {
                    // Notify the client that we have found a user
                    pipeCreationListener.onUserFound(new User(
                        userObject.getString("username"),
                        userObject.getString("discriminator"),
                        Long.parseLong(userObject.getString("id")),
                        userObject.optString("avatar", null)
                    ));

                    break;
                }

                open[pipe.build.ordinal()] = pipe; // didn't find first choice yet, so store what we have
                open[DiscordBuild.ANY.ordinal()] = pipe; // also store in 'any' for use later

                pipe.build = null;
                pipe = null;
            } catch (IOException | JSONException ex) {
                pipe = null;
            }
        }

        if (pipe == null) {
            // we already know we don't have our first pick
            // check each of the rest to see if we have that
            for (int i = 1; i < preferredOrder.length; i++) {
                DiscordBuild cb = preferredOrder[i];
                if (open[cb.ordinal()] != null) {
                    pipe = open[cb.ordinal()];
                    open[cb.ordinal()] = null;
                    if (cb == DiscordBuild.ANY) // if we pulled this from the 'any' slot, we need to figure out which build it was
                    {
                        for (int k = 0; k < open.length; k++) {
                            if (open[k] == pipe) {
                                pipe.build = DiscordBuild.values()[k];
                                open[k] = null; // we don't want to close this
                            }
                        }
                    } else pipe.build = cb;

                    break;
                }
            }
            if (pipe == null) {
                throw new NoDiscordClientException();
            }
        }
        // close unused files, except skip 'any' because its always a duplicate
        for (int i = 0; i < open.length; i++) {
            if (i == DiscordBuild.ANY.ordinal())
                continue;
            if (open[i] != null) {
                try {
                    open[i].close();
                } catch (IOException ex) {
                    // This isn't really important to applications and better
                    // as debug info
                    ex.printStackTrace();
                }
            }
        }

        pipe.status = PipeStatus.CONNECTED;

        return pipe;
    }

    private static Pipe createPipe(IPCClient ipcClient, HashMap<String, Callback> callbacks, String location) {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return new WindowsPipe(ipcClient, callbacks, location);
        } else if (osName.contains("linux") || osName.contains("mac")) {
            try {
                return new UnixPipe(ipcClient, callbacks, location);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Unsupported OS: " + osName);
        }
    }

    /**
     * Generates a nonce.
     *
     * @return A random {@link UUID}.
     */
    private static String generateNonce() {
        return UUID.randomUUID().toString();
    }

    /**
     * Finds the IPC location in the current system.
     *
     * @param i Index to try getting the IPC at.
     * @return The IPC location.
     */
    private static String getPipeLocation(int i) {
        if (System.getProperty("os.name").contains("Win"))
            return "\\\\?\\pipe\\discord-ipc-" + i;
        String tmppath = null;
        for (String str : unixPaths) {
            tmppath = System.getenv(str);
            if (tmppath != null)
                break;
        }
        if (tmppath == null)
            tmppath = "/tmp";
        return tmppath + "/discord-ipc-" + i;
    }

    /**
     * Sends json with the given {@link Packet.OpCode}.
     *
     * @param op       The {@link Packet.OpCode} to send data with.
     * @param data     The data to send.
     * @param callback callback for the response
     */
    public void send(Packet.OpCode op, JSONObject data, Callback callback) {
        try {
            String nonce = generateNonce();
            Packet p = new Packet(op, data.put("nonce", nonce));
            if (callback != null && !callback.isEmpty())
                callbacks.put(nonce, callback);
            write(p.toBytes());
            if (listener != null)
                listener.onPacketSent(ipcClient, p);
        } catch (IOException ex) {
            status = PipeStatus.DISCONNECTED;
        }
    }

    /**
     * Blocks until reading a {@link Packet} or until the
     * read thread encounters bad data.
     *
     * @return A valid {@link Packet}.
     * @throws IOException   If the pipe breaks.
     * @throws JSONException If the read thread receives bad data.
     */
    public abstract Packet read() throws IOException, JSONException;

    public abstract void write(byte[] b) throws IOException;

    public PipeStatus getStatus() {
        return status;
    }

    public void setStatus(PipeStatus status) {
        this.status = status;
    }

    public void setListener(IPCListener listener) {
        this.listener = listener;
    }

    public abstract void close() throws IOException;

    public DiscordBuild getDiscordBuild() {
        return build;
    }
}
