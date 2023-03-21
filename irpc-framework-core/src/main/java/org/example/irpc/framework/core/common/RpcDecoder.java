package org.example.irpc.framework.core.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.example.irpc.framework.core.common.constants.RpcConstants;

import java.util.List;

import static org.example.irpc.framework.core.common.constants.RpcConstants.MAGIC_NUMBER;

public class RpcDecoder extends ByteToMessageDecoder {
    /**
     * 协议的开头部分的标准长度
     */
    public final int BASE_LENGTH = 2 + 4;

    /**
     * // TODO 如何重置的索引？
     * @param channelHandlerContext           the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param byteBuf            the {@link ByteBuf} from which to read data
     * @param list           the {@link List} to which decoded messages should be added
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (byteBuf.readableBytes() >= BASE_LENGTH) {
            //防止收到一些体积过大的数据包
            // TODO 这是在干嘛？为什么要跳过这么大的数据包？
            if (byteBuf.readableBytes() > 1000) {
                byteBuf.skipBytes(byteBuf.readableBytes());
            }
            int beginReader;
            while (true) {
                beginReader = byteBuf.readerIndex();
                // 这是在？
                byteBuf.markReaderIndex();
                if (byteBuf.readShort() == MAGIC_NUMBER) {
                    break;
                } else {
                    // TODO 为什么要这么关？关闭的是谁？channel 和 socket 是什么关系？
                    channelHandlerContext.close();
                }
                int length = byteBuf.readInt();
                if (byteBuf.readableBytes() < length) {
                    byteBuf.readerIndex(beginReader);
                    return;
                }

                byte[] data = new byte[length];
                byteBuf.readBytes(data);
                RpcProtocol rpcProtocol = new RpcProtocol(data);
                list.add(rpcProtocol);
            }
        }
    }
}
