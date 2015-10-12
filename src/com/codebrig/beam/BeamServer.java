/*
 * Copyright © 2014-2015 CodeBrig, LLC.
 * http://www.codebrig.com/
 *
 * Beam - Client/Server & P2P Networking Library
 *
 * ====
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * ====
 */
package com.codebrig.beam;

import com.codebrig.beam.connection.ConnectionType;
import com.codebrig.beam.crypt.handlers.RSAHandshakeHandler;
import com.codebrig.beam.handlers.BeamHandler;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.BeamMessageType;
import com.codebrig.beam.pool.CommunicatorPool;
import com.codebrig.beam.pool.DefaultCommunicatorPool;
import com.codebrig.beam.system.handlers.ping.ServerPingPongHandler;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Objects;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class BeamServer extends Thread
{

    /**
     * Creates but does NOT start an available server.
     *
     * @param secure if true server is created with an SSL socket; if false
     * server will be created with a regular server socket.
     *
     * @return a BeamServer listening on a randomly available port.
     */
    public static BeamServer getAvailableServer (boolean secure) {
        return getAvailableServer (secure, false);
    }

    public static BeamServer getAvailableServer (boolean secure, boolean localRestricted) {
        final BeamServer server = new BeamServer (
                String.format ("QuickServer (Secure: %s)", secure), 0, secure, localRestricted);

        try {
            if (secure) {
                ServerSocketFactory factory = SSLServerSocketFactory.getDefault ();
                server.serverSocket = (SSLServerSocket) factory.createServerSocket (0);
                ((SSLServerSocket) server.serverSocket).setEnabledCipherSuites (CIPHER_SUITES);
            } else {
                ServerSocketFactory factory = ServerSocketFactory.getDefault ();
                server.serverSocket = factory.createServerSocket (0);
            }
        } catch (IOException ex) {
            ex.printStackTrace ();
        }

        server.port = server.serverSocket.getLocalPort ();

        return server;
    }

    private static final String[] CIPHER_SUITES = {
        "SSL_DH_anon_WITH_RC4_128_MD5",
        "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
        "SSL_DH_anon_WITH_DES_CBC_SHA",
        "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
        "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA"
    };

    private int port;
    private final boolean secure;
    private final boolean localRestricted;
    private CommunicatorPool pool;
    private ServerSocket serverSocket;
    private boolean listening;
    private ArrayList<HandlerCapsule> handlers;
    private ArrayList<BeamHandler> globalHandlers;
    private String serverName;
    private ServerPingPongHandler pingPong;
    private int servedCount = 0;
    private boolean pingPongEnabled = true;
    private boolean tunneledFlag = false;
    private static int serverUID = 0;
    private long bootTime = -1;
    private BeamMessageType messageType;
    private boolean debugOutput;

    private ConnectionType.Incoming[] incomingConnectionTypes = new ConnectionType.Incoming[] {
        ConnectionType.Incoming.DIRECT
    };
    private final ConnectionType.Outgoing[] outgoingConnectionTypes = new ConnectionType.Outgoing[] {
        ConnectionType.Outgoing.DIRECT
    };

    public BeamServer (int port) {
        this (null, port, true);
    }

    public BeamServer (String serverName, int port) {
        this (serverName, port, true);
    }

    public BeamServer (String serverName, int port, boolean secure) {
        this (serverName, port, secure, false);
    }

    public BeamServer (String serverName, int port, boolean secure, boolean localRestricted) {
        if (serverName == null) {
            serverName = String.format ("BeamServer #%s (Secure: %s)", serverUID, secure);
        } else {
            this.serverName = serverName;
        }

        this.port = port;
        this.secure = secure;
        this.localRestricted = localRestricted;

        handlers = new ArrayList<HandlerCapsule> ();
        globalHandlers = new ArrayList<BeamHandler> ();
        pool = new DefaultCommunicatorPool ();
        pool.setName (serverName + " - Pool");

        setName (serverName);
        serverUID++;
    }

    @Override
    public synchronized void start () {
        if (pingPongEnabled) {
            pingPong = new ServerPingPongHandler (pool);

            //and start main ping pong thread
            Thread ppThread = new Thread (pingPong, serverName + " - PingPong Thread");
            ppThread.setDaemon (true);
            ppThread.start ();
        }

        super.start ();
    }

    @Override
    public void run () {
        bootTime = System.currentTimeMillis ();
        listening = true;

        try {
            if (serverSocket == null) {
                if (secure) {
                    final ServerSocketFactory factory = SSLServerSocketFactory.getDefault ();
                    if (localRestricted) {
                        serverSocket = factory.createServerSocket (port, 0, InetAddress.getByName (null));
                    } else {
                        serverSocket = factory.createServerSocket (port);
                    }

                    ((SSLServerSocket) serverSocket).setEnabledCipherSuites (CIPHER_SUITES);
                } else {
                    if (localRestricted) {
                        serverSocket = new ServerSocket (port, 0, InetAddress.getByName (null));
                    } else {
                        serverSocket = new ServerSocket (port);
                    }
                }
            }

            while (listening) {
                Socket socket = serverSocket.accept ();
                final Communicator comm = new Communicator (socket, serverName, true);
                comm.setMessageType (messageType);
                comm.setDebugOutput (debugOutput);

                for (HandlerCapsule pass : handlers) {
                    final Class<?> theClass = pass.getClassFile ();
                    final Object passObject = pass.getPassObject ();
                    final BeamHandler listener = (BeamHandler) theClass.newInstance ();

                    if (passObject != null) {
                        listener.passObject (passObject);
                    }

                    comm.addHandler (listener);
                }

                //now add any static listeners
                for (BeamHandler global : globalHandlers) {
                    comm.addHandler (global);
                }

                if (pingPong != null) {
                    //add system handlers
                    comm.addSystemHandler (pingPong);
                }

                //and a post connection listener to keep the communicator pool
                //up to date.
                comm.addConnectionStateListener (new ConnectionStateListener ()
                {
                    @Override
                    public void preConnection (Communicator comm) {
                    }

                    @Override
                    public void postConnection (Communicator comm) {
                        //remove from pool
                        pool.removeCommunicator (comm.getUID ());
                    }
                });

                //start after listeners have been added
                comm.init ();

                //and add to pool
                pool.addCommunicator (comm);

                //and add to servedCount
                servedCount++;
            }
        } catch (BindException ex) {
            //BindException means someone is already listening at the port chosen.
            //log and exit.
            ex.printStackTrace ();
            System.exit (-1);
        } catch (SocketException ex) {
            if (!listening) {
                //ignore, user closed
            } else {
                ex.printStackTrace ();
            }
        } catch (IOException ex) {
            ex.printStackTrace ();
        } catch (InstantiationException ex) {
            ex.printStackTrace ();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace ();
        } finally {
            try {
                serverSocket.close ();
            } catch (Exception ex) {
                //server is closed. ignore anything thrown at this point
            }
        }

        listening = false;
    }

    public boolean isTunneled () {
        return tunneledFlag;
    }

    public void setTunneledFlag (boolean tunneledFlag) {
        this.tunneledFlag = tunneledFlag;
    }

    public void addRSAHandshakeHandler (RSAHandshakeHandler handshakeHandler) {
        globalHandlers.add (handshakeHandler);
    }

    public void addGlobalHandler (BeamHandler handler) {
        globalHandlers.add (handler);
    }

    public void addHandler (Class<? extends BeamHandler> type) {
        handlers.add (new HandlerCapsule (type, null));
    }

    public void addHandler (Class<? extends BeamHandler> type, Object passObject) {
        handlers.add (new HandlerCapsule (type, passObject));
    }

    public void clearHandlers () {
        handlers.clear ();
    }

    public void removeHandler (Class<? extends BeamHandler> type) {
        handlers.remove (new HandlerCapsule (type, null));
    }

    public void broadcast (BeamMessage message) {
        pool.broadcastMessage (message);
    }

    public CommunicatorPool getPool () {
        return pool;
    }

    public int getClientCount () {
        return pool.size ();
    }

    public long getServedCount () {
        return servedCount;
    }

    public int getPort () {
        return port;
    }

    public boolean isSecure () {
        return secure;
    }

    public void setPingPongEnabled (boolean pingPongEnabled) {
        this.pingPongEnabled = pingPongEnabled;
    }

    public boolean isPingPongEnabled () {
        return pingPongEnabled;
    }

    public void close () {
        listening = false;

        if (pingPong != null) {
            pingPong.kill ();
        }

        pool.close ();

        try {
            if (serverSocket != null) {
                serverSocket.close ();
            }
        } catch (IOException ex) {
            //server is closed. ignore anything thrown at this point
        }
    }

    public void setMessageType (BeamMessageType messageType) {
        this.messageType = messageType;
    }

    public BeamMessageType getMessageType () {
        return messageType;
    }

    public ConnectionType.Incoming[] getIncomingConnectionTypes () {
        return incomingConnectionTypes;
    }

    public void setIncomingConnectionTypes (ConnectionType.Incoming[] incomingConnections) {
        if (incomingConnections != null) {
            this.incomingConnectionTypes = incomingConnections;
        }
    }

    public ConnectionType.Outgoing[] getOutgoingConnectionTypes () {
        return outgoingConnectionTypes;
    }

    public long getBootTime () {
        return bootTime;
    }

    public void setDebugOutput (boolean debugOutput) {
        this.debugOutput = debugOutput;
    }

    public boolean isDebugOutput () {
        return debugOutput;
    }

    private class HandlerCapsule
    {

        private final Class<?> classFile;
        private final Object passObject;

        public HandlerCapsule (Class<?> classFile, Object passObject) {
            this.classFile = classFile;
            this.passObject = passObject;
        }

        public Class<?> getClassFile () {
            return classFile;
        }

        public Object getPassObject () {
            return passObject;
        }

        @Override
        public boolean equals (Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass () != obj.getClass ()) {
                return false;
            }

            final HandlerCapsule other = (HandlerCapsule) obj;

            return Objects.equals (this.classFile, other.classFile);
        }

        @Override
        public int hashCode () {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode (this.classFile);

            return hash;
        }
    }

}
