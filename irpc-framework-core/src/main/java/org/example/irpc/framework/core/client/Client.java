package org.example.irpc.framework.core.client;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.example.irpc.framework.core.common.RpcDecoder;
import org.example.irpc.framework.core.common.RpcEncoder;
import org.example.irpc.framework.core.common.RpcInvocation;
import org.example.irpc.framework.core.common.RpcProtocol;
import org.example.irpc.framework.core.common.config.ClientConfig;
import org.example.irpc.framework.core.proxy.javassist.JavassistProxyFactory;
import org.example.irpc.framework.core.proxy.jdk.JDKProxyFactory;
import org.example.irpc.framework.interfaces.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.example.irpc.framework.core.common.cache.CommonClientCache.SEND_QUEUE;

public class Client {
    private Logger logger = LoggerFactory.getLogger(Client.class);
    // TODO 它可以解决什么问题？
    public static EventLoopGroup clientGroup = new NioEventLoopGroup();
    private ClientConfig clientConfig;

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    public RpcReference startClientApplication() throws InterruptedException {
        EventLoopGroup clientGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new io.netty.bootstrap.Bootstrap();
        bootstrap.group(clientGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new RpcEncoder());
                ch.pipeline().addLast(new RpcDecoder());
                ch.pipeline().addLast(new ClientHandler());
            }
        });

        ChannelFuture channelFuture = bootstrap.connect(clientConfig.getServerAddr(), clientConfig.getServerPort()).sync();
        logger.info("============ 服务启动 ============");
        this.startClient(channelFuture);
        // TODO 这里是解决了什么问题？
        RpcReference rpcReference = new RpcReference(new JDKProxyFactory());
//        RpcReference rpcReference = new RpcReference(new JavassistProxyFactory());
        return rpcReference;
    }

    private void startClient(ChannelFuture channelFuture) {
        Thread asyncSendJob = new Thread(new AsyncSendJob(channelFuture));
        asyncSendJob.start();
    }

    public static void main(String[] args) throws Throwable {
        Client client = new Client();
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setServerPort(9090);
        clientConfig.setServerAddr("localhost");
        client.setClientConfig(clientConfig);

        RpcReference rpcReference = client.startClientApplication();
        // TODO
        DataService dataService = rpcReference.get(DataService.class);
        for (int i = 0; i < 100; i++) {
            String result = dataService.sendData("test");
            System.out.println(result);
        }
    }

    class AsyncSendJob implements Runnable {
        private ChannelFuture channelFuture;

        public AsyncSendJob(ChannelFuture channelFuture) {
            this.channelFuture = channelFuture;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    RpcInvocation data = SEND_QUEUE.take();
                    String json = JSON.toJSONString(data);
                    RpcProtocol rpcProtocol = new RpcProtocol(json.getBytes());
                    channelFuture.channel().writeAndFlush(rpcProtocol);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
