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
package com.codebrig.beam.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class Checksum
{

    private static final int BUFFER_SIZE = 8192;

    public static long Adler32 (File file) {
        FileInputStream inputStream = null;

        try {
            inputStream = new FileInputStream (file);
            final Adler32 adlerChecksum = new Adler32 ();
            final CheckedInputStream cinStream = new CheckedInputStream (
                    inputStream, adlerChecksum);

            byte[] b = new byte[BUFFER_SIZE];
            while (cinStream.read (b) >= 0) {
            }

            return cinStream.getChecksum ().getValue ();
        } catch (IOException ex) {
            ex.printStackTrace ();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close ();
                } catch (IOException ex) {
                    ex.printStackTrace ();
                }
            }
        }

        return -1;
    }

    public static long Adler32 (String data) {
        final Adler32 adlerChecksum = new Adler32 ();

        try {
            adlerChecksum.update (data.getBytes ("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace ();
        }

        return adlerChecksum.getValue ();
    }

    public static String CRC64File (File file) {
        return crcFile (file);
    }

    public static String MD5 (String message) {
        return digest (message, "MD5");
    }

    public static String MD5File (File file) {
        return digestFile (file, "MD5");
    }

    public static String SHA1File (File file) {
        return digestFile (file, "SHA-1");
    }

    public static String SHA512File (File file) {
        return digestFile (file, "SHA-512");
    }

    public static String SHA512 (String message) {
        return digest (message, "SHA-512");
    }

    public static String SHA1WithPBKDF2 (String message, String salt, int iterations) {
        message = digest (message, "SHA-1");

        return PBKDF2 (message, salt, iterations);
    }

    public static String SHA1 (String message) {
        return digest (message, "SHA-1");
    }

    public static String PBKDF2 (String password, String salt, int iterations) {
        try {
            final String algorithm = "PBKDF2WithHmacSHA1";
            final int derivedKeyLength = 160;

            final KeySpec spec = new PBEKeySpec (password.toCharArray (),
                    salt.getBytes ("UTF-8"), iterations, derivedKeyLength);

            final SecretKeyFactory f = SecretKeyFactory.getInstance (algorithm);

            return convert (f.generateSecret (spec).getEncoded ());
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace ();
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace ();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

    private static String digest (String message, String algorithm) {
        byte[] messageData;
        try {
            messageData = message.getBytes ("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace ();
            return null;
        }

        return digest (messageData, algorithm);
    }

    private static String digest (byte[] message, String algorithm) {
        String result = "";

        try {
            final MessageDigest md = MessageDigest.getInstance (algorithm);
            md.update (message);

            final byte[] digest = md.digest ();
            result = convert (digest);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace ();
        }

        return result;
    }

    private static String digestFile (File file, String algorithm) {
        if (file.isDirectory ()) {
            return null;
        }

        FileInputStream rAF = null;

        try {
            rAF = new FileInputStream (file);
            byte[] buffer = new byte[BUFFER_SIZE];
            final MessageDigest complete = MessageDigest.getInstance (algorithm);
            int numRead;

            do {
                numRead = rAF.read (buffer);
                if (numRead > 0) {
                    complete.update (buffer, 0, numRead);
                }
            } while (numRead != -1);

            return convert (complete.digest ());
        } catch (IOException ex) {
            ex.printStackTrace ();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace ();
        } finally {
            try {
                if (rAF != null) {
                    rAF.close ();
                }
            } catch (IOException ex) {
                ex.printStackTrace ();
            }
        }

        return null;
    }

    private static String crcFile (File file) {
        if (file.isDirectory ()) {
            return null;
        }

        final CRC64 crc = new CRC64 ();
        FileInputStream rAF = null;

        try {
            rAF = new FileInputStream (file);
            byte[] buffer = new byte[BUFFER_SIZE];
            int numRead;

            do {
                numRead = rAF.read (buffer);
                if (numRead > 0) {
                    crc.update (buffer, 0, numRead);
                }
            } while (numRead != -1);

            return convert (crc.finish ());
        } catch (IOException ex) {
            ex.printStackTrace ();
        } finally {
            try {
                if (rAF != null) {
                    rAF.close ();
                }
            } catch (IOException ex) {
                ex.printStackTrace ();
            }
        }

        return null;
    }

    private static String convert (byte[] data) {
        return convertToHex (data);
    }

    private static String convertToHex (byte[] data) {
        final StringBuilder sb = new StringBuilder ();

        for (int i = 0; i < data.length; i++) {
            sb.append (Integer.toString ((data[i] & 0xff) + 0x100, 16).substring (1));
        }

        return sb.toString ();
    }

}
