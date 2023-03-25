package org.example.irpc.framework.core.registy.zookeeper;

import org.example.irpc.framework.core.registy.RegistryService;
import org.example.irpc.framework.core.registy.URL;

import java.util.List;

import static org.example.irpc.framework.core.common.cache.CommonClientCache.SUBSCRIBE_SERVICE_LIST;
import static org.example.irpc.framework.core.common.cache.CommonServerCache.PROVIDER_URL_SET;

public abstract class AbstractRegister implements RegistryService {
    @Override
    public void register(URL url) {
        PROVIDER_URL_SET.add(url);
    }

    @Override
    public void unRegister(URL url) {
        PROVIDER_URL_SET.remove(url);
    }

    @Override
    public void subscribe(URL url) {
        SUBSCRIBE_SERVICE_LIST.add(url.getServiceName());
    }

    @Override
    public void doUnsubscribe(URL url) {
        SUBSCRIBE_SERVICE_LIST.remove(url.getServiceName());
    }

    public abstract void doAfterSubscribe(URL url);

    public abstract void doBeforeSubscribe(URL url);

    public abstract List<String> getProviderIps(String serviceName);
}
