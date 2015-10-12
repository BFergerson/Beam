/*
 Copyright (c) 2004 ymnk, JCraft,Inc. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright 
 notice, this list of conditions and the following disclaimer in 
 the documentation and/or other materials provided with the distribution.

 3. The names of the authors may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
 INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcraft.jhttptunnel;

import com.codebrig.beam.connection.httptunnel.TunnelPorts;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class JHttpTunnelServer extends Thread
{

    private int connections = 0;
    private int client_connections = 0;
    private int source_connections = 0;

    private ServerSocket serverSocket = null;
    private String myaddress = null;
    private String myURL = null;
    private String default_host;
    private final TunnelPorts tunnelPorts;

    public JHttpTunnelServer (int lport, String fhost, TunnelPorts tunnelPorts) {
        this (lport, tunnelPorts);
        this.default_host = fhost;
    }

    public JHttpTunnelServer (int port, TunnelPorts tunnelPorts) {
        connections = 0;

        try {
            serverSocket = new ServerSocket (port);
        } catch (IOException e) {
            e.printStackTrace ();
            System.exit (1);
        }

        try {
            if (myaddress == null) {
                myURL = "http://" + InetAddress.getLocalHost ().getHostAddress () + ":" + port;
            } else {
                myURL = "http://" + myaddress + ":" + port;
            }
            System.out.println ("Tunnel Server URL: " + myURL);
        } catch (Exception e) {
            e.printStackTrace ();
        }

        this.tunnelPorts = tunnelPorts;
    }

    @Override
    public void run () {
        Socket socket = null;
        while (true) {
            try {
                socket = serverSocket.accept ();
            } catch (IOException e) {
                System.out.println ("accept error");
                System.exit (1);
            }
            connections++;

            final Socket _socket = socket;
            new Thread (new Runnable ()
            {
                public void run () {
                    try {
                        (new Dispatch (_socket, default_host, tunnelPorts)).doit ();
                    } catch (Exception e) {
                    }
                }
            }).start ();
        }
    }

}
