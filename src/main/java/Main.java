import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;

import java.time.OffsetDateTime;

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
                        .setStartTimestamp(OffsetDateTime.now().toEpochSecond())
                        .setLargeImage("success", "Test Successful");
                client.sendRichPresence(builder.build());
            }
        });
        client.connect();
        System.out.println("Check Discord...");
    }
}
