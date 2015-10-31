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
package com.codebrig.beam.connection.traversal.punch.udp.server.handlers;

import com.codebrig.beam.Communicator;
import com.codebrig.beam.connection.traversal.punch.udp.messages.UDPPunchMessage;
import static com.codebrig.beam.connection.traversal.punch.udp.messages.UDPPunchMessageType.CONNECTION_CONNECT_MESSAGE;
import static com.codebrig.beam.connection.traversal.punch.udp.messages.UDPPunchMessageType.CONNECTION_LISTEN_MESSAGE;
import com.codebrig.beam.connection.traversal.punch.udp.server.NATDevice;
import com.codebrig.beam.connection.traversal.punch.udp.server.NATDeviceHolder;
import com.codebrig.beam.connection.traversal.punch.udp.server.UDPPunchServer;
import com.codebrig.beam.handlers.LegacyHandler;
import com.codebrig.beam.messages.LegacyMessage;

/**
 *
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class ConnectionHandler extends LegacyHandler
{

    private UDPPunchServer punchServer;

    public ConnectionHandler () {
        super (CONNECTION_LISTEN_MESSAGE, CONNECTION_CONNECT_MESSAGE);
    }

    @Override
    public LegacyMessage messageReceived (Communicator comm, LegacyMessage message) {
        UDPPunchMessage msg = new UDPPunchMessage (message);
        String peerIdentifier = msg.getPeerIdentifier ();
        String accessCode = msg.getAccessCode ();

        if (message.getType () == CONNECTION_LISTEN_MESSAGE) {
            NATDevice device = new NATDevice (comm);
            device.addConnection (accessCode, 0); //no port neccessary as we will do the punching

            NATDeviceHolder deviceHolder = punchServer.getNATDeviceHolder ();
            deviceHolder.addNATDevice (peerIdentifier, device);
        } else {
            String requestPeerIdentifier = msg.getRequestPeerIdentifier ();

            NATDeviceHolder deviceHolder = punchServer.getNATDeviceHolder ();
            NATDevice device = deviceHolder.getNATDevice (requestPeerIdentifier, accessCode);

            if (device != null) {
                device.getCommunicator ().queue (msg);
            }
        }

        return null;
    }

    @Override
    public void passObject (Object passObject) {
        punchServer = (UDPPunchServer) passObject;
    }
}
