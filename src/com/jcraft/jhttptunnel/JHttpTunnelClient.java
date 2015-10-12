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

import com.codebrig.beam.Communicator;
import com.codebrig.beam.utils.Generator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class JHttpTunnelClient
{

    final static private int CONTENT_LENGTH = 1024 * 10;

    private boolean init = false;
    private boolean closed = false;

    private String dest_host = null;
    private int dest_port = 0;

    private InBoundURL ib = null;
    private OutBoundURL ob = null;
    private static long readAfter = 0;
    private final byte[] command = new byte[4];
    int buf_len = 0;
    private InputStream in = null;
    private OutputStream out = null;
    private Communicator comm;

    public static Communicator getCommunicator (String host, int forwardPort)
            throws JHttpTunnelException {
        final JHttpTunnelClient client = new JHttpTunnelClient (host, 80);
        client.ib = new InBoundURL (forwardPort);
        client.ob = new OutBoundURL (forwardPort);
        client.connect ();

        //hideous solution but there are issues with tunnel clients reading before anything
        //has been written. This will cause the tunnel clients to wait 2.5 seconds before attempting
        //to read from the tunnel.
        //todo: fix this
        readAfter = System.currentTimeMillis () + 2500;

        client.comm = new Communicator (client, "TestComm", false);
        Communicator.setGlobalDefaultWaitTime (Communicator.WAIT_FOREVER);

        return client.comm;
    }

    private JHttpTunnelClient (String host, int port) {
        this.dest_host = host;
        this.dest_port = port;
    }

    public void connect () throws JHttpTunnelException {
        String uid = Generator.makeString (40);

        if (ib == null) {
            throw new JHttpTunnelException ("InBound is not given");
        }
        ib.setUid (uid);
        ib.setHost (dest_host);
        ib.setPort (dest_port);

        if (ob == null) {
            throw new JHttpTunnelException ("OutBound is not given");
        }
        ob.setUid (uid);
        ob.setHost (dest_host);
        ob.setPort (dest_port);
        ob.setContentLength (CONTENT_LENGTH);

        try {
            getOutbound ();
            getInbound ();
        } catch (Exception e) {
            e.printStackTrace ();
        }
    }

    private void getOutbound () throws IOException {
        System.out.println ("getOutbound()");
        if (closed) {
            throw new IOException ("broken pipe");
        }
        ob.connect ();
        if (!init) {
            openChannel (1);
            init = true;
        }
    }

    private void getInbound () throws IOException {
        System.out.println ("getInbound()");
        ib.connect ();
    }

    public void openChannel (int i) throws IOException {
        command[0] = JHttpTunnel.TUNNEL_OPEN;
        command[1] = 0;
        command[2] = 1;
        command[3] = 0;
        ob.sendData (command, 0, 4, true);
    }

    public void sendDisconnect () throws IOException {
        //System.out.println("sendDisconnect: "+sendCount);
        command[0] = JHttpTunnel.TUNNEL_DISCONNECT;
        ob.sendData (command, 0, 1, true);
    }

    public void sendClose () throws IOException {
        //System.out.println("sendClose: ");
        command[0] = JHttpTunnel.TUNNEL_CLOSE;
        ob.sendData (command, 0, 1, true);
    }

    public void sendReadRequest (int readSize) throws IOException {
        System.out.println ("Sent read request for: " + readSize + " bytes");

        readSize = readSize + 5; //add 5 for command to go back

        byte[] command = new byte[5];
        command[0] = JHttpTunnel.TUNNEL_NEXT_READ;
        byte[] size = toIntByteArray (readSize);
        System.arraycopy (size, 0, command, 1, size.length);

        ob.sendData (command, 0, 5, true);
    }

    private int intFromByteArray (byte[] bytes) {
        return ByteBuffer.wrap (bytes).getInt ();
    }

    private int intFromByteArray (byte[] bytes, int offset, int length) {
        return ByteBuffer.wrap (bytes, offset, length).getInt ();
    }

    private byte[] toIntByteArray (int value) {
        return ByteBuffer.allocate (4).putInt (value).array ();
    }

    public void sendPad1 (boolean flush) throws IOException {
        command[0] = JHttpTunnel.TUNNEL_PAD1;
        ob.sendData (command, 0, 1, flush);
    }

    public void write (byte[] foo, int s, int l) throws IOException {
        //System.out.println("write: l="+l+", sendCount="+sendCount);

        if (l <= 0) {
            return;
        }

        if (ob.sendCount <= 4) {
            System.out.println ("ob.sendCount<=4: " + ob.sendCount);
            if (0 < ob.sendCount) {
                while (ob.sendCount > 1) {
                    sendPad1 (false);
                }
                //sendDisconnect();
            }
            getOutbound ();
        }

//        while ((ob.sendCount - 1 - 3) < l) {
//            int len = (ob.sendCount - 1 - 3);
//            command[0] = JHttpTunnel.TUNNEL_DATA;
//            command[1] = (byte) ((len >>> 8) & 0xff);
//            command[2] = (byte) (len & 0xff);
//            System.out.println ("send " + (len));
//            ob.sendData (command, 0, 3, true);
//            ob.sendData (foo, s, len, true);
//            s += len;
//            l -= len;
//
////      sendCount=1;
//            sendDisconnect ();
//            if (l > 0) {
//                getOutbound ();
//            }
//        }
        if (l <= 0) {
            return;
        }

        byte[] command = new byte[5];
        command[0] = JHttpTunnel.TUNNEL_DATA;

        byte[] size = toIntByteArray (l);
        System.arraycopy (size, 0, command, 1, size.length);

        ob.sendData (command, 0, 5, false);
        ob.sendData (foo, s, l, true);
    }

    public int read (byte[] foo, int s, int l) throws IOException {
        return readFull (foo, s, l, false);
    }

    public int readFull (byte[] foo, int s, int l, boolean readFull) throws IOException {
        while (System.currentTimeMillis () < readAfter) {
            try {
                Thread.sleep (250);
            } catch (InterruptedException ex) {
            }
        }
        if (closed) {
            return -1;
        }

        sendReadRequest (l - s);

        try {
            if (buf_len > 0) {
                int len = buf_len;
                if (l < buf_len) {
                    len = l;
                }
                int i = ib.receiveData (foo, s, len);
                buf_len -= i;
                return i;
            }

            int len = 0;
            byte[] dataBuff = null;
            while (!closed) {
                int i;

                if (readFull) {
                    int b = l + 5;
                    dataBuff = new byte[b];
                    l = dataBuff.length;
                    i = ib.receiveData (dataBuff, s, l);
                } else {
                    i = ib.receiveData (foo, s, 1);
                }

                if (i <= 0) {
                    return -1;
                }
                int request;
                if (readFull) {
                    request = dataBuff[s] & 0xff;
                } else {
                    request = foo[s] & 0xff;
                }
                System.out.println ("request: " + request);

                if (request == 2 || request == JHttpTunnel.TUNNEL_DATA) {
//                    if (readFull) {
                    ib.receiveData (foo, s, 4);
                    len = intFromByteArray (foo);
//                    } else {
//                        i = ib.receiveData (foo, s, 4);
//                        len = intFromByteArray (foo);
//                    }
                } else if ((request & JHttpTunnel.TUNNEL_SIMPLE) == 0) {
                    i = ib.receiveData (foo, s, 1);
                    len = (((foo[s]) << 8) & 0xff00);
                    i = ib.receiveData (foo, s, 1);
                    len = len | (foo[s] & 0xff);
                }
                System.out.println ("request: " + request);
                switch (request) {
                    case JHttpTunnel.TUNNEL_DATA:
//                        if (readFull) {
//                            System.arraycopy (dataBuff, 5, foo, 0, foo.length);
//                            return foo.length;
//                        }
                        //byte[] buffFoo = new byte[foo.length + 2];

                        buf_len = len;
                        System.out.println ("buf_len=" + buf_len);
                        if (l < buf_len) {
                            len = l;
                        }
                        int orgs = s;
                        while (len > 0) {
                            i = ib.receiveData (foo, s, len);
                            if (i < 0) {
                                break;
                            }
                            buf_len -= i;
                            s += i;
                            len -= i;
                        }

//                        for (int z = 0; z < foo.length; z++) {
//                            foo[z] = buffFoo[z];
//                        }
                        System.out.println ("receiveData: " + (s - orgs));
                        return s - orgs;
                    case JHttpTunnel.TUNNEL_PADDING:
                        ib.receiveData (null, 0, len);
                        continue;
                    case JHttpTunnel.TUNNEL_ERROR:
                        byte[] error = new byte[len];
                        ib.receiveData (error, 0, len);
                        System.out.println (new String (error, 0, len));
                        throw new IOException ("JHttpTunnel: " + new String (error, 0, len));
                    case JHttpTunnel.TUNNEL_PAD1:
                        continue;
                    case JHttpTunnel.TUNNEL_CLOSE:
                        closed = true;
                        close ();
                        System.out.println ("CLOSE");
                        break;
                    case JHttpTunnel.TUNNEL_DISCONNECT:
                        System.out.println ("DISCONNECT");
                        continue;
                    default:
                        System.out.println ("request=" + request);
                        System.out.println (Integer.toHexString (request & 0xff) + " " + new Character ((char) request));
                        throw new IOException ("JHttpTunnel: protocol error 0x" + Integer.toHexString (request & 0xff));
                }
            }
        } catch (IOException e) {
            System.out.println ("JHttpTunnelClient.read: " + e);
            return -1;
        } catch (Exception e) {
            e.printStackTrace ();
        }

        return -1;
    }

    public InputStream getInputStream () {
        if (in != null) {
            return in;
        }
        in = new InputStream ()
        {
            byte[] tmp = new byte[1];

            @Override
            public int read () throws IOException {
                int i = JHttpTunnelClient.this.read (tmp, 0, 1);
                return (i == -1 ? -1 : tmp[0]);
            }

            @Override
            public int read (byte[] foo) throws IOException {
                return JHttpTunnelClient.this.read (foo, 0, foo.length);
            }

            @Override
            public int read (byte[] foo, int s, int l) throws IOException {
                return JHttpTunnelClient.this.read (foo, s, l);
            }
        };

        return in;
    }

    public OutputStream getOutputStream () {
        if (out != null) {
            return out;
        }
        out = new OutputStream ()
        {
            final byte[] tmp = new byte[1];

            @Override
            public void write (int foo) throws IOException {
                tmp[0] = (byte) foo;
                JHttpTunnelClient.this.write (tmp, 0, 1);
            }

            @Override
            public void write (byte[] foo) throws IOException {
                JHttpTunnelClient.this.write (foo, 0, foo.length);
            }

            @Override
            public void write (byte[] foo, int s, int l) throws IOException {
                JHttpTunnelClient.this.write (foo, s, l);
            }
        };

        return out;
    }

    public void close () {
        System.out.println ("close");

        try {
            sendClose ();
        } catch (Exception e) {
        }

        try {
            ib.close ();
        } catch (Exception e) {
        }

        try {
            ob.close ();
        } catch (Exception e) {
        }

        closed = true;
    }

    public void setInBound (InBoundURL ib) {
        this.ib = ib;
    }

    public InBound getInBound () {
        return ib;
    }

    public void setOutBound (OutBoundURL ob) {
        this.ob = ob;
    }

    public OutBound getOutBound () {
        return ob;
    }

}
