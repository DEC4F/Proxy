import java.io.IOException;
import java.net.ServerSocket;

/**
 * created by Stanley Tian
 */
public class proxyd {

    /**
     * main method that creates a new DNS cache and a new　ProxyThread
     * @param args contains the port number the proxyd waits request at
     */
    public static void main(String[] args) {
        int port = 5042;
        DNSCache cache = new DNSCache();

        // read port number from program argument
        if (args.length > 0)
            port = Integer.parseInt(args[1]);

        try {
            ServerSocket server = new ServerSocket(port);
            System.out.printf("Wait request from port at %s\n", port);

            while (true)
                new Thread(new ProxyThread(server.accept(), cache)).start();

        } catch (IOException e1) {
            System.err.printf("Error: %s\n", e1);
        }
    }
}