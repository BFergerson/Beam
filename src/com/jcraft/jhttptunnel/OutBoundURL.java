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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;

public class OutBoundURL extends OutBound
{

    private InputStream in = null;
    private OutputStream out = null;
    private URLConnection con = null;
    private final byte[] _TUNNEL_DISCONNECT = {(byte) 0x47};
    private String uid;

    public OutBoundURL (int forwardPort) {
        super (forwardPort);
    }

    public void setUid (String uid) {
        this.uid = uid;
    }

    public String getUid () {
        return uid;
    }

    public void connect () throws IOException {
        close ();

        String host = getHost ();
        int port = getPort ();

        URL url = new URL ("http://" + host + ":" + port + "/index.html?crap=1&SESSIONID=" + uid + "&fport=" + forwardPort);
        con = url.openConnection ();
        con.setUseCaches (false);
        con.setDoOutput (true);
        con.setRequestProperty ("Connection", "none");
        out = con.getOutputStream ();
        sendCount = getContentLength ();
    }

    public void sendData (byte[] foo, int s, int l, boolean flush) throws IOException {
        System.out.println ("sendData: l=" + l + " sendCount=" + sendCount + " flush=" + flush);
        if (l <= 0) {
            return;
        }

        if (con == null) {
            connect ();
        }

        if (sendCount <= 0) {
            connect ();
        }

        int retry = 2;
        while (retry > 0) {
            try {
                System.out.println ("write l=" + l);
                if (out == null) {
                    break;
                }
                out.write (foo, s, l);
                sendCount -= l;
                if (flush) {
                    if (sendCount > 0) {
                        //out.write(_TUNNEL_DISCONNECT, 0, 1);
                    }
                    out.flush ();
                    out.close ();
                    out = null;
                    if (con != null) {
                        in = con.getInputStream ();
                    }
                    close ();

                    sendCount = 0;
                    return;
                }
                return;
            } catch (SocketException e) {
                System.out.println ("2# " + e + " " + l + " " + flush);
                throw e;
//connect();
            } catch (IOException e) {
//                e.printStackTrace ();
////System.out.println("2# "+e+" "+l+" "+flush);
//                System.out.println ("2# " + e);
//                connect ();
                throw e;
            }
            //retry--;
        }
    }

    public void close () throws IOException {
        //System.out.println(this+".close() con="+con+" in="+in);
        if (con != null) {
            if (out != null) {
                try {
                    out.close ();
                    out = null;
                } catch (IOException e) {
                }
            }
            if (in != null) {
                try {
                    /*
                     while(true){
                     int c=in.read();
                     if(c==-1)break;
                     //	    System.out.println("c="+c);
                     }
                     */
                    in.close ();
                    in = null;
                } catch (IOException e) {
                }
            }
            con = null;
        }
//    System.out.println("close() done");
    }

}
