# DiscordIPC

Connect locally to the Discord client using IPC for a subset of RPC features like Rich Presence and Activity Join/Spectate

*This project is a fork of [jagrosh's DiscordIPC](https://github.com/jagrosh/DiscordIPC) with some changes to provide support for M1 macs*

# Features

- Setting Rich Presence
- Listen for Join, Spectate, and Join-Request events
- Detect and specify priority for client build (Stable, PTB, Canary)
- 100% Java


# Getting Started

First you'll need to add this project as a dependency. If you're using maven:
```xml
<dependency>
  <groupId>com.github.cbyrneee</groupId>
  <artifactId>DiscordIPC</artifactId>
  <version>latest_commit_hash</version>
</dependency>
```
```xml
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>
```
With gradle:
```groovy
dependencies {
    compile 'com.github.cbyrneee:DiscordIPC:<latest_commit_hash>'
}

repositories {
    maven { url = "https://jitpack.io" }
}
```

# Example

Quick example, assuming you already have a GUI application
```java
IPCClient client = new IPCClient(345229890980937739L);
client.setListener(new IPCListener(){
    @Override
    public void onReady(IPCClient client)
    {
        RichPresence.Builder builder = new RichPresence.Builder();
        builder.setState("West of House")
            .setDetails("Frustration level: Over 9000")
            .setStartTimestamp(OffsetDateTime.now())
            .setLargeImage("canary-large", "Discord Canary")
            .setSmallImage("ptb-small", "Discord PTB")
            .setParty("party1234", 1, 6)
            .setMatchSecret("xyzzy")
            .setJoinSecret("join")
            .setSpectateSecret("look");
        client.sendRichPresence(builder.build());
    }
});
client.connect();
```
