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
package com.codebrig.beam.connection.traversal.punch.udp.server;

import com.codebrig.beam.Communicator;
import com.codebrig.beam.ConnectionStateListener;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class NATDeviceHolder
{

    private final ConcurrentHashMap<String, NATDevice> natDevices;

    public NATDeviceHolder () {
        this.natDevices = new ConcurrentHashMap<String, NATDevice> ();
    }

    public NATDevice getNATDevice (String peerIdentifier, String accessCode) {
        NATDevice natDevice = natDevices.get (peerIdentifier);

        if (natDevice != null && natDevice.validAccessCode (accessCode)) {
            return natDevice;
        }

        return null;
    }

    public void addNATDevice (final String peerIdentifier, final NATDevice natDevice) {
        if (!natDevices.contains (peerIdentifier)) {
            natDevices.put (peerIdentifier, natDevice);
        }

        Communicator comm = natDevice.getCommunicator ();
        if (comm.getAttribute ("nat_post_listener") == null) {
            comm.addConnectionStateListener (new ConnectionStateListener ()
            {
                @Override
                public void preConnection (Communicator comm) {
                }

                @Override
                public void postConnection (Communicator comm) {
                    //remove NAT device
                    removeNATDevice (peerIdentifier);
                }
            });

            comm.setAttribute ("nat_post_listener", "true");
        }
    }

    public void removeNATDevice (String peerIdentifier) {
        natDevices.remove (peerIdentifier);
    }

}
