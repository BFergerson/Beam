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
package com.codebrig.beam.unit.crypt;

import com.codebrig.beam.crypt.handlers.RSAHandshakeHandler;
import com.codebrig.beam.crypt.messages.RSABeamMessage;
import com.codebrig.beam.crypt.messages.AESBeamMessage;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.BeamClient;
import com.codebrig.beam.Communicator;
import com.codebrig.beam.BeamServer;
import com.codebrig.beam.crypt.AES;
import com.codebrig.beam.crypt.RSA;
import com.codebrig.beam.crypt.RSAConnection;
import java.io.IOException;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class BeamCryptTest
{

    public final static AES aes = new AES ("password");

    public final static RSA serverRSA = new RSA (1024);

    public final static int CRYPT_TEST_PORT = 4444;

    public final static int AES_CRYPT_TEST_MESSAGE = 1;
    public final static int RSA_CRYPT_TEST_MESSAGE = 2;

    private static BeamServer server;
    private static BeamClient client;

    public static void main (String[] args) throws IOException {
        //disables timeouts; useful when debugging
        Communicator.setGlobalDefaultWaitTime (Communicator.WAIT_FOREVER);

        //start server/client
        startServer ();
        startClient ();

        //run tests
        doTest ();
    }

    public static void startClient () throws IOException {
        client = new BeamClient ("localhost", "Test Client", CRYPT_TEST_PORT);
        client.connect ();
    }

    public static void startServer () {
        server = new BeamServer ("Test Server", CRYPT_TEST_PORT);
        server.addHandler (AESTestHandler.class);
        server.addHandler (RSATestHandler.class);
        server.setPingPongEnabled (false);

        server.addRSAHandshakeHandler (new RSAHandshakeHandler (serverRSA));

        server.start ();
    }

    public static void doTest () {
        //check crypt sending/receiving
        checkAES ();
        checkRSA ();

        //and we're done
        server.close ();
        client.close ();
        System.exit (0);
    }

    private static void checkAES () {
        BeamMessage sendMessage = new AESBeamMessage (
                aes, AES_CRYPT_TEST_MESSAGE);

        //set message data
        sendMessage.set ("aes_test_variable", "aes_test_value");

        //send and recieve response
        BeamMessage responseMessage = client.getCommunicator ().send (sendMessage);

        //check response data
        assert (responseMessage.get ("aes_response_variable").equals ("aes_response_value"));
        System.out.println ("AES response message valid!");
    }

    private static void checkRSA () {
        RSAConnection rsaConn = client.establishRSAConnection (serverRSA);

        BeamMessage sendMessage = new RSABeamMessage (
                rsaConn, RSA_CRYPT_TEST_MESSAGE);

        //set message data
        sendMessage.set ("rsa_test_variable", "rsa_test_value");

        //send and recieve response
        BeamMessage responseMessage = client.getCommunicator ().send (sendMessage);

        //check response data
        assert (responseMessage.get ("rsa_response_variable").equals ("rsa_response_value"));
        System.out.println ("RSA response message valid!");
    }

}
