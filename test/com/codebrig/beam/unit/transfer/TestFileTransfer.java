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
package com.codebrig.beam.unit.transfer;

import com.codebrig.beam.BeamClient;
import com.codebrig.beam.BeamServer;
import com.codebrig.beam.Communicator;
import com.codebrig.beam.handlers.BasicHandler;
import com.codebrig.beam.messages.BasicMessage;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.transfer.FileTransferChannel;
import java.io.File;
import java.io.IOException;

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

        FileTransferChannel fileChannel = client.getCommunicator ().createFileTransferChannel ();
        BasicMessage message = new BasicMessage (TEST_MESSAGE)
                .setLong ("channel_id", fileChannel.getTransferChannelId ());
        BeamMessage responseMessage = client.getCommunicator ().send (message);
        message = new BasicMessage (responseMessage);

        fileChannel.connect (message.getLong ("channel_id"));
        fileChannel.sendFile (new File ("C:\\temp\\send_file.txt"));
        fileChannel.close ();

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
            public BeamMessage messageReceived (Communicator comm, BasicMessage message) {
                //user wants to transfer a file. establish file transfer channel
                FileTransferChannel fileChannel = comm.createFileTransferChannel ();
                fileChannel.connect (message.getLong ("channel_id"));

                message.clear ();
                message.setSuccessful (true).setLong ("channel_id", fileChannel.getTransferChannelId ());
                comm.queue (message);

                try {
                    fileChannel.receiveFile (new File ("C:\\temp\\receive_file_" + System.currentTimeMillis () + ".txt"));
                    fileChannel.close ();
                } catch (IOException ex) {
                    ex.printStackTrace ();
                }

                return null;
            }
        });
    }

}
