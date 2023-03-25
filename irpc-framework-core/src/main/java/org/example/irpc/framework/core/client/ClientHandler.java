package org.example.irpc.framework.core.client;

import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.example.irpc.framework.core.common.RpcInvocation;
import org.example.irpc.framework.core.common.RpcProtocol;

import static org.example.irpc.framework.core.common.cache.CommonClientCache.RESP_MAP;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcProtocol rpcProtocol = (RpcProtocol) msg;
        byte[] reqContent = rpcProtocol.getContent();
        String json = new String(reqContent, 0, reqContent.length);
        RpcInvocation rpcInvocation = JSON.parseObject(json, RpcInvocation.class);
        if (!RESP_MAP.containsKey(rpcInvocation.getUuid())) {
            throw new IllegalArgumentException("server response is error!");
        }
        // todo 为什么判空之后还要再放一遍？是哪里阻塞住了吗？
        // 答：在结果返回之前，客户端一定已经保存了一份 rpcInvocation 了，如果服务端此时没有，那么就可以认为服务端返回的 id 是异常的，因此报错
        RESP_MAP.put(rpcInvocation.getUuid(), rpcInvocation);
        // TODO 这步是在？
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            ctx.close();
        }
    }
}
