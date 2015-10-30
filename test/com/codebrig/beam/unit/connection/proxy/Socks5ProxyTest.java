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
package com.codebrig.beam.unit.connection.proxy;

import com.codebrig.beam.utils.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * todo: convert to using Beam client (no handshake) and get raw streams
 *
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class Socks5ProxyTest
{

    private static final int PROXY_TIMEOUT_INTERVAL = 10 * 1000; //10 seconds

    public static void main (String[] args) throws UnknownHostException, IOException {
        //non-proxy connect
        System.out.println ("Connecting without proxy...\n");
        Socket sock = new Socket ();
        printIPInformation (sock);
        System.out.println ("\nFinished connection without proxy!\n");

        //proxy settings
        String proxyHost = "";
        int proxyPort = -1;
        String proxyUsername = null;
        String proxyPassword = null;

        Proxy proxy = new Proxy (Proxy.Type.SOCKS, InetSocketAddress.createUnresolved (proxyHost, proxyPort));
        final Socket proxySock = new Socket (proxy);
        if (proxyPassword != null && proxyPassword.isEmpty ()) {
            Authenticator.setDefault (new AuthenticatorImpl (proxy.type (), proxyUsername, proxyPassword));
        } else {
            Authenticator.setDefault (null);
        }

        //proxy connect
        System.out.println ("\nConnecting with proxy...\n");
        printIPInformation (proxySock);
        System.out.println ("\nFinished connection with proxy!\n");

        //done
        System.exit (0);
    }

    private static void printIPInformation (Socket sock) throws IOException {
        try {
            sock.connect (new InetSocketAddress (InetAddress.getByName ("wtfismyip.com"), 80), PROXY_TIMEOUT_INTERVAL);

            PrintWriter pw = new PrintWriter (sock.getOutputStream ());
            pw.println ("GET /text HTTP/1.1");
            pw.println ("Host: wtfismyip.com");
            pw.println ("");
            pw.flush ();

            try (BufferedReader br = new BufferedReader (new InputStreamReader (sock.getInputStream ()))) {
                while (!br.ready ()) {
                    //wait
                }

                String text;
                while (br.ready () && (text = br.readLine ()) != null) {
                    System.out.println (text);
                }
            }
        } finally {
            sock.close ();
        }
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
