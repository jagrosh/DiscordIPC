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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class MacPipe extends UnixPipe {

    MacPipe(IPCClient ipcClient, HashMap<String, Callback> callbacks, File location) throws IOException {
        super(ipcClient, callbacks, location);
    }

    private void registerCommand(String applicationId, String command) {
        String home = System.getenv("HOME");
        if (home == null)
            throw new RuntimeException("Unable to find user HOME directory");

        String path = home + "/Library/Application Support/discord";

        if (!this.mkdir(path))
            throw new RuntimeException("Failed to create directory '" + path + "'");

        path += "/games";

        if (!this.mkdir(path))
            throw new RuntimeException("Failed to create directory '" + path + "'");

        path += "/" + applicationId + ".json";

        try (FileWriter fileWriter = new FileWriter(path)) {
            fileWriter.write("{\"command\": \"" + command + "\"}");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write fame info into '" + path + "'");
        }
    }

    private void registerUrl(String applicationId) {
        throw new UnsupportedOperationException("MacOS URL registration is not supported at this time.");
    }

    @Override
    public void registerApp(String applicationId, String command) {
        try {
            if (command != null)
                this.registerCommand(applicationId, command);
            else
                this.registerUrl(applicationId);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to register " + (command == null ? "url" : "command"), ex);
        }
    }

    @Override
    public void registerSteamGame(String applicationId, String steamId) {
        this.registerApp(applicationId, "steam://rungameid/" + steamId);
    }
}
