import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * created by Stanley Tian
 */
class DNSCache {
    private Map<String, String> cache;
    private Map<String, Long> cacheTime;
    private final int timeToLive = 30;

    DNSCache() {
        cache = new HashMap<>();
        cacheTime = new HashMap<>();
    }

    /**
     * checks if the IP address of the input host is stored locally
     * @param host is the host
     * @return the IP address of the host
     */
    synchronized String checkCache(String host) {
        long currentTime = System.currentTimeMillis()/1000;
        Long expireTime = cacheTime.get(host);
        if (expireTime != null && currentTime < expireTime) {
            System.out.println("DNS cache is found");
            return cache.get(host);
        }
        return requestIP(host, currentTime);
    }

    /**
     * request IP address from the host name
     * @param host is the host
     * @return the newly requested IP address of the host
     */
    private String requestIP(String host, long currentTime) {
        try {
            String ip = InetAddress.getByName(host).getHostAddress();
            System.out.printf("Newly requested IP = %s\n", ip);
            cache.put(host, ip);
            cacheTime.put(host, currentTime+timeToLive);
            return ip;
        } catch (UnknownHostException e1) {
            System.err.printf("Error: %s\n", e1);
            return null;
        }
    }
}