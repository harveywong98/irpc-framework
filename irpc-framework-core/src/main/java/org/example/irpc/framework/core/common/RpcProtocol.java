package org.example.irpc.framework.core.common;

import java.io.Serializable;
import static org.example.irpc.framework.core.common.constants.RpcConstants.MAGIC_NUMBER;

public class RpcProtocol implements Serializable {
    private static final long serialVersionUID = -15540144252385937L;

    // TODO 应该是几？
    private short magicNumber = MAGIC_NUMBER;
    private int contentLength;
    // TODO 这里为什么是类字节数组？
    private byte[] content;

    public short getMagicNumber() {
        return magicNumber;
    }

    public void setMagicNumber(short magicNumber) {
        this.magicNumber = magicNumber;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public RpcProtocol(byte[] content) {
        // TODO 其他字段如何赋值？
        this.content = content;
    }
}
