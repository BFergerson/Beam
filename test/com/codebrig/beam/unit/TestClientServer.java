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
package com.codebrig.beam.unit;

import com.codebrig.beam.BeamClient;
import com.codebrig.beam.BeamServer;
import com.codebrig.beam.Communicator;
import com.codebrig.beam.handlers.BasicHandler;
import com.codebrig.beam.messages.BasicMessage;
import com.codebrig.beam.messages.BeamMessage;
import java.io.IOException;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class TestClientServer
{

    public final static int TEST_PORT = 4444;
    public final static int TEST_MESSAGE = 1;

    private static BeamServer server;

    public static void main (String[] args) throws IOException {
        //disables timeouts; useful when debugging
        Communicator.setGlobalDefaultWaitTime (Communicator.WAIT_FOREVER);

        //start server
        startServer ();

        //start up multiple clients
        for (int i = 0; i < 10; i++) {
            BeamClient client = startClient ();
            sendMessageToServer (client);
        }

        //broadcast message to all clients from server
        broadcastMessage ();

        //and we're done
        server.close ();
        System.exit (0);
    }

    private static BeamClient startClient () throws IOException {
        BeamClient client = new BeamClient ("localhost", TEST_PORT);
        client.connect ();

        //add broadcast handler
        client.getCommunicator ().addHandler (new TestClientBroadcastHandler ());

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
                System.out.println ("Received message from client: " + message.getString ("client_message"));

                //clear and add response
                message.clear ();
                message.setString ("server_response", "Hello from server!");

                return message;
            }
        });
    }

    private static void sendMessageToServer (BeamClient client) {
        BasicMessage message = new BasicMessage (TEST_MESSAGE);
        message.setString ("client_message", "Hello from client!");
        
        BeamMessage responseMessage = client.getCommunicator ().send (message);
        BasicMessage responseBasicMessage = new BasicMessage (responseMessage);
        System.out.println ("Received message from server: " + responseBasicMessage.getString ("server_response"));
    }

    private static void broadcastMessage () {
        BasicMessage message = new BasicMessage (TEST_MESSAGE);
        message.setString ("broadcast_message", "Hello everyone from server!");

        server.broadcast (message);
    }

}
