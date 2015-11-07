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
package com.codebrig.beam.connection.traversal.punch.udp.client;

import com.codebrig.beam.BeamClient;
import com.codebrig.beam.Communicator;
import com.codebrig.beam.connection.traversal.punch.udp.messages.UDPPunchMessage;
import com.codebrig.beam.connection.traversal.punch.udp.messages.UDPPunchMessageType;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.utils.Generator;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import net.rudp.ReliableSocket;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class UDPHoleClient
{

    private final String serverHost;
    private final int serverPort;
    private final static byte[] MESSAGE2 = "MSG_MSG2".getBytes ();
    private final static byte[] MESSAGE3 = "MSG_MSG3".getBytes ();

    private final DatagramSocket socket;

    public UDPHoleClient (String serverHost, int serverPort) throws SocketException {
        this.serverHost = serverHost;
        this.serverPort = serverPort;

        socket = new DatagramSocket ();
    }

    public Communicator createHoleCommunicator (String peerIdentifier, String accessCode)
            throws IOException {
        return createHoleCommunicator (peerIdentifier, accessCode, null, Communicator.WAIT_FOREVER);
    }

    public Communicator createHoleCommunicator (String peerIdentifier, String accessCode, int waitTime)
            throws IOException {
        return createHoleCommunicator (peerIdentifier, accessCode, null, waitTime);
    }

    public Communicator createHoleCommunicator (String peerIdentifier, String accessCode, String requestPeerIdentifier)
            throws IOException {
        return createHoleCommunicator (peerIdentifier, accessCode, requestPeerIdentifier, Communicator.WAIT_FOREVER);
    }

    public Communicator createHoleCommunicator (String peerIdentifier, String accessCode,
            String requestPeerIdentifier, int waitTime)
            throws IOException {
        BeamClient c = new BeamClient (serverHost, serverPort);
        c.connect ();

        Communicator comm = c.getCommunicator ();
        UDPPunchMessage holeMessage = new UDPPunchMessage (UDPPunchMessageType.HOLE_REQUEST_MESSAGE,
                peerIdentifier, accessCode, socket.getLocalPort ());

        BeamMessage respMessage = comm.send (holeMessage);

        if (respMessage.isSuccessful ()) {
            if (requestPeerIdentifier != null) {
                //let server know we are going to be waiting for punch request
                UDPPunchMessage connectMessage = new UDPPunchMessage (UDPPunchMessageType.CONNECTION_CONNECT_MESSAGE,
                        peerIdentifier, accessCode, socket.getLocalPort ());
                connectMessage.setRequestPeerIdentifier (requestPeerIdentifier);

                c.getCommunicator ().queue (connectMessage);
            }

            //wait for a punch request
            BeamMessage message = c.getCommunicator ().fetchWithWait (
                    waitTime, UDPPunchMessageType.PUNCH_REQUEST_MESSAGE);

            //done with server
            c.close ();

            if (message == null) {
                //unknown (should never happen)
                return null;
            }

            holeMessage = new UDPPunchMessage (message);
            if (holeMessage.isSuccessful ()) {
                String remoteIP = holeMessage.getIpAddress ();
                int remotePort = holeMessage.getListenPort ();

                boolean successfulPunch = false;
                socket.setSoTimeout (2500);
                for (int i = 0; i < 3; i++) {
                    try {
                        successfulPunch = socketPunch (remoteIP, remotePort);
                        if (successfulPunch) {
                            break;
                        }
                    } catch (SocketTimeoutException ex) {
                        //ex.printStackTrace ();
                        System.out.println ("Socket hole punch failed! Trying again...");
                    }
                }
                socket.setSoTimeout (0);

                if (!successfulPunch) {
                    return null;
                }

                ReliableSocket relSocket = new ReliableSocket (socket);
                relSocket.setSoTimeout (5000);
                relSocket.connect (new InetSocketAddress (remoteIP, remotePort));

                Communicator peerComm = new Communicator (relSocket, "UDP Hole Client", false);
                peerComm.performBeamHandshake ();
                return peerComm;
            }
        } else {
            //done with server
            c.close ();
        }

        return null;
    }

    private boolean socketPunch (String remoteIP, int remotePort) throws IOException {
        long ident = Generator.randomLong ();
        if (ident < 0) {
            ident = ident * -1;
        }
        byte[] MESSAGE1 = toBytes (ident);
        System.out.println ("Phase 1 established!");
        sendPacket (remoteIP, remotePort, MESSAGE1);

        byte[] buf = new byte[MESSAGE2.length];
        receivePacket (buf);
        long num = fromBytes (buf);
        if (num >= 0) {
            //got their number which means my number was lost.
            //send them our number again
            sendPacket (remoteIP, remotePort, MESSAGE1);
        }

        if (Arrays.equals (buf, MESSAGE2)) {
            //connection good send msg3
            System.out.println ("Phase 2 established!");
            sendPacket (remoteIP, remotePort, MESSAGE3);
            receivePacket (buf);

            if (Arrays.equals (buf, MESSAGE3)) {
                //connection established!
                System.out.println ("Hole punch connection established!");
                return true;
            }
        } else if (Arrays.equals (buf, MESSAGE3)) {
            //connection established!
            System.out.println ("Hole punch connection established!");
            return true;
        } else {
            while (true) {
                //message 1
                if (num < ident) {
                    receivePacket (buf);
                    if (Arrays.equals (buf, MESSAGE2)) {
                        //connection good send msg3
                        System.out.println ("Phase 2 established!");
                        sendPacket (remoteIP, remotePort, MESSAGE3);

                        //connection established!
                        System.out.println ("Hole punch connection established!");
                        return true;
                    } else if (Arrays.equals (buf, MESSAGE3)) {
                        //connection established!
                        System.out.println ("Hole punch connection established!");
                        return true;
                    } else {
                        //client sent number again
                        num = fromBytes (buf);
                    }
                } else {
                    sendPacket (remoteIP, remotePort, MESSAGE2);
                    receivePacket (buf);

                    if (Arrays.equals (buf, MESSAGE3)) {
                        System.out.println ("Phase 2 established!");

                        //connection established!
                        System.out.println ("Hole punch connection established!");
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void sendPacket (String destAddress, int destPort, byte[] data) throws IOException {
        DatagramPacket packet = new DatagramPacket (data, data.length,
                InetAddress.getByName (destAddress), destPort);
        socket.send (packet);
    }

    private void receivePacket (byte[] data) throws IOException {
        DatagramPacket packet = new DatagramPacket (data, data.length);
        socket.receive (packet);
    }

    private static byte[] toBytes (long value) {
        return ByteBuffer.allocate (8).putLong (value).array ();
    }

    private long fromBytes (byte[] bytes) {
        return ByteBuffer.wrap (bytes).getLong ();
    }

}
