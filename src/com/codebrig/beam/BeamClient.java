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

import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.connection.ConnectionType;
import com.codebrig.beam.crypt.AES;
import com.codebrig.beam.crypt.RSA;
import com.codebrig.beam.crypt.RSAConnection;
import com.codebrig.beam.crypt.RSAConnectionHolder;
import com.codebrig.beam.crypt.messages.RSAHandshakeMessage;
import com.codebrig.beam.utils.Base64;
import com.codebrig.beam.utils.Generator;
import com.jcraft.jhttptunnel.JHttpTunnelClient;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;
import java.util.Arrays;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class BeamClient
{

    private static final String[] CIPHER_SUITES = {
        "SSL_DH_anon_WITH_RC4_128_MD5",
        "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
        "SSL_DH_anon_WITH_DES_CBC_SHA",
        "SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
        "SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA"
    };

    private ConnectionType.Incoming[] incomingConnectionTypes
            = new ConnectionType.Incoming[] {
                ConnectionType.Incoming.DIRECT
            };
    private ConnectionType.Outgoing[] outgoingConnectionTypes
            = new ConnectionType.Outgoing[] {
                ConnectionType.Outgoing.DIRECT
            };

    private final String host;
    private final String clientName;
    private final int port;
    private final boolean secure;
//    private final Proxy proxy;
//    private final String proxyUsername;
//    private final String proxyPassword;

    private Communicator communicator;
    private boolean connected = false;
    private boolean debugOutput;

    public BeamClient (String host, int port) {
        this (host, null, port, true);
    }

    public BeamClient (String host, String clientName, int port) {
        this (host, clientName, port, true);
    }

    public BeamClient (String host, String clientName, int port, boolean secure) {
        this.host = host;
        this.clientName = clientName;
        this.port = port;
        this.secure = secure;
    }

    public void connect () throws IOException {
        boolean tunnel = false;
        Proxy proxy = null;
        String proxyUsername = null;
        String proxyPassword = null;

        if (proxy != null) {
            if (secure) {
                final SocketFactory factory = SSLSocketFactory.getDefault ();

                final SSLSocket sock;
                Socket tmpSock = new Socket (proxy);
                if (proxyPassword != null && proxyPassword.isEmpty ()) {
                    Authenticator.setDefault (new AuthenticatorImpl (proxy.type (), proxyUsername, proxyPassword));
                } else {
                    Authenticator.setDefault (null);
                }
                tmpSock.connect (new InetSocketAddress (host, port));

                sock = (SSLSocket) ((SSLSocketFactory) factory).createSocket (tmpSock, host, port, true);

                sock.setEnabledCipherSuites (CIPHER_SUITES);
                if (tunnel) {
                    communicator = JHttpTunnelClient.getCommunicator (host, port); //todo: use sock to get host
                } else {
                    communicator = new Communicator (sock, clientName, false);
                }
            } else {
                final Socket sock = new Socket (proxy);
                if (proxyPassword != null && proxyPassword.isEmpty ()) {
                    Authenticator.setDefault (new AuthenticatorImpl (proxy.type (), proxyUsername, proxyPassword));
                } else {
                    Authenticator.setDefault (null);
                }
                sock.connect (new InetSocketAddress (host, port));

                if (tunnel) {
                    communicator = JHttpTunnelClient.getCommunicator (host, port); //todo: use sock to get host
                } else {
                    communicator = new Communicator (sock, clientName, false);
                }
            }
        } else {
            if (secure) {
                final SocketFactory factory = SSLSocketFactory.getDefault ();
                final SSLSocket sock = (SSLSocket) factory.createSocket (host, port);

                sock.setEnabledCipherSuites (CIPHER_SUITES);
                if (tunnel) {
                    communicator = JHttpTunnelClient.getCommunicator (host, port);
                } else {
                    communicator = new Communicator (sock, clientName, false);
                }
            } else {
                final Socket sock;
                if (tunnel) {
                    sock = new Socket (host, 80);
                } else {
                    sock = new Socket (host, port);
                }
                if (tunnel) {
                    communicator = JHttpTunnelClient.getCommunicator (host, port);
                } else {
                    communicator = new Communicator (sock, clientName, false);
                }
            }
        }

        communicator.setDebugOutput (debugOutput);
        communicator.performBeamHandshake ();
        connected = true;
    }

    public void setIncomingConnectionTypes (ConnectionType.Incoming[] incomingConnections) {
        if (incomingConnections != null) {
            this.incomingConnectionTypes = incomingConnections;
        }
    }

    public ConnectionType.Incoming[] getIncomingConnectionTypes () {
        return incomingConnectionTypes;
    }

    public void setOutgoingConnectionTypes (ConnectionType.Outgoing[] outgoingConnections) {
        if (outgoingConnections != null) {
            this.outgoingConnectionTypes = outgoingConnections;
        }
    }

    public ConnectionType.Outgoing[] getOutgoingConnectionTypes () {
        return outgoingConnectionTypes;
    }

    public Communicator getCommunicator () {
        return communicator;
    }

    public boolean isSecure () {
        return secure;
    }

    public void close () {
        if (communicator != null) {
            communicator.close ();
        }
    }

    public BeamMessage sendMessage (BeamMessage message) {
        if (!connected) {
            throw new CommunicatorException ("Client has not yet been connected!");
        }

        return communicator.send (message);
    }

    public void queueMessage (BeamMessage message) {
        if (!connected) {
            throw new CommunicatorException ("Client has not yet been connected!");
        }

        communicator.queue (message);
    }

    public RSAConnection establishRSAConnection (RSA publicKey) {
        if (!connected) {
            throw new CommunicatorException ("Client has not yet been connected!");
        }

        return establishRSAConnection (publicKey, 100);
    }

    public RSAConnection establishRSAConnection (RSA publicKey, int keyCharCount) {
        if (!connected) {
            throw new CommunicatorException ("Client has not yet been connected!");
        } else if (publicKey == null) {
            throw new IllegalArgumentException ("Missing public key!");
        }

        RSAHandshakeMessage rsaMessage = new RSAHandshakeMessage ();
        String connectionKey = Generator.makeString (keyCharCount);
        rsaMessage.setConnectionKey (publicKey.encrypt (connectionKey));

        BeamMessage respMessage = communicator.send (rsaMessage);
        if (respMessage != null) {
            RSAHandshakeMessage respRSAMessage = new RSAHandshakeMessage (respMessage);

            if (respRSAMessage.isSuccessful ()) {
                RSAConnection rsaConnection
                        = new RSAConnection (publicKey, new AES (connectionKey), respRSAMessage.getSession ());
                RSAConnectionHolder.addRSAConnection (communicator, rsaConnection);

                return rsaConnection;
            }
        }

        return null;
    }

    public void setDebugOutput (boolean debugOutput) {
        this.debugOutput = debugOutput;
        
        if (communicator != null) {
            communicator.setDebugOutput (debugOutput);
        }
    }

    public boolean isDebugOutput () {
        return debugOutput;
    }

    private static class AuthenticatorImpl extends Authenticator
    {

        private final String username;
        private final String password;

        public AuthenticatorImpl (Proxy.Type proxyType, String username, String password) {
            if (username == null) {
                username = "";
            }
            if (password == null) {
                password = "";
            }
            if (!password.isEmpty () && proxyType == Proxy.Type.HTTP) {
                password = new String (Base64.encode (username + ":" + Arrays.toString (password.getBytes ())));
            }

            this.username = username;
            this.password = password;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication () {
            return new PasswordAuthentication (username, password.toCharArray ());
        }
    }

}
