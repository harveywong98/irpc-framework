package org.example.irpc.framework.core.proxy;

public interface ProxyFactory {
    <T> T getProxy(final Class clazz) throws Throwable;
}
