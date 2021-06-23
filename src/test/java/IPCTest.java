import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;

public class IPCTest {
    public static void main(String... args) {
        IPCClient client = new IPCClient(857191688454537226L);
        client.setListener(new IPCListener() {
            @Override
            public void onReady(IPCClient client, User user) {
                System.out.println("Ready! User: " + user);
                client.sendRichPresence(new RichPresence.Builder().setDetails("DiscordIPC library test").setState("Hello world!").build());
            }
        });

        try {
            client.connect();
        } catch (NoDiscordClientException e) {
            e.printStackTrace();
        }
    }
}
