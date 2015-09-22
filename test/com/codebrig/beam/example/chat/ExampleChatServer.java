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

import com.codebrig.beam.BeamServer;
import com.codebrig.beam.Communicator;
import com.codebrig.beam.handlers.BeamHandler;
import com.codebrig.beam.messages.BeamMessage;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class ExampleChatServer
{

    private final static int CHAT_SERVER_PORT = 4444;
    private static BeamServer server;

    public static void main (String[] args) {
        System.out.println ("Example chat server starting...");

        server = new BeamServer (CHAT_SERVER_PORT);
        server.addHandler (ExampleServerChatMessageHandler.class);
        server.setDebugOutput (true); //debug output on
        server.start ();

        System.out.println ("Example chat server started!");
    }

    public static class ExampleServerChatMessageHandler extends BeamHandler<ExampleChatMessage>
    {

        public ExampleServerChatMessageHandler () {
            super (ExampleChatMessage.CHAT_MESSAGE_ID);
        }

        @Override
        public ExampleChatMessage messageReceived (Communicator comm, ExampleChatMessage message) {
            //got chat message from client
            System.out.println ("Server received client chat message: " + message.getMessage ());

            //send to other clients connected to server
            server.broadcast (message);
            return null;
        }

        @Override
        public ExampleChatMessage castMessage (BeamMessage message) {
            return new ExampleChatMessage (message);
        }
    }

}
