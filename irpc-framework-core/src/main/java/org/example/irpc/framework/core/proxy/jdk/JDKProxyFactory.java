package org.example.irpc.framework.core.proxy.jdk;

import org.example.irpc.framework.core.proxy.ProxyFactory;

import java.lang.reflect.Proxy;

public class JDKProxyFactory implements ProxyFactory {
    @Override
    public <T> T getProxy(final Class clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz},
                 new JDKClientInvocationHandler(clazz));
    }
}
