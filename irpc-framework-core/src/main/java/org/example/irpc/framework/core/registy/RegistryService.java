package org.example.irpc.framework.core.registy;

public interface RegistryService {

    void register(URL url);

    void unRegister(URL url);

    void subscribe(URL url);

    void doUnsubscribe(URL url);
}
