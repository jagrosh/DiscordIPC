package com.jagrosh.discordipc.entities.pipe.listener;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.User;

import java.util.HashMap;

/**
 * A listener which is used to notify the IPCClient when a user is found
 */
public interface PipeCreationListener {
    /**
     * Fired in {@link com.jagrosh.discordipc.entities.pipe.Pipe#openPipe(IPCClient, PipeCreationListener, long, HashMap, DiscordBuild...)} once a user is found
     * @param user The client's user
     */
    void onUserFound(User user);
}
