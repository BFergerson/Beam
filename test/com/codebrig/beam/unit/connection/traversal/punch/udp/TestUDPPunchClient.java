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
package com.codebrig.beam.unit.connection.traversal.punch.udp;

import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.Communicator;
import static com.codebrig.beam.unit.connection.traversal.punch.udp.TestUDPPunchServer.*;
import com.codebrig.beam.connection.traversal.punch.udp.client.UDPPunchClient;
import com.codebrig.beam.messages.BasicMessage;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class TestUDPPunchClient
{

    public static void main (String[] args) throws Exception {
        String serverHost;
        int serverPort;
        if (args.length < 2) {
            System.out.println ("Please input punch server host and port! ex. 127.0.0.1 33333");
            return;
        } else {
            serverHost = args[0];
            serverPort = Integer.parseInt (args[1]);
        }

        //for debugging purposes
        Communicator.setGlobalDefaultWaitTime (Communicator.WAIT_FOREVER);

        UDPPunchClient punchClient = new UDPPunchClient (serverHost, serverPort);
        Communicator peerComm = punchClient.punchPeerCommunicator (
                TEST_PUNCH_PEER_IDENTIFIER, TEST_PUNCH_PEER_ACCESS_CODE);

        if (peerComm == null) {
            System.out.println ("Could not find peer to connect to! Make sure peer is waiting with hole request...");
            return;
        }

        //handshake
        peerComm.performBeamHandshake ();

        //send test message
        BasicMessage message = new BasicMessage (TEST_PUNCH_MESSAGE_TYPE);
        message.setString ("test_message", "test_data");
        BeamMessage rtnMessage = peerComm.send (message);
        message = new BasicMessage (rtnMessage);

        //verify response
        assert (message.getString ("response_message").equals ("response_data"));
        System.out.println ("UDP punch client response successful!");

        //clean up
        Thread.sleep (500);
        peerComm.close ();
    }

}
