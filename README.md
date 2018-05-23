# Overview

This is a __web proxy__ that accepts HTTP requests from browsers, generates the corresponding HTTP requests for the same objects to the origin servers and forwards the responses to the browsers. 

__DNS caching__ is also implemented in this proxy. Each DNS resolution is stored for future use and has a default TTL of 30 seconds

# Installation
1. `Configure`

Configure your network to use a proxy server for LAN connection. Then input the IP address of the proxy server for `Address` (127.0.0.1 if locally deployed) and corresponding port number for `Port` (5042 as default).

Also configure your browser to use this proxy in order to access internet with same parameters as mentioned above.

2. `Install`

```bash
$ git clone https://github.com/DEC4F/Proxy.git
$ cd Proxy
```

3. `Run`

Simply compile and run `proxyd.java` and open your configured browser and you are good to go.
