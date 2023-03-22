package org.example.irpc.framework.core.client;

import org.example.irpc.framework.core.proxy.ProxyFactory;

public class RpcReference {
    private ProxyFactory proxyFactory;

    public RpcReference() {
    }

    public <T> T get(Class<T> tClass) throws Throwable {
        return proxyFactory.getProxy(tClass);
    }

    public RpcReference(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }
}
