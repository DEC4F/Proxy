import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * created by Stanley Tian
 */
public class ProxyThread implements Runnable {

    private final int bufferSize = 65536; // 64kB
    private Socket server;
    private Socket client;
    private DNSCache cache;

    // the type of the method
    private String methodType;
    // the uri contained in the header
    private String uri;
    // the protocol version
    private String version;
    // the header of the packet
    private List<String> requestHeaders;
    // the message of the packet
    private byte[] msg;
    // the length of the message
    private int contentLength;
    // the host
    private String host;
    // port number, default to 80
    private int port = 80;

    /**
     * constructor that initializes client, cache and headers
     * @param socket is the socket of the client
     * @param cache is the cache of this proxy
     */
    ProxyThread(Socket socket, DNSCache cache) {
        this.client = socket;
        this.cache = cache;
        requestHeaders = new ArrayList<>();
    }

    /**
     * the run method
     */
    @Override
    public void run() {
        try {
            // read request from client
            readRequest();
            System.out.printf("Read request from client, methodType = %s, URI = %s, Host = %s, Port = %d\n", methodType, uri, host, port);

            // Get IP from host
            String ip = cache.checkCache(host);
            if (ip == null) {
                System.err.println("Failed to obtain IP");
                return;
            }
            System.out.printf("Get IP, Host = %s, IP = %s\n", host, ip);

            // Initializes the server with IP address and port number
            server = new Socket(ip, port);
            System.out.printf("Connecting to server at IP = %s, Port = %d\n", ip, port);

            // Send the request to server
            sendRequest();
            System.out.println("Sending request to server");

            // Forward the data from server to client
            forwardData();

            //Close the connection
            server.close();
            client.close();
            System.out.printf("Connection closed. Method Type = %s, URI = %s, Host = %s, Port = %d\n", methodType, uri, host, port);
        } catch (IOException | ProxyException e) {
            System.err.printf("Error = %s\n", e.toString());
        }
    }

    /**
     * reads request from client
     * @throws ProxyException when the proxy fails to read the request or to get the header
     * @throws IOException when there's an abnormal input/output
     */
    private void readRequest() throws ProxyException, IOException {
        InputStream inputStream = client.getInputStream();
        byte[] bytes = new byte[bufferSize];
        int bytesRead = 0;
        // the indicator of the position of the end of a http header with \r\n\r\n
        int position = 0;

        // parse the header from the input stream
        while (true) {
            int dataInBytes = inputStream.read(bytes, bytesRead, bufferSize - bytesRead);
            if (dataInBytes == -1)
                throw new ProxyException(ProxyException.ErrorCode.FAILED_READ_HEADER);
            bytesRead += dataInBytes;
            // find the position
            while (position + 3 < bytesRead && !(bytes[ position ] == '\r'
                                            && bytes[position + 1] == '\n'
                                            && bytes[position + 2] == '\r'
                                            && bytes[position + 3] == '\n'))
                position += 1;
            //reached the end of the header
            if (position + 3 < bytesRead)
                break;
        }

        // Transfer byte to string for header
        String header = new String(bytes, 0, position);
        position += 4;
        // Split header by "\r\n" regex
        String[] lines = header.split("\\r\\n");
        // parse the header
        parseHeader(lines);

        // drops "http://" from the URI
        if (uri.startsWith("http://")) {
            modifyURI();
        }

        // reads the content
        if (contentLength > 0) {
            parseMsg(position, bytesRead, bytes, inputStream);
        }

        // reassigns port number
        if (host != null)
            getPort();
    }

    /**
     * parses the header, assigns values to different fields
     * @param lines is the header stored in string array split by \r\n
     */
    private void parseHeader(String[] lines) throws ProxyException{
        // Split the first line by space
        String[] firstLine = lines[0].split(" +");
        // incorrect request reading if the first line of the header is not consisted of 3 parts: methodType, uri, version
        if (firstLine.length != 3)
            throw new ProxyException(ProxyException.ErrorCode.FAILED_READ_REQUEST);
        methodType = firstLine[0];
        uri = firstLine[1];
        version = firstLine[2];

        // parse header for contentLength and host
        contentLength = 0;
        for (int i = 1; i < lines.length; i++) {
            String[] tmp = lines[i].split(": ?");
            if (tmp.length != 2) {
                continue;
            }
            if (tmp[0].equalsIgnoreCase("proxyd-Connection") ||
                    tmp[0].equalsIgnoreCase("Connection")) {
                // Ignore the proxyd-Connection and Connection in the header
                continue;
            }
            if (tmp[0].equalsIgnoreCase("Content-Length")) {
                // parse contentLength from header
                contentLength = Integer.parseInt(tmp[1]);
            }
            if (tmp[0].equalsIgnoreCase("Host")) {
                //parse host from header
                host = tmp[1];
            }
            requestHeaders.add(lines[i]);
        }
        requestHeaders.add("Connection: close");
    }

    /**
     * modifies uri that starts with http://
     */
    private void modifyURI() {
        int p = uri.indexOf('/', 7);
        if (p > 7) {
            // Get the cleaner version of uri
            if (host == null) {
                host = uri.substring(7, p);
                requestHeaders.add("Host: " + host);
            }
            uri = uri.substring(p);
        }
    }

    /**
     * initializes and parses msg
     * @param position is the position indicator of the end of header
     * @param bytesRead is the number of bytes read
     * @param bytes is data in an array of bytes
     * @param inputStream is the inputStream
     * @throws IOException when there's an abnormal input/output
     * @throws ProxyException when the request was failed to be read
     */
    private void parseMsg(int position, int bytesRead, byte[] bytes, InputStream inputStream) throws IOException, ProxyException{
        msg = new byte[contentLength];
        // Maybe part of body in the b, we need copy to the body
        if (position < bytesRead) {
            System.arraycopy(bytes, position, msg, 0, bytesRead - position);
            bytesRead = bytesRead - position;
        } else
            bytesRead = 0;
        // Now if the body not finished, we need complete it
        while (bytesRead < contentLength) {
            int n = inputStream.read(msg, bytesRead, contentLength - bytesRead);
            if (n == -1) {
                throw new ProxyException(ProxyException.ErrorCode.FAILED_READ_REQUEST);
            }
            bytesRead += n;
        }
    }

    /**
     * get port number from host
     */
    private void getPort() {
        String[] tmp = host.split(":");
        if (tmp.length == 2) {
            host = tmp[0];
            port = Integer.parseInt(tmp[1]);
        }
    }

    /**
     * sends request received from client to server
     * @throws IOException when there's irregular input or output
     */
    private void sendRequest() throws IOException {
        OutputStream outputStream = server.getOutputStream();
        String line = methodType + " " + uri + " " + version + "\r\n";
        outputStream.write(line.getBytes());

        for (String header : requestHeaders) {
            line = header + "\r\n";
            outputStream.write(line.getBytes());
        }
        outputStream.write("\r\n".getBytes());

        if (msg != null)
            outputStream.write(msg);
    }

    /**
     * forwards data received from server to the client
     * @throws IOException when there's irregular input or output
     */
    private void forwardData() throws IOException {
        InputStream inputStream = server.getInputStream();
        OutputStream outputStream = client.getOutputStream();

        byte[] bytes = new byte[bufferSize];
        int dataInBytes;
        while ((dataInBytes = inputStream.read(bytes)) > 0) {
            System.out.printf("Read %d bytes from server sending to client\n", dataInBytes);
            outputStream.write(bytes, 0, dataInBytes);
        }
    }
}