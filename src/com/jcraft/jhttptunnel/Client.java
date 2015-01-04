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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
class Client extends Thread
{

    private static final Map<String, Client> cpool = new HashMap<String, Client> ();

    private Socket socket = null;
    private InputStream in;
    private OutputStream out;
    boolean connected = false;
    boolean closing = false;

    private final String sid;
    private final String host;
    private final int port;

    public int nextRead = 0;
    public int dataremain = 0;
    public byte command = 0;

    private int buflen = 0;
    private byte[] buf = new byte[1024 * 10000];

    public Client (String sid, String host, int port) {
        this.sid = sid;
        this.host = host;
        this.port = port;

        putClient (sid, this);
    }

    public static Client getClient (String sid) {
        return cpool.get (sid);
    }

    private static void putClient (String sid, Client client) {
        cpool.put (sid, client);
    }

    private static void removeClient (String sid) {
        cpool.remove (sid);
    }

    public void connect () {
        try {
            socket = new Socket (host, port);
            System.out.println ("socket: " + socket);
            in = socket.getInputStream ();
            out = socket.getOutputStream ();
            connected = true;
            start ();
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }

    public boolean isConnected () {
        return connected;
    }

    public void send (byte[] foo, int s, int l) {
        //System.out.println ("send: " + new String (foo, s, l));
        try {
            out.write (foo, s, l);
            out.flush ();
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }

    @Override
    public void run () {
        byte[] tmpBuf = new byte[1024 * 10000];

        while (!closing) {
            try {
                if (closing) {
                    break;
                }

                int space = space ();
                if (space > 0) {
                    if (space > tmpBuf.length) {
                        space = tmpBuf.length;
                    }

                    int i;
                    try {
                        i = in.read (tmpBuf, 0, space);
                    } catch (SocketException ex) {
                        break;
                    }

                    if (i < 0) {
                        break;
                    }
                    if (i > 0) {
                        push (tmpBuf, 0, i);
                        try {
                            Thread.sleep (1);
                        } catch (Exception ee) {
                        }
                        continue;
                    }
                }

                while (!closing) {
                    if (space () > 0) {
                        break;
                    }
                    try {
                        Thread.sleep (1000);
                    } catch (Exception ee) {
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace ();
                break;
            } catch (Exception e) {
                e.printStackTrace ();
            }
        }

        if (!closing) {
            close ();
        }
        closing = false;
    }

    public synchronized int pop (byte[] foo, int s, int l) {
        if (buflen == 0) {
            System.out.println ("Nothing to pop!");
            return 0;
        } else if (buflen < l) {
            System.out.println ("Not enough to pop yet");
            return 0;
        } else if (l > buflen) {
            l = buflen;
        }

        System.out.println ("Popped: " + l + " bytes");
        try {
            System.arraycopy (buf, 0, foo, s, l);
            byte[] bufCopy = new byte[buf.length + l];
            System.arraycopy (buf, l, bufCopy, 0, buf.length - l);
            buf = bufCopy; //System.arraycopy (buf, l, buf, 0, buflen - l);
        } catch (java.lang.ArrayIndexOutOfBoundsException ex) {
            ex.printStackTrace ();
        }

        buflen -= l;
        if (socket == null && buflen <= 0) {
            removeClient (sid);
        }

        System.out.println ("pop: " + l);
        return l;
    }

    public void close () {
        closing = true;
        System.out.println ("Closed client: " + sid);

        try {
            in.close ();
            out.close ();
            socket.close ();
            socket = null;
        } catch (Exception e) {
        }

        if (buflen == 0) {
            System.out.println ("Removed client: " + sid);
            removeClient (sid);
        }
    }

    private synchronized int space () {
        //System.out.println ("space " + (buf.length - buflen));
        return buf.length - buflen;
    }

    private synchronized void push (byte[] foo, int s, int l) {
        System.out.println ("Pushed: " + l + " bytes");
        System.arraycopy (foo, s, buf, buflen, l);
        buflen += l;
    }

}
