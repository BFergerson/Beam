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
package com.codebrig.beam.crypt.handlers;

import com.codebrig.beam.Communicator;
import com.codebrig.beam.crypt.CryptException;
import com.codebrig.beam.crypt.RSAConnection;
import com.codebrig.beam.crypt.RSAConnectionHolder;
import com.codebrig.beam.crypt.messages.RSABeamMessage;
import com.codebrig.beam.handlers.BeamHandler;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.LegacyMessage;
import com.codebrig.beam.messages.SystemMessage;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public abstract class RSABeamHandler extends BeamHandler
{

    public RSABeamHandler (int... types) {
        super (types);
    }

    @Override
    public BeamMessage processIncomingMessage (Communicator comm, BeamMessage message) {
        LegacyMessage basicMessage = new LegacyMessage (message);
        String session = basicMessage.getString ("beam_rsas");
        byte[] messageData = basicMessage.getBytes ("beam_rsamd");
        String rawData = basicMessage.getString ("beam_rsa_raw");
        if (session == null || messageData == null || rawData == null) {
            throw new CryptException ("Invalid RSA connection!");
        }

        RSAConnection conn = RSAConnectionHolder.getRSAConnection (comm.getUID (), session);
        if (conn == null) {
            throw new CryptException ("Invalid RSA connection!");
        }

        byte[] decryptedData = conn.getAES ().decrypt (messageData);
        return new SystemMessage (message.getType (), decryptedData, message.isSystemMessage (),
                Boolean.valueOf (rawData)).toBeamMessage (decryptedData);
    }

    @Override
    public BeamMessage processOutgoingMessage (Communicator comm,
            BeamMessage originalMessage, BeamMessage responseMessage) {
        LegacyMessage basicMessage = new LegacyMessage (originalMessage);
        String session = basicMessage.getString ("beam_rsas");
        if (session == null) {
            throw new CryptException ("Invalid RSA connection!");
        }

        RSAConnection conn = RSAConnectionHolder.getRSAConnection (comm.getUID (), session);
        if (conn == null) {
            throw new CryptException ("Invalid RSA connection!");
        }

        return new RSABeamMessage (conn, responseMessage);
    }

}
