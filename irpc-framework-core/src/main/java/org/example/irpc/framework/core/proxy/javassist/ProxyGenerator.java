package org.example.irpc.framework.core.proxy.javassist;

import javassist.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyGenerator {
    private static final AtomicInteger counter = new AtomicInteger(1);

    private static ConcurrentHashMap<Class<?>, Object> proxyInstanceCache = new ConcurrentHashMap<>();

    public static Object newProxyInstance(ClassLoader classLoader, Class<?> targetClass, InvocationHandler invocationHandler)
            throws Exception {
        if (proxyInstanceCache.containsKey(targetClass)) {
            return proxyInstanceCache.get(targetClass);
        }
        ClassPool classPool = ClassPool.getDefault();
        String qualifiedName = generateClassName(targetClass);
        CtClass proxy = classPool.makeClass(qualifiedName);
        CtField mf = CtField.make("private static java.lang.reflect.Method[] methods;", proxy);
        proxy.addField(mf);
        CtField hf = CtField.make("private " + InvocationHandler.class.getName() + " handler;", proxy);
        proxy.addField(hf);
        CtConstructor constructor = new CtConstructor(new CtClass[]{classPool.get(InvocationHandler.class.getName())}, proxy);
        constructor.setBody("this.handler=$1;");
        constructor.setModifiers(Modifier.PUBLIC);
        // TODO 在 new CtConstructor() 时第二个参数都传 proxy 了,为什么还要 add 一下?
        proxy.addConstructor(constructor);
        // TODO 这步是在？
        proxy.addConstructor(CtNewConstructor.defaultConstructor(proxy));

        List<Method> methods = new ArrayList<>();
        CtClass ctClass = classPool.get(targetClass.getName());
        proxy.addInterface(ctClass);

        Method[] arr = targetClass.getDeclaredMethods();
        for (Method method : arr) {
            int ix = methods.size();
            Class<?> rt = method.getReturnType();
            Class<?>[] pts = method.getParameterTypes();

            StringBuilder code = new StringBuilder();
            for (int j = 0; j < pts.length; j++) {
                code.append(" args[").append(j).append("] = ($w)$").append(j + 1).append(";");
            }
            code.append(" Object ret = handler.invoke(this, methods[" + ix + "], args);");
            if (!Void.TYPE.equals(rt)) {
                code.append(" return ").append(asArgument(rt, "ret")).append(";");
            }

            // 组装方法
            StringBuilder sb = new StringBuilder(1024);
            sb.append(modifier(method.getModifiers())).append(' ').append(getParameterType(rt)).append(' ').append(method.getName());
            sb.append('(');
            for (int i = 0; i < pts.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(getParameterType(pts[i]));
                sb.append(" arg").append(i);
            }
            sb.append(')');

            Class<?>[] ets = method.getExceptionTypes();
            if (ets != null && ets.length > 0) {
                sb.append(" throws ");
                for (int i = 0; i < ets.length; i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(getParameterType(ets[i]));
                }
            }
            sb.append('{').append(code.toString()).append('}');

            CtMethod ctMethod = CtMethod.make(sb.toString(), proxy);
            proxy.addMethod(ctMethod);

            methods.add(method);
        }

        proxy.setModifiers(Modifier.PUBLIC);
        // TODO 这步是在？
        Class<?> proxyClass = proxy.toClass(classLoader, null);
        proxyClass.getField("methods").set(null, methods.toArray(new Method[0]));

        Object instance = proxyClass.getConstructor(InvocationHandler.class).newInstance(invocationHandler);
        Object old = proxyInstanceCache.putIfAbsent(targetClass, instance);
        if (old != null) {
            instance = old;
        }
        return instance;
    }

    private static String generateClassName(Class<?> type){
        return String.format("%s$Proxy%d", type, counter.getAndIncrement());
    }

    private static String asArgument(Class<?> cl, String name) {
        if (cl.isPrimitive()) {
            if (Boolean.TYPE == cl) {
                return name + "==null?false:((Boolean)" + name + ").booleanValue()";
            }
            if (Byte.TYPE == cl) {
                return name + "==null?(byte)0:((Byte)" + name + ").byteValue()";
            }
            if (Character.TYPE == cl) {
                return name + "==null?(char)0:((Character)" + name + ").charValue()";
            }
            if (Double.TYPE == cl) {
                return name + "==null?(double)0:((Double)" + name + ").doubleValue()";
            }
            if (Float.TYPE == cl) {
                return name + "==null?(float)0:((Float)" + name + ").floatValue()";
            }
            if (Integer.TYPE == cl) {
                return name + "==null?(int)0:((Integer)" + name + ").intValue()";
            }
            if (Long.TYPE == cl) {
                return name + "==null?(long)0:((Long)" + name + ").longValue()";
            }
            if (Short.TYPE == cl) {
                return name + "==null?(short)0:((Short)" + name + ").shortValue()";
            }
            throw new RuntimeException(name + " is unknown primitive type.");
        }
        return "(" + getParameterType(cl) + ")" + name;
    }

    public static String getParameterType(Class<?> c) {
        if (c.isArray()) {   //数组类型
            StringBuilder sb = new StringBuilder();
            do {
                sb.append("[]");
                c = c.getComponentType();
            } while (c.isArray());

            return c.getName() + sb.toString();
        }
        return c.getName();
    }

    private static String modifier(int mod) {
        if (Modifier.isPublic(mod)) {
            return "public";
        }
        if (Modifier.isProtected(mod)) {
            return "protected";
        }
        if (Modifier.isPrivate(mod)) {
            return "private";
        }
        return "";
    }

}
