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
package com.codebrig.beam.crypt;

import com.codebrig.beam.Communicator;
import com.codebrig.beam.ConnectionStateListener;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class RSAConnectionHolder
{

    private final static ConcurrentHashMap<Long, ConcurrentHashMap<String, RSAConnection>> connections
            = new ConcurrentHashMap<Long, ConcurrentHashMap<String, RSAConnection>> ();

    public RSAConnectionHolder () {
        //empty constructor
    }

    public static RSAConnection getRSAConnection (long communicatorUID, String connectionKey) {
        ConcurrentHashMap<String, RSAConnection> rsaConnections = connections.get (communicatorUID);

        if (rsaConnections != null) {
            return rsaConnections.get (connectionKey);
        }

        return null;
    }

    public static void addRSAConnection (Communicator comm, RSAConnection rsaConnection) {
        ConcurrentHashMap<String, RSAConnection> rsaConnections = connections.get (comm.getUID ());

        if (rsaConnections == null) {
            ConcurrentHashMap<String, RSAConnection> userConnections
                    = new ConcurrentHashMap<String, RSAConnection> ();
            userConnections.put (rsaConnection.getSession (), rsaConnection);
            connections.put (comm.getUID (), userConnections);
        } else {
            if (!rsaConnections.contains (rsaConnection.getSession ())) {
                rsaConnections.put (rsaConnection.getSession (), rsaConnection);
            }
        }

        if (comm.getAttribute ("rsa_post_listener") == null) {
            comm.addConnectionStateListener (new ConnectionStateListener ()
            {
                @Override
                public void preConnection (Communicator comm) {
                }

                @Override
                public void postConnection (Communicator comm) {
                    //remove rsa connections
                    removeAllRSACOnnections (comm.getUID ());
                }
            });

            comm.setAttribute ("rsa_post_listener", "true");
        }
    }

    public static void removeAllRSACOnnections (long communicatorUID) {
        connections.remove (communicatorUID);
    }

}
