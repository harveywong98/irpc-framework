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
import org.example.irpc.framework.core.common.config.PropertiesBootstrap;
import org.example.irpc.framework.core.common.event.IRpcListenerLoader;
import org.example.irpc.framework.core.common.utils.CommonUtils;
import org.example.irpc.framework.core.proxy.javassist.JavassistProxyFactory;
import org.example.irpc.framework.core.proxy.jdk.JDKProxyFactory;
import org.example.irpc.framework.core.registy.URL;
import org.example.irpc.framework.core.registy.zookeeper.AbstractRegister;
import org.example.irpc.framework.core.registy.zookeeper.ZookeeperRegister;
import org.example.irpc.framework.interfaces.DataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.example.irpc.framework.core.common.cache.CommonClientCache.SEND_QUEUE;
import static org.example.irpc.framework.core.common.cache.CommonClientCache.SUBSCRIBE_SERVICE_LIST;

public class Client {
    private Logger logger = LoggerFactory.getLogger(Client.class);
    private Bootstrap bootstrap = new Bootstrap();
    public Bootstrap getBootstrap() {
        return bootstrap;
    }
    // TODO 它可以解决什么问题？
    public static EventLoopGroup clientGroup = new NioEventLoopGroup();
    private ClientConfig clientConfig;
    private AbstractRegister abstractRegister;

    private IRpcListenerLoader iRpcListenerLoader;

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }


    public RpcReference initClientApplication() {
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
        iRpcListenerLoader = new IRpcListenerLoader();
        iRpcListenerLoader.init();
        this.clientConfig = PropertiesBootstrap.loadClientConfigFromLocal();
        RpcReference rpcReference;
        if ("javassist".equals(clientConfig.getProxyType())) {
            rpcReference = new RpcReference(new JavassistProxyFactory());
        } else {
            rpcReference = new RpcReference(new JDKProxyFactory());
        }
        return rpcReference;
    }

    public void doSubscribeService(Class serviceBean) {
        if (abstractRegister == null) {
            abstractRegister = new ZookeeperRegister(clientConfig.getRegisterAddr());
        }
        URL url = new URL();
        url.setApplicationName(clientConfig.getApplicationName());
        url.setServiceName(serviceBean.getName());
        url.addParameter("host", CommonUtils.getIpAddress());
        abstractRegister.subscribe(url);
    }

    private void doConnectServer(){
        for (String providerServiceName : SUBSCRIBE_SERVICE_LIST) {
            List<String> providerIps = abstractRegister.getProviderIps(providerServiceName);
            for (String providerIp : providerIps) {
                try {
                    ConnectionHandler.connect(providerServiceName, providerIp);
                } catch (InterruptedException e) {
                    logger.error("[doConnectServer] connect fail ", e);
                }
            }
            URL url = new URL();
            url.setServiceName(providerServiceName);
            //客户端在此新增一个订阅的功能
            // TODO 做什么用？
            abstractRegister.doAfterSubscribe(url);
        }
    }

    private void startClient() {
        Thread asyncSendJob = new Thread(new AsyncSendJob());
        asyncSendJob.start();
    }

    class AsyncSendJob implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    RpcInvocation data = SEND_QUEUE.take();
                    String json = JSON.toJSONString(data);
                    RpcProtocol rpcProtocol = new RpcProtocol(json.getBytes());
                    // TODO 注意这里的改动
                    ChannelFuture channelFuture = ConnectionHandler.getChannelFuture(data.getTargetServiceName());
                    channelFuture.channel().writeAndFlush(rpcProtocol);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        Client client = new Client();
        ClientConfig clientConfig = new ClientConfig();
        RpcReference rpcReference = client.initClientApplication();
        DataService dataService = rpcReference.get(DataService.class);
        client.doSubscribeService(DataService.class);
        ConnectionHandler.setBootstrap(client.getBootstrap());
        // TODO 注意这里的改动
        client.doConnectServer();
        client.startClient();
    }

}
