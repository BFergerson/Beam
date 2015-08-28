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
package com.codebrig.beam.crypt;

import com.codebrig.beam.utils.Checksum;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class AES
{

    private SecretKey key;
    private Cipher cipher;

    public AES (String password) {
        if (password != null && !password.isEmpty ()) {
            key = new SecretKeySpec (Checksum.SHA1 (password)
                    .substring (0, 16).getBytes (), "AES");

            try {
                cipher = Cipher.getInstance ("AES");
            } catch (NoSuchAlgorithmException ex) {
                ex.printStackTrace ();
            } catch (NoSuchPaddingException ex) {
                ex.printStackTrace ();
            }
        }
    }

    public String encrypt (String toEncrypt) {
        if (key == null) {
            return toEncrypt;
        }

        try {
            byte[] plaintext = toEncrypt.getBytes ();
            cipher.init (Cipher.ENCRYPT_MODE, key);
            byte[] encryptedData = cipher.doFinal (plaintext);

            return DatatypeConverter.printBase64Binary (encryptedData);
        } catch (InvalidKeyException ex) {
            ex.printStackTrace ();
        } catch (IllegalBlockSizeException ex) {
            ex.printStackTrace ();
        } catch (BadPaddingException ex) {
            ex.printStackTrace ();
        }

        return "";
    }

    public String decrypt (String toDecrypt) {
        if (key == null) {
            return toDecrypt;
        }

        try {
            byte[] ciphertext = DatatypeConverter.parseBase64Binary (toDecrypt);
            cipher.init (Cipher.DECRYPT_MODE, key);
            byte[] data = cipher.doFinal (ciphertext);

            return new String (data);
        } catch (InvalidKeyException ex) {
            ex.printStackTrace ();
        } catch (IllegalBlockSizeException ex) {
            ex.printStackTrace ();
        } catch (BadPaddingException ex) {
            ex.printStackTrace ();
        }

        return "";
    }

    public byte[] encrypt (byte[] toEncrypt) {
        if (key == null) {
            return toEncrypt;
        }

        try {
            cipher.init (Cipher.ENCRYPT_MODE, key);
            byte[] encryptedData = cipher.doFinal (toEncrypt);

            return encryptedData;
        } catch (InvalidKeyException ex) {
            ex.printStackTrace ();
        } catch (IllegalBlockSizeException ex) {
            ex.printStackTrace ();
        } catch (BadPaddingException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

    public byte[] decrypt (byte[] toDecrypt) {
        if (key == null) {
            return toDecrypt;
        }

        try {
            cipher.init (Cipher.DECRYPT_MODE, key);
            byte[] decryptedData = cipher.doFinal (toDecrypt);

            return decryptedData;
        } catch (InvalidKeyException ex) {
            ex.printStackTrace ();
        } catch (IllegalBlockSizeException ex) {
            ex.printStackTrace ();
        } catch (BadPaddingException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

    public Cipher getCipher (int mode) {
        try {
            cipher.init (mode, key);

            return cipher;
        } catch (InvalidKeyException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

}
