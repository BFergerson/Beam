/*
 * Copyright (c) 2014-2015 CodeBrig, LLC.
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
package com.codebrig.beam.example.chat;

import com.codebrig.beam.BeamClient;
import com.codebrig.beam.Communicator;
import com.codebrig.beam.handlers.BeamHandler;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.utils.Generator;
import java.io.IOException;
import java.util.Scanner;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class ExampleChatClient
{

    private final static String CHAT_SERVER_HOST = "localhost";
    private final static int CHAT_SERVER_PORT = 4444;
    private final static String CHAT_CLIENT_USERNAME = "User #" + Generator.makeString (7);

    public static void main (String[] args) throws IOException {
        BeamClient client = new BeamClient (CHAT_SERVER_HOST, CHAT_SERVER_PORT);
        client.connect ();
        client.addHandler (ExampleChatMessageHandler.class);

        //loop taking user input for sending messages
        System.out.println ("Connected to chat server! Type /exit to finish or anything else to send chat message.");
        Scanner in = new Scanner (System.in);
        while (true) {
            String message = in.nextLine ().trim ();
            if (message.equalsIgnoreCase ("/exit")) {
                //finished
                client.close ();
                break;
            } else if (!message.isEmpty ()) {
                //send chat message
                ExampleChatMessage chatMessage = new ExampleChatMessage ();
                chatMessage.setUsername (CHAT_CLIENT_USERNAME);
                chatMessage.setMessage (message);
                client.queueMessage (chatMessage);
            }
        }
    }

    public static class ExampleChatMessageHandler extends BeamHandler<ExampleChatMessage>
    {

        public ExampleChatMessageHandler () {
            super (ExampleChatMessage.CHAT_MESSAGE_ID);
        }

        @Override
        public ExampleChatMessage messageReceived (Communicator comm, ExampleChatMessage message) {
            if (!message.getUsername ().equalsIgnoreCase (CHAT_CLIENT_USERNAME)) {
                //we have a chat message from someone else
                System.out.println (message.getUsername () + ": " + message.getMessage ());
            }

            return null;
        }

        @Override
        public ExampleChatMessage castMessage (BeamMessage message) {
            return new ExampleChatMessage (message);
        }
    }

}
