package com.unsmart.campus.jmdns;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;

public class ServiceRegistration {
    public static void registerService(String serviceName, String serviceType, int port, String serviceDescription) {
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            ServiceInfo serviceInfo = ServiceInfo.create(serviceType, serviceName, port, serviceDescription);
            jmdns.registerService(serviceInfo);
            System.out.println("Registered service: " + serviceName + " on port " + port);
        } catch (IOException e) {
            System.err.println("Error registering service: " + e.getMessage());
        }
    }
}