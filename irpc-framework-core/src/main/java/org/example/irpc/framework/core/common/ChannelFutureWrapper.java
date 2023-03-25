package org.example.irpc.framework.core.common;

import io.netty.channel.ChannelFuture;

public class ChannelFutureWrapper {
    private String host;
    private Integer port;
    private ChannelFuture channelFuture;

    public ChannelFutureWrapper() {
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    public void setChannelFuture(ChannelFuture channelFuture) {
        this.channelFuture = channelFuture;
    }

}
