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
package com.codebrig.beam.unit.connection.raw;

import com.codebrig.beam.BeamClient;
import com.codebrig.beam.BeamServer;
import com.codebrig.beam.Communicator;
import com.codebrig.beam.connection.raw.RawDataChannel;
import com.codebrig.beam.handlers.BasicHandler;
import com.codebrig.beam.messages.BasicMessage;
import com.codebrig.beam.messages.BeamMessage;
import java.io.IOException;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class BeamRawTest
{

    public final static int TEST_PORT = 4444;
    public final static int RAW_SOCKET_INIT_MESSAGE = 1;

    private static BeamServer server;
    private static BeamClient client;

    public static void main (String[] args) throws IOException {
        //disables timeouts; useful when debugging
        Communicator.setGlobalDefaultWaitTime (Communicator.WAIT_FOREVER);

        startServer ();
        startClient ();

        //start transfer
        RawDataChannel clientRawSocket = client.getCommunicator ().createRawDataChannel ();

        BasicMessage message = new BasicMessage (RAW_SOCKET_INIT_MESSAGE);
        message.setLong ("raw_channel_id", clientRawSocket.getRawChannelId ());
        BeamMessage responseMessage = client.getCommunicator ().send (message);
        message = new BasicMessage (responseMessage);

        clientRawSocket.connect (message.getLong ("raw_channel_id"));

        clientRawSocket.getOutputStream ().write ("TEST RAW DATA".getBytes ());

        //clean up
        client.close ();
        server.close ();
    }

    private static void startClient () throws IOException {
        client = new BeamClient ("localhost", TEST_PORT);
        client.connect ();
    }

    private static void startServer () {
        server = new BeamServer ("Test Server", TEST_PORT);
        server.start ();

        server.addGlobalHandler (new BasicHandler (RAW_SOCKET_INIT_MESSAGE)
        {

            @Override
            public BeamMessage messageReceived (Communicator comm, BasicMessage message) {
                Long channelId = message.getLong ("raw_channel_id");
                RawDataChannel rawSocket = comm.createRawDataChannel ();
                rawSocket.connect (channelId);

                message.setLong ("raw_channel_id", rawSocket.getRawChannelId ());
                comm.queue (message);

                byte[] buff = new byte[13]; //13 = size of "TEST RAW DATA"
                try {
                    rawSocket.getInputStream ().read (buff);
                } catch (IOException ex) {
                }

                System.out.println ("Received raw data: " + new String (buff));
                return null;
            }
        });
    }

}
