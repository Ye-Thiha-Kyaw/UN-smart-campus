package com.unsmart.campus.jmdns;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ServiceDiscovery {
    private static final Logger logger = Logger.getLogger(ServiceDiscovery.class.getName());
    private final ConcurrentMap<String, String> services = new ConcurrentHashMap<>();
    private final Consumer<String> logConsumer;
    private JmDNS jmdns;

    public ServiceDiscovery(Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
    }

    public void startDiscovery() throws IOException {
        try {
            jmdns = JmDNS.create(InetAddress.getLocalHost());
            jmdns.addServiceListener("_grpc._tcp.local.", new ServiceListener() {
                @Override
                public void serviceAdded(ServiceEvent event) {
                    jmdns.requestServiceInfo(event.getType(), event.getName());
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    services.remove(event.getName());
                    logConsumer.accept("Service removed: " + event.getName());
                }

                @Override
                public void serviceResolved(ServiceEvent event) {
                    String name = event.getName();
                    String address = event.getInfo().getHostAddresses()[0] + ":" + event.getInfo().getPort();
                    services.put(name, address);
                    logConsumer.accept("Service resolved: " + name + " at " + address);
                }
            });
        } catch (IOException e) {
            logger.severe("Failed to start service discovery: " + e.getMessage());
            throw e;
        }
    }

    public String getServiceAddress(String serviceName) {
        return services.get(serviceName);
    }
}