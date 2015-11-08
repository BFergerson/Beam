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
import com.codebrig.beam.connection.traversal.punch.udp.messages.UDPPunchMessageType;
import com.codebrig.beam.connection.traversal.punch.udp.server.NATDevice;
import com.codebrig.beam.connection.traversal.punch.udp.server.UDPPunchServer;
import com.codebrig.beam.handlers.BeamHandler;
import com.codebrig.beam.messages.BeamMessage;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class PunchRequestHandler extends BeamHandler
{

    private UDPPunchServer punchServer;

    public PunchRequestHandler () {
        super (UDPPunchMessageType.PUNCH_REQUEST_MESSAGE);
    }

    @Override
    public BeamMessage messageReceived (Communicator comm, BeamMessage message) {
        UDPPunchMessage punchMessage = new UDPPunchMessage (message);
        String peerIdentifier = punchMessage.getPeerIdentifier ();
        String accessCode = punchMessage.getAccessCode ();
        int listenPort = punchMessage.getListenPort ();

        NATDevice natDevice = punchServer.getNATDeviceHolder ().getNATDevice (peerIdentifier, accessCode);
        if (natDevice == null) {
            punchMessage.clear ();
            return punchMessage.setSuccessful (false);
        }

        //alert user who created hole of punch
        Communicator holeComm = natDevice.getCommunicator ();
        punchMessage.setIpAddress (comm.getHostIPAddress ());
        punchMessage.setListenPort (listenPort);
        punchMessage.setSuccessful (true);
        holeComm.queue (punchMessage);

        //let puncher know who to connect to
        punchMessage.clear ();
        punchMessage.setIpAddress (holeComm.getHostIPAddress ());
        punchMessage.setListenPort (natDevice.getConnectionPort (accessCode));
        System.out.println (String.format ("Peer punched hole - IP: %s Port: %s to Peer: %s Access: %s IP:%s Port: %s",
                comm.getHostIPAddress (), listenPort, peerIdentifier, accessCode, punchMessage.getIpAddress (), punchMessage.getListenPort ()));
        return punchMessage.setSuccessful (true);
    }

    @Override
    public void passObject (Object passObject) {
        punchServer = (UDPPunchServer) passObject;
    }

}
