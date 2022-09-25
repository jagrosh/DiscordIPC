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
import com.jagrosh.discordipc.impl.WinRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

public class WindowsPipe extends Pipe {
    private static final Logger LOGGER = LoggerFactory.getLogger(WindowsPipe.class);
    private static final Float javaSpec = Float.parseFloat(System.getProperty("java.specification.version"));
    private final int targetKey = WinRegistry.HKEY_CURRENT_USER;
    private final long targetLongKey = targetKey;
    public RandomAccessFile file;

    WindowsPipe(IPCClient ipcClient, HashMap<String, Callback> callbacks, File location) {
        super(ipcClient, callbacks);
        try {
            this.file = new RandomAccessFile(location, "rw");
        } catch (FileNotFoundException e) {
            this.file = null;
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        file.write(b);
    }

    @Override
    @SuppressWarnings("BusyWait")
    public Packet read() throws IOException, JsonParseException {
        while ((status == PipeStatus.CONNECTED || status == PipeStatus.CLOSING) && file.length() == 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }

        if (status == PipeStatus.DISCONNECTED)
            throw new IOException("Disconnected!");

        if (status == PipeStatus.CLOSED)
            return new Packet(Packet.OpCode.CLOSE, null, ipcClient.getEncoding());

        Packet.OpCode op = Packet.OpCode.values()[Integer.reverseBytes(file.readInt())];
        int len = Integer.reverseBytes(file.readInt());
        byte[] d = new byte[len];

        file.readFully(d);

        return receive(op, d);
    }

    @Override
    public void close() throws IOException {
        if (ipcClient.isDebugMode()) {
            LOGGER.info("[DEBUG] Closing IPC pipe...");
        }

        status = PipeStatus.CLOSING;
        send(Packet.OpCode.CLOSE, new JsonObject());
        status = PipeStatus.CLOSED;
        file.close();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void registerApp(String applicationId, String command) {
        String javaLibraryPath = System.getProperty("java.home");
        File javaExeFile = new File(javaLibraryPath.split(";")[0] + "/bin/java.exe");
        File javawExeFile = new File(javaLibraryPath.split(";")[0] + "/bin/javaw.exe");
        String javaExePath = javaExeFile.exists() ? javaExeFile.getAbsolutePath() : javawExeFile.exists() ? javawExeFile.getAbsolutePath() : null;

        if (javaExePath == null)
            throw new RuntimeException("Unable to find java path");

        String openCommand;

        if (command != null)
            openCommand = command;
        else
            openCommand = javaExePath;

        String protocolName = "discord-" + applicationId;
        String protocolDescription = "URL:Run game " + applicationId + " protocol";
        String keyName = "Software\\Classes\\" + protocolName;
        String iconKeyName = keyName + "\\DefaultIcon";
        String commandKeyName = keyName + "\\DefaultIcon";

        try {
            if (javaSpec >= 11) {
                WinRegistry.createKey(targetLongKey, keyName);
                WinRegistry.writeStringValue(targetLongKey, keyName, "", protocolDescription);
                WinRegistry.writeStringValue(targetLongKey, keyName, "URL Protocol", "\0");

                WinRegistry.createKey(targetLongKey, iconKeyName);
                WinRegistry.writeStringValue(targetLongKey, iconKeyName, "", javaExePath);

                WinRegistry.createKey(targetLongKey, commandKeyName);
                WinRegistry.writeStringValue(targetLongKey, commandKeyName, "", openCommand);
            } else {
                WinRegistry.createKey(targetKey, keyName);
                WinRegistry.writeStringValue(targetKey, keyName, "", protocolDescription);
                WinRegistry.writeStringValue(targetKey, keyName, "URL Protocol", "\0");

                WinRegistry.createKey(targetKey, iconKeyName);
                WinRegistry.writeStringValue(targetKey, iconKeyName, "", javaExePath);

                WinRegistry.createKey(targetKey, commandKeyName);
                WinRegistry.writeStringValue(targetKey, commandKeyName, "", openCommand);
            }
        } catch (Exception | Error ex) {
            throw new RuntimeException("Unable to modify Discord registry keys", ex);
        }
    }

    @Override
    public void registerSteamGame(String applicationId, String steamId) {
        try {
            String steamPath;
            if (javaSpec >= 11) {
                steamPath = WinRegistry.readString(targetLongKey, "Software\\\\Valve\\\\Steam", "SteamExe");
            } else {
                steamPath = WinRegistry.readString(targetKey, "Software\\\\Valve\\\\Steam", "SteamExe");
            }
            if (steamPath == null)
                throw new RuntimeException("Steam exe path not found");

            steamPath = steamPath.replaceAll("/", "\\");

            String command = "\"" + steamPath + "\" steam://rungameid/" + steamId;

            this.registerApp(applicationId, command);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to register Steam game", ex);
        }
    }

}
