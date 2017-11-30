# DiscordIPC

Connect locally to the Discord client using IPC for a subset of RPC features like Rich Presence and Activity Join/Spectate


# Features

- Setting Rich Presence
- Listen for Join, Spectate, and Join-Request events
- Detect and specify priority for client build (Stable, PTB, Canary)
- 100% Java


# Example

Quick example, assuming you already have a GUI application
```java
IPCClient client = new IPCClient(345229890980937739L);
client.setListener(new IPCListener(){
    @Override
    public void onReady(IPCClient client)
    {
        client.sendRichPresence(new RichPresence("West of House", "Frustration level: Over 9000",
                OffsetDateTime.now(), null, "canary-large", null, "ptb-small", null, "party1234",
                1, 6, "xyzzy", "join", "look", false));
    }
});
client.connect();
```
Additional examples will be available soon.


# Official Discord-RPC Bindings

The official RPC bindings can be found here: https://github.com/discordapp/discord-rpc

A Java wrapper for the official bindings is available here: https://github.com/MinnDevelopment/Java-DiscordRPC