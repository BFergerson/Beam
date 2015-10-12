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

import com.codebrig.beam.utils.ByteArrayQueue;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;

public class InBoundURL extends InBound
{

    private InputStream in = null;
    private URLConnection con = null;
    private static final Object lock = new Object ();
    private static final Object readLock = new Object ();
    private int backoffTime = 100;
    private String uid;
    private final ByteArrayQueue byteQueue = new ByteArrayQueue ();

    public InBoundURL (int forwardPort) {
        super (forwardPort);
    }

    public String getUid () {
        return uid;
    }

    public void setUid (String uid) {
        this.uid = uid;
    }

    public void connect () throws IOException {
        close ();
        String host = getHost ();
        int port = getPort ();
        URL url = new URL ("http://" + host + ":" + port + "/index.html?crap=1&SESSIONID=" + uid + "&fport=" + forwardPort);
        con = url.openConnection ();
        con.setUseCaches (false);
        con.setDoOutput (false);
        con.connect ();
        in = con.getInputStream ();
    }

    public int receiveData (byte[] buf, int s, int l) throws IOException {
        System.out.println ("receiveData: " + l);
        if (l <= 0) {
            System.out.println ("receiveData: " + l);
        }
        if (l <= 0) {
            return -1;
        }

        while (true) {
            if (con == null) {
                return -1;
            }

            try {
                if (buf == null) {
                    if (l <= 0) {
                        return -1;
                    }
                    long bar = in.skip ((long) l);
                    l -= bar;
                    continue;
                }

                int sizeTaken = 0;
                synchronized (readLock) {
                    int avail = in.available ();
                    System.out.println ("AVAILABLE: " + avail);

                    if (avail > l) {
                        byte[] dataBuf = new byte[avail];
                        int i = in.read (dataBuf, 0, avail);
                        System.out.println ("Add queue1: " + avail);
                        byteQueue.add (dataBuf, 0, i);
                    } else {
                        int i = in.read (buf, s, l);
                        if (i != -1) {
                            byteQueue.add (buf, s, i);
                            System.out.println ("Add queue2: " + i);
                        }
                    }

                    sizeTaken = byteQueue.length ();
                    System.out.println ("Queue size: " + sizeTaken);
                    if (sizeTaken == 0) {
                        System.out.println ("Queue empty");
                        sizeTaken = -1;
                    } else if (l <= sizeTaken) {
                        System.out.println ("Took: " + l);
                        sizeTaken = l;
                        byteQueue.remove (buf, s, l);
                    }
                }

                //System.out.println (Arrays.toString (byteQueue.array ()));
                if (sizeTaken > 0) {
                    resetBackoffTime ();
                    return sizeTaken;
                } else if (sizeTaken == -1) {
                    backoffTime = backoffTime + 250;//0;
                    rest (backoffTime);
                }
                //System.out.println ("1$ i=" + i);
                connect ();
            } catch (SocketException e) {
                throw e;
            } catch (IOException e) {
                throw e;
            }
        }
    }

    public void resetBackoffTime () {
        backoffTime = 250;

        synchronized (lock) {
            lock.notifyAll (); //wake self up
        }
    }

    private void rest (int time) {
        System.out.println ("Resting: " + time);
        synchronized (lock) {
            try {
                if (con != null) {
                    lock.wait (time);
                }
            } catch (InterruptedException ex) {
                //eat it
            }
        }
    }

    public void close () throws IOException {
        synchronized (lock) {
            lock.notifyAll (); //wake self up
        }

        if (con != null) {
            if (in != null) {
                try {
                    in.close ();
                } catch (IOException e) {
                }
            }
            con = null;
        }
    }

}
