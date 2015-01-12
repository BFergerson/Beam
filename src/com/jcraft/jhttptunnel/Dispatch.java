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
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
class Dispatch
{

    private final String rootDirectory = ".";
    private final String defaultFile = "index.html";

    private final TunnelPorts tunnelPorts;
    private String default_host = null;
    private HTTPSocket mySocket = null;
    private byte[] buf = new byte[1024 * 10000];

    public Dispatch (Socket s, String default_host, TunnelPorts tunnelPorts) throws IOException {
        mySocket = new HTTPSocket (s);
        this.default_host = default_host;
        this.tunnelPorts = tunnelPorts;
    }

    private List<String> getHttpHeader (HTTPSocket ms) throws IOException {
        List<String> v = new ArrayList<String> ();
        String foo;

        while (true) {
            foo = ms.readLine ();
            if (foo.length () == 0) {
                break;
            }
            v.add (foo);
        }

        return v;
    }

    private void procPOST (String string, List<String> httpheader) throws IOException {
        String foo;
        int len = 0;
        int c;
        String file = string.substring (string.indexOf (' ') + 1);
        if (file.indexOf (' ') != -1) {
            file = file.substring (0, file.indexOf (' '));
        }

        Map<String, String> vars = getVars ((file.indexOf ('?') != -1)
                ? file.substring (file.indexOf ('?') + 1) : null);
        String sid = (String) vars.get ("SESSIONID");
        boolean gotForwardPort = false;
        int fPort = 0;
        Object portObj = vars.get ("fport");
        if (portObj != null) {
            String portStr = (String) portObj;
            if (!portStr.isEmpty ()) {
                try {
                    int port = tunnelPorts.getTunnelPort (Integer.parseInt (portStr));
                    if (port != -1) {
                        fPort = port;
                        gotForwardPort = true;
                    }
                } catch (NumberFormatException ex) {
                    ex.printStackTrace ();
                }
            }
        }

        if (!gotForwardPort) {
            notFound (mySocket);
            return;
        }

        Client client = Client.getClient (sid);
        if (client == null) {
            client = new Client (sid, default_host, fPort);
        }

        client.dataremain = 0;

        System.out.println ("client: " + client);

        for (int i = 0; i < httpheader.size (); i++) {
            foo = httpheader.get (i);
            if (foo.startsWith ("Content-Length:")
                    || foo.startsWith ("Content-length:") // hmm... for Opera, lynx
                    ) {
                foo = foo.substring (foo.indexOf (' ') + 1);
                foo = foo.trim ();
                len = Integer.parseInt (foo);
            }
        }

        System.out.println ("len: " + len);

        if (len == 0) {   // just read data
            client.command = -1;
            client.dataremain = 0;
        }

        if (client.dataremain == 0 && len > 0) {
            int i = mySocket.read (buf, 0, 1);  // command
            len--;
            client.command = buf[0];
            int datalen = 0;

            if ((client.command & JHttpTunnel.TUNNEL_SIMPLE) == 0) {
                if (client.command == JHttpTunnel.TUNNEL_NEXT_READ || client.command == JHttpTunnel.TUNNEL_DATA) {
                    i = mySocket.read (buf, 0, 4);
                    len -= 4;

                    datalen = intFromByteArray (buf);
                } else {
                    i = mySocket.read (buf, 0, 2);
                    len -= 2;

                    datalen = (((buf[0]) << 8) & 0xff00);
                    datalen = datalen | (buf[1] & 0xff);
                }
            }

            System.out.println ("command: " + client.command + " " + datalen);
            client.dataremain = datalen;
        }

        System.out.println ("dataremain: " + client.dataremain + " len=" + len);

        int i = 0;
        if (len > 0) {
            buf = new byte[1024 * 10000];
            i = mySocket.read (buf, 0, len);
            //String data = new String (buf, 0, i);
            //System.println (String.format ("Received data: [%s]", data));
            client.dataremain -= len;
        }

        if (client.command == JHttpTunnel.TUNNEL_NEXT_READ || client.dataremain == 0) {
            System.out.println (sid + ": " + client.command);
            switch (client.command) {
                case JHttpTunnel.TUNNEL_OPEN:
                    client.connect ();
                    System.out.println ("Tunnel opened!");
                    break;
                case JHttpTunnel.TUNNEL_DATA:
                    client.send (buf, 0, len);
                    System.out.println ("Tunneled (to server): " + len + " bytes");
                    break;
                case JHttpTunnel.TUNNEL_CLOSE:
                    System.out.println ("Tunnel closed!");
                    client.close ();
                    break;
                case JHttpTunnel.TUNNEL_NEXT_READ:
                    client.nextRead = client.dataremain - 5; //minus 5 for command to go back
                    System.out.println ("Set next read: " + client.nextRead);
                    break;
            }
        }

        i = 0;
        if (client.command != JHttpTunnel.TUNNEL_NEXT_READ && client.isConnected ()) {
            i = client.pop (buf, 5, buf.length - 5);
            if (i > 0) {
                buf[0] = JHttpTunnel.TUNNEL_DATA;
                byte[] size = toIntByteArray (i);
                System.arraycopy (size, 1, buf, 1, size.length);

                i += 5;
            }
        }
        ok (mySocket, buf, 0, i, sid);
    }

    private void procGET (String string, List<String> httpheader) throws IOException {
        String foo;
        int c;
        String file = string.substring (string.indexOf (' ') + 1);
        if (file.indexOf (' ') != -1) {
            file = file.substring (0, file.indexOf (' '));
        }
        Map<String, String> vars = getVars ((file.indexOf ('?') != -1)
                ? file.substring (file.indexOf ('?') + 1) : null);
        String sid = (String) vars.get ("SESSIONID");
        Client client = Client.getClient (sid);
        if (client == null) {
            notFound (mySocket);
            return;
        }

        try {
            ok (mySocket, client, client.nextRead, null);
        } catch (IOException e) {
            e.printStackTrace ();
        }
    }

    private void procHEAD (String string, List<String> httpheader) throws IOException {
        ok (mySocket, null, 0, 0, "");
    }

    private String decode (String arg) {
        byte[] foo = arg.getBytes ();
        StringBuilder sb = new StringBuilder ();
        for (int i = 0; i < foo.length; i++) {
            if (foo[i] == '+') {
                sb.append ((char) ' ');
                continue;
            }
            if (foo[i] == '%' && i + 2 < foo.length) {
                int bar = foo[i + 1];
                bar = ('0' <= bar && bar <= '9') ? bar - '0'
                        : ('a' <= bar && bar <= 'z') ? bar - 'a' + 10
                                : ('A' <= bar && bar <= 'Z') ? bar - 'A' + 10 : bar;
                bar *= 16;
                int goo = foo[i + 2];
                goo = ('0' <= goo && goo <= '9') ? goo - '0'
                        : ('a' <= goo && goo <= 'f') ? goo - 'a' + 10
                                : ('A' <= goo && goo <= 'F') ? goo - 'A' + 10 : goo;
                bar += goo;
                bar &= 0xff;
                sb.append ((char) bar);
                i += 2;
                continue;
            }
            sb.append ((char) foo[i]);
        }

        return sb.toString ();
    }

    private Map<String, String> getVars (String arg) {
        HashMap<String, String> vars = new HashMap<String, String> ();
        if (arg == null) {
            return vars;
        }
        arg = decode (arg);
        int foo = 0;
        int i = 0;
        int c = 0;
        String key, value;

        while (true) {
            foo = arg.indexOf ('=');
            if (foo == -1) {
                break;
            }
            key = arg.substring (0, foo);
            arg = arg.substring (foo + 1);
            foo = arg.indexOf ('&');
            if (foo != -1) {
                value = arg.substring (0, foo);
                arg = arg.substring (foo + 1);
            } else {
                value = arg;
            }
            vars.put (key, value);
            if (foo == -1) {
                break;
            }
        }

        return vars;
    }

    public void doit () {
        try {
            String foo = mySocket.readLine ();

            //System.out.println (mySocket.socket.getInetAddress () + ": " + foo + " " + (new java.util.Date ()));
            if (foo.indexOf (' ') == -1) {
                mySocket.close ();
                return;
            }

            String bar = foo.substring (0, foo.indexOf (' '));
            //System.out.println (foo);

            List<String> v = getHttpHeader (mySocket);

            //System.out.println (v);
            if (bar.equalsIgnoreCase ("POST")) {
                procPOST (foo, v);
                return;
            }

            if (bar.equalsIgnoreCase ("GET")) {
                procGET (foo, v);
                return;
            }

            if (bar.equalsIgnoreCase ("HEAD")) {
                procHEAD (foo, v);
            }
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }

    private void ok (HTTPSocket mysocket, byte[] buf, int s, int l, String sid) throws IOException {
        mysocket.println ("HTTP/1.1 200 OK");
        mysocket.println ("Last-Modified: Thu, 04 Oct 2001 14:09:23 GMT");
        if (sid != null) {
            mysocket.println ("x-SESSIONID: " + sid);
        }
        mysocket.println ("Content-Length: " + l);
        mysocket.println ("Connection: close");
        mysocket.println ("Content-Type: text/html; charset=iso-8859-1");
        mysocket.println ("");

        if (l > 0) {
            mysocket.write (buf, s, l);
        }

        mysocket.flush ();
        mysocket.close ();
    }

    private void ok (HTTPSocket mysocket, Client client, int l, String sid) throws IOException {
        mysocket.println ("HTTP/1.1 200 OK");
        mysocket.println ("Last-Modified: Thu, 04 Oct 2001 14:09:23 GMT");
        if (sid != null) {
            mysocket.println ("x-SESSIONID: " + sid);
        }
        mysocket.println ("Content-Length: " + l);
        mysocket.println ("Connection: close");
        mysocket.println ("Content-Type: text/html; charset=iso-8859-1");
        mysocket.println ("");

        int sent = 0;
        if (l > 0) {
            byte[] buf = new byte[1024 * 10000];
            byte[] command = new byte[5];
            command[0] = JHttpTunnel.TUNNEL_DATA;

            byte[] size = toIntByteArray (l);
            System.arraycopy (size, 0, command, 1, size.length);

            while (sent < l) {
                int i = client.pop (buf, 0, l);
                if (i <= 0) {
                    break;
                }

                if (i > 0) {
                    byte[] combined = new byte[command.length + l];

                    System.arraycopy (command, 0, combined, 0, command.length);
                    System.arraycopy (buf, 0, combined, command.length, l);

                    if (sent == 0) {
                        System.out.println ("Tunneled (to client): " + combined.length + " bytes");
                        mysocket.write (combined, 0, combined.length);
                    } else {
                        System.out.println ("Tunneled (to server): " + buf.length + " bytes");
                        mysocket.write (buf, 0, buf.length);
                    }

                    sent += i;
                    client.nextRead -= sent;
                }
            }
        }

        mysocket.flush ();
        mysocket.close ();
    }

    private void notFound (HTTPSocket ms) throws IOException {
        ms.println ("HTTP/1.1 404 Not Found");
        ms.println ("Content-Type: text/html");
        ms.println ("Content-Length: 0");
        ms.println ("Connection: close");
        ms.println ("");
        ms.flush ();
        ms.close ();
    }

    private int intFromByteArray (byte[] bytes) {
        return ByteBuffer.wrap (bytes).getInt ();
    }

    private byte[] toIntByteArray (int value) {
        return ByteBuffer.allocate (4).putInt (value).array ();
    }

}
