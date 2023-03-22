package org.example.irpc.framework.core.common.cache;

import org.example.irpc.framework.core.common.RpcInvocation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class CommonClientCache {

    public static BlockingDeque<RpcInvocation> SEND_QUEUE = new LinkedBlockingDeque<>();

    public static Map<String, Object> RESP_MAP = new HashMap<>();

}
