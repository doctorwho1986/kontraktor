package org.nustaq.kontraktor;

/**
 * Created by ruedi on 24.08.2014.
 */
public interface RemoteConnection {
    public void close();
    public void setClassLoader( ClassLoader l );
    public int getRemoteId( Actor act );
}
