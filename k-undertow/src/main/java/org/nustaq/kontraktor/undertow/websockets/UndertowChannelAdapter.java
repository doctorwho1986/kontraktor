package org.nustaq.kontraktor.undertow.websockets;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketChannelAdapter;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ruedi on 28/03/15.
 */
public class UndertowChannelAdapter implements WebSocketChannelAdapter {
    private final WebSocketChannel channel;

    public UndertowChannelAdapter(WebSocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public void setAttribute(String key, Object value) {
        channel.setAttribute(key,value);
    }

    @Override
    public Object getAttribute(String key) {
        return channel.getAttribute(key);
    }

    @Override
    public void sendBinaryMessage(byte[] b) {
        //FIXME: needs to block until complete !
        WebSockets.sendBinary(ByteBuffer.wrap(b),channel,null);
    }

    @Override
    public void sendTextMessage(String s) {
        //FIXME: needs to block until complete !
        WebSockets.sendText(s, channel, null);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public String toString() {
        return "UndertowChannel{" +
                   "channel=" + channel +
                   '}';
    }
}
