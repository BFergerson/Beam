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
package com.codebrig.beam.transfer;

import com.codebrig.beam.BeamClient;
import com.codebrig.beam.BeamServer;
import com.codebrig.beam.Communicator;
import com.codebrig.beam.connection.raw.RawDataChannel;
import com.codebrig.beam.handlers.BasicHandler;
import com.codebrig.beam.messages.BasicMessage;
import com.codebrig.beam.messages.BeamMessage;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class TestFileTransfer
{

    public final static int TEST_PORT = 4444;
    public final static int TEST_MESSAGE = 1;

    private static BeamServer server;

    public static void main (String[] args) throws IOException {
        //disables timeouts; useful when debugging
        Communicator.setGlobalDefaultWaitTime (Communicator.WAIT_FOREVER);

        //start server
        startServer ();

        //start client
        BeamClient client = startClient ();

        RawDataChannel rawChannel = client.getCommunicator ().createRawDataChannel ();
        BasicMessage message = new BasicMessage (TEST_MESSAGE)
                .setLong ("channel_id", rawChannel.getRawChannelId ());
        BeamMessage responseMessage = client.getCommunicator ().send (message);
        message = new BasicMessage (responseMessage);

        rawChannel.connect (message.getLong ("channel_id"));
        FileTransferChannel ftc = new FileTransferChannel (rawChannel);
        ftc.sendFile ("C:\\temp\\send_file.txt");
        ftc.close ();

        //and we're done
        server.close ();
        System.exit (0);
    }

    private static BeamClient startClient () throws IOException {
        BeamClient client = new BeamClient ("localhost", TEST_PORT);
        client.connect ();

        return client;
    }

    private static void startServer () {
        server = new BeamServer ("Test Server", TEST_PORT);
        server.start ();

        //add handler to accept client's test message
        server.addGlobalHandler (new BasicHandler (TEST_MESSAGE)
        {

            @Override
            public BeamMessage messageRecieved (Communicator comm, BasicMessage message) {
                //user wants to transfer a file. establish file transfer channel
                RawDataChannel rawChannel = comm.createRawDataChannel ();
                rawChannel.connect (message.getLong ("channel_id"));

                message.clear ();
                message.setSuccessful (true).setLong ("channel_id", rawChannel.getRawChannelId ());
                comm.queue (message);

                FileTransferChannel fileChannel = new FileTransferChannel (rawChannel);

                try {
                    fileChannel.receiveFile (new RandomAccessFile ("C:\\temp\\receive_file.txt", "rw"));
                    fileChannel.close ();
                } catch (TransferException | IOException ex) {
                    ex.printStackTrace ();
                }

                return null;
            }
        });
    }

}
