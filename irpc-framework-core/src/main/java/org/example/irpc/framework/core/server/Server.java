package org.example.irpc.framework.core.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.example.irpc.framework.core.common.RpcDecoder;
import org.example.irpc.framework.core.common.RpcEncoder;
import org.example.irpc.framework.core.common.config.PropertiesBootstrap;
import org.example.irpc.framework.core.common.config.ServerConfig;
import org.example.irpc.framework.core.common.utils.CommonUtils;
import org.example.irpc.framework.core.registy.RegistryService;
import org.example.irpc.framework.core.registy.URL;
import org.example.irpc.framework.core.registy.zookeeper.ZookeeperRegister;


import java.util.concurrent.TimeUnit;

import static org.example.irpc.framework.core.common.cache.CommonServerCache.PROVIDER_CLASS_MAP;
import static org.example.irpc.framework.core.common.cache.CommonServerCache.PROVIDER_URL_SET;

public class Server {

    private static EventLoopGroup bossGroup = null;
    private static EventLoopGroup workerGroup = null;

    private ServerConfig serverConfig;
    private RegistryService registryService;

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }


    public void startApplication() throws InterruptedException {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup);
        // TODO
        bootstrap.channel(NioServerSocketChannel.class);
        // TODO
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
        bootstrap.option(ChannelOption.SO_SNDBUF, 16 * 1024)
                .option(ChannelOption.SO_RCVBUF, 16 * 1024)
                .option(ChannelOption.SO_KEEPALIVE, true);

        // TODO 这是在？
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                System.out.println("初始化provider过程");
                socketChannel.pipeline().addLast(new RpcEncoder());
                socketChannel.pipeline().addLast(new RpcDecoder());
                // 核心代码，可以类比于 MVC 中的 Controller，需要实现 ChannelInboundHandlerAdapter
                socketChannel.pipeline().addLast(new ServerHandler());
            }
        });
        // TODO sync() 方法的作用是？
        bootstrap.bind(serverConfig.getServerPort()).sync();
    }


    /**
     * todo 这个方法是在干嘛？答：模拟注册，手动暴露 server 端的接口
     *
     * @param serviceBean
     */
    @Deprecated
    public void registryService(Object serviceBean) {
        if (serviceBean.getClass().getInterfaces().length == 0) {
            throw new RuntimeException("service must had interfaces!");
        }
        Class[] classes = serviceBean.getClass().getInterfaces();
        if (classes.length > 1) {
            throw new RuntimeException("service must only had one interfaces!");
        }
        Class interfaceClass = classes[0];
        //需要注册的对象统一放在一个MAP集合中进行管理
        PROVIDER_CLASS_MAP.put(interfaceClass.getName(), serviceBean);
    }

    public void exportService(Object serviceBean) {
        if (serviceBean.getClass().getInterfaces().length == 0) {
            throw new RuntimeException("service must had interfaces!");
        }
        Class[] classes = serviceBean.getClass().getInterfaces();
        if (classes.length > 1) {
            throw new RuntimeException("service must only had one interfaces!");
        }
        if (registryService == null) {
            registryService = new ZookeeperRegister(serverConfig.getRegisterAddr());
        }
        Class interfaceClass = classes[0];
        PROVIDER_CLASS_MAP.put(interfaceClass.getName(), serviceBean);
        URL url = new URL();
        url.setServiceName(interfaceClass.getName());
        url.setApplicationName(serverConfig.getApplicationName());
        url.addParameter("host", CommonUtils.getIpAddress());
        url.addParameter("port", String.valueOf(serverConfig.getServerPort()));
        PROVIDER_URL_SET.add(url);
    }

    public void batchExportUrl() {
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.MILLISECONDS.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                for (URL url : PROVIDER_URL_SET) {
                    registryService.register(url);
                }
            }
        });
        task.start();
    }

    public void initServerConfig() {
        ServerConfig serverConfig = PropertiesBootstrap.loadServerConfigFromLocal();
        this.setServerConfig(serverConfig);
    }

    public static void main(String[] args) throws InterruptedException {
        Server server = new Server();
        server.initServerConfig();
        server.exportService(new DataServiceImpl());
        server.startApplication();
    }
}
