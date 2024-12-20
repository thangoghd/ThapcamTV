package com.thangoghd.thapcamtv;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import okhttp3.Dns;

public class DNSConfig implements Dns {
    private static final List<String> DNS_SERVERS = Arrays.asList("8.8.8.8", "8.8.4.4", "1.1.1.1");

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        UnknownHostException lastException = null;
        
        for (String dnsServer : DNS_SERVERS) {
            try {
                InetAddress[] addresses = InetAddress.getAllByName(hostname);
                return Arrays.asList(addresses);
            } catch (UnknownHostException e) {
                lastException = e;
            }
        }
        
        throw new UnknownHostException("Không thể kết nối đến " + hostname + 
            ". Đã thử tất cả DNS servers. Vui lòng kiểm tra kết nối mạng.");
    }
}