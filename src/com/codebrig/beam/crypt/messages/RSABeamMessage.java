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
package com.codebrig.beam.crypt.messages;

import com.codebrig.beam.crypt.AES;
import com.codebrig.beam.crypt.CryptException;
import com.codebrig.beam.crypt.EncryptedBeamMessage;
import com.codebrig.beam.crypt.RSA;
import com.codebrig.beam.crypt.RSAConnection;
import com.codebrig.beam.messages.BasicMessage;
import com.codebrig.beam.messages.BeamMessage;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class RSABeamMessage extends BeamMessage implements EncryptedBeamMessage
{

    private final RSAConnection rsaConnection;
    private byte[] rawData;

    public RSABeamMessage (RSAConnection rsaConnection, int type) {
        super (type);

        if (rsaConnection == null) {
            throw new IllegalArgumentException ("Null rsa connection!");
        }

        this.rsaConnection = rsaConnection;
    }

    public RSABeamMessage (RSAConnection rsaConnection, BeamMessage message) {
        super (message, false, false);

        if (rsaConnection == null) {
            throw new IllegalArgumentException ("Null rsa connection!");
        }

        this.rsaConnection = rsaConnection;

        if (message.isRawData ()) {
            rawData = message.getData ();
        }
    }

    @Override
    public byte[] getData () {
        BasicMessage message = new BasicMessage (getType ());
        AES aes = rsaConnection.getAES ();
        RSA publicRSA = rsaConnection.getPublicRSA ();
        byte[] messageData;
        if (rawData != null) {
            messageData = rawData;
            message.setString ("beam_rsa_raw", "true");
        } else {
            messageData = super.getData ();
            message.setString ("beam_rsa_raw", "false");
        }

        if (aes == null || rsaConnection.getSession () == null) {
            message.setBytes ("beam_rsamd", publicRSA.encrypt (messageData));
        } else {
            message.setString ("beam_rsas", rsaConnection.getSession ());
            message.setBytes ("beam_rsamd", aes.encrypt (messageData));
        }

        return message.getData ();
    }

    public BeamMessage decryptBeamMessage (BeamMessage message) {
        try {
            BasicMessage basicMessage = new BasicMessage (message);
            AES aes = rsaConnection.getAES ();
            RSA publicRSA = rsaConnection.getPublicRSA ();
            byte[] messageData = basicMessage.getBytes ("beam_rsamd");
            String rsaRaw = basicMessage.getString ("beam_rsa_raw");

            if (aes == null || rsaConnection.getSession () == null) {
                byte[] data = publicRSA.decrypt (messageData);

                BeamMessage decryptedMessage = new BeamMessage (message.getType (), data, "false".equals (rsaRaw));
                return decryptedMessage;
            } else {
                byte[] data = aes.decrypt (messageData);
                return new BeamMessage (message.getType (), data, "false".equals (rsaRaw));
            }
        } catch (Exception ex) {
            throw new CryptException (ex);
        }
    }

}
