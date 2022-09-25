import com.google.gson.JsonObject;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;

@SuppressWarnings("resource")
public class Main {

    public static void main(String[] args) throws NoDiscordClientException {
        System.out.println("Booting RPC...");
        IPCClient client = new IPCClient(659083077968199710L, true);
        client.setListener(new IPCListener() {
            @Override
            public void onReady(IPCClient client) {
                RichPresence.Builder builder = new RichPresence.Builder();
                builder.setState("Testing RPC Data...")
                        .setDetails("$DETAILS_HERE")
                        .setStartTimestamp(System.currentTimeMillis() / 1000L)
                        .setLargeImage("success", "Test Successful");
                client.sendRichPresence(builder.build());
            }

            @Override
            public void onPacketSent(IPCClient client, Packet packet) {
                // N/A
            }

            @Override
            public void onPacketReceived(IPCClient client, Packet packet) {
                // N/A
            }

            @Override
            public void onActivityJoin(IPCClient client, String secret) {
                // N/A
            }

            @Override
            public void onActivitySpectate(IPCClient client, String secret) {
                // N/A
            }

            @Override
            public void onActivityJoinRequest(IPCClient client, String secret, User user) {
                // N/A
            }

            @Override
            public void onClose(IPCClient client, JsonObject json) {
                // N/A
            }

            @Override
            public void onDisconnect(IPCClient client, Throwable t) {
                // N/A
            }
        });
        client.connect();
        System.out.println("Check Discord...");
    }
}
