package org.nustaq.kontraktor.remoting.websocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.ActorProxy;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.http.NioHttpServer;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketChannelAdapter;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketErrorMessage;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketTextMessage;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;

/**
 * Created by ruedi on 30.03.2015.
 */
public class WebSocketActorServer extends ActorServer {

    protected NioHttpServer server;
    protected Coding coding;

    public WebSocketActorServer( NioHttpServer server, Coding coding, Actor facade) {
        super(facade);
        this.coding = coding;
        this.server = server;
    }

    @Override
    public void acceptLoop() throws Exception {
        // event driven
    }

    @Override
    protected ActorServerConnection acceptConnections() throws Exception {
        return null; // event driven
    }

    /**
     * @return wether to spwan a thread per client
     */
    protected boolean isThreadPerClient() {
        return false;
    }

    @Override
    protected void connectServerSocket() throws Exception {
        // needs to be wired from outside
        throw new RuntimeException("unsupported operation");
    }

    @Override
    protected void closeSocket() {
        server.stopServer();
    }

    ///////////////////////////////////////////////// ws adapter callbacks

    public void onOpen(WebSocketChannelAdapter channel) {
        ActorServerConnection con = new ActorServerConnection();
        MyWSObjectSocket socket = new MyWSObjectSocket(con.getConf(), channel);
        con.init(socket, facade);
        channel.setAttribute("con",con);
        doAccept(con);
    }

    public void onBinaryMessage(WebSocketChannelAdapter channel, byte[] buffer) {
        ActorServerConnection con = (ActorServerConnection) channel.getAttribute("con");
        ((MyWSObjectSocket)con.getObjSocket()).setNextMsg(buffer);
        con.currentObjectSocket.set(con.getObjSocket());

        try {
            while( con.singleReceive(con.getObjSocket()) ) {
                // do nothing
            }
        } catch (Exception e) {
            e.printStackTrace();
            // FIXME: cleanup ?
        }
    }

    public void onClose(WebSocketChannelAdapter channel) {
        ActorServerConnection con = (ActorServerConnection) channel.getAttribute("con");
        con.handleTermination();
    }

    public void onTextMessage(WebSocketChannelAdapter channel, String text) {
        // kontrakor uses binary messages only, however just forward text messages sent by a client
        facade.$receive(new WebSocketTextMessage(text, channel));
    }

    public void onPong(WebSocketChannelAdapter channel) {
        // ping pong is implemented by adapter
    }

    public void onError(WebSocketChannelAdapter channel) {
        // kontrakor uses binary messages only, however just forward text messages sent by a client
        // to facade actor
        Log.Lg.info(this, "websocket connection error " + channel);
        facade.$receive(new WebSocketErrorMessage(channel));
        ActorServerConnection con = (ActorServerConnection) channel.getAttribute("con");
        con.handleTermination();
    }

    class MyWSObjectSocket extends WebObjectSocket {
        WebSocketChannelAdapter channel;

        MyWSObjectSocket(FSTConfiguration conf, WebSocketChannelAdapter channel ) {
            super(conf);
            this.channel = channel;
        }

        @Override
        public void writeAndFlushObject(Object toWrite) throws Exception {
            final byte[] b = conf.asByteArray(toWrite);
            channel.sendBinaryMessage(b);
        }

        @Override
        public void close() throws IOException {
            channel.close();

        }

        @Override
        public FSTConfiguration getConf() {
            return conf;
        }

    }

}
