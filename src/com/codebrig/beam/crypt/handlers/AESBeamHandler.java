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
import com.codebrig.beam.crypt.AES;
import com.codebrig.beam.handlers.BeamHandler;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.SystemMessage;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public abstract class AESBeamHandler extends BeamHandler
{

    private final AES aes;

    public AESBeamHandler (AES aes, int... types) {
        super (types);

        this.aes = aes;
    }

    @Override
    public BeamMessage processIncomingMessage (Communicator comm, BeamMessage message) {
        byte[] decryptedData = aes.decrypt (message.getData ());
        return new SystemMessage (message.getType (), decryptedData, message.isSystemMessage (),
                message.isRawData ()).toBeamMessage (decryptedData);
    }

    @Override
    public BeamMessage processOutgoingMessage (Communicator comm,
            BeamMessage originalMessage, BeamMessage responseMessage) {
        byte[] encryptedData = aes.encrypt (responseMessage.getData ());
        return new SystemMessage (responseMessage.getType (), encryptedData, 
                responseMessage.isSystemMessage (), true);
    }

}
