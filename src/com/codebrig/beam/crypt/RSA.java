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

import com.codebrig.beam.utils.Base64;
import com.codebrig.beam.utils.Checksum;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

public class RSA
{

    private static final BigInteger one = new BigInteger ("1");
    private static final int certainty = 50;
    private BigInteger pubExp;
    private BigInteger pubMod;
    private BigInteger privKey;
    private int keyLength = 0;

    public static void main (String[] args) throws Exception {
        RSA rsa = new RSA (2048);

        System.out.println (rsa.getPublicKeyPEM () + "\n");
        System.out.println (rsa.getPrivateKeyPEM ());
    }

    public static RSA parsePublicKeyPEM (String publicKey) {
        if (publicKey == null) {
            return null;
        }

        publicKey = publicKey.replaceAll ("-----BEGIN PUBLIC KEY-----", "");
        publicKey = publicKey.replaceAll ("-----END PUBLIC KEY-----", "");

        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec (Base64.decode (publicKey));
            KeyFactory keyFactory = KeyFactory.getInstance ("RSA");
            RSAPublicKey pubKey = (RSAPublicKey) keyFactory.generatePublic (spec);

            return new RSA (pubKey.getPublicExponent (), pubKey.getModulus ());
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace ();
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

    public static RSA parsePrivateKeyPEM (String privateKey) {
        if (privateKey == null) {
            return null;
        }

        privateKey = privateKey.replaceAll ("-----BEGIN PRIVATE KEY-----", "");
        privateKey = privateKey.replaceAll ("-----END PRIVATE KEY-----", "");

        try {
            KeyFactory keyFactory = KeyFactory.getInstance ("RSA");
            RSAPrivateKey privKey = (RSAPrivateKey) keyFactory.generatePrivate (new PKCS8EncodedKeySpec (Base64.decode (privateKey)));

            return new RSA (null, privKey.getModulus (), privKey.getPrivateExponent ());
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace ();
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

    public static RSA parsePrivateKeyPEM (String publicKey, String privateKey) {
        if (publicKey == null || privateKey == null) {
            return null;
        }

        publicKey = publicKey.replaceAll ("-----BEGIN PUBLIC KEY-----", "");
        publicKey = publicKey.replaceAll ("-----END PUBLIC KEY-----", "");
        privateKey = privateKey.replaceAll ("-----BEGIN PRIVATE KEY-----", "");
        privateKey = privateKey.replaceAll ("-----END PRIVATE KEY-----", "");

        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec (Base64.decode (publicKey));
            KeyFactory keyFactory = KeyFactory.getInstance ("RSA");
            RSAPublicKey pubKey = (RSAPublicKey) keyFactory.generatePublic (spec);
            RSAPrivateKey privKey = (RSAPrivateKey) keyFactory.generatePrivate (new PKCS8EncodedKeySpec (Base64.decode (privateKey)));

            return new RSA (pubKey.getPublicExponent (), pubKey.getModulus (), privKey.getPrivateExponent ());
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace ();
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

    public RSA (int keySize) {
        generateKeys (keySize);
    }

    /**
     * Constructor with public exponent and public modulus. Everybody can
     * encrypt and verify a message with this.
     *
     * @param publicExponent the public exponent.
     * @param publicModulus the public modulus.
     */
    public RSA (String publicExponent, String publicModulus) {
        this.pubExp = new BigInteger (publicExponent);
        this.pubMod = new BigInteger (publicModulus);
    }

    /**
     * Constructor with public exponent and public modulus. Everybody can
     * encrypt and verify a message with this.
     *
     * @param publicExponent the public exponent.
     * @param publicModulus the public modulus.
     */
    public RSA (BigInteger publicExponent, BigInteger publicModulus) {
        this.pubExp = publicExponent;
        this.pubMod = publicModulus;
    }

    /**
     * Constructor with public exponent, public modulus and private key. This is
     * for the recipient who holds the private key.
     *
     * @param publicExponent the public exponent.
     * @param publicModulus the public modulus.
     * @param privateKey the private key.
     */
    public RSA (String publicExponent, String publicModulus, String privateKey) {
        this.pubExp = new BigInteger (publicExponent);
        this.pubMod = new BigInteger (publicModulus);
        this.privKey = new BigInteger (privateKey);
    }

    /**
     * Constructor with public exponent, public modulus and private key. This is
     * for the recipient who holds the private key.
     *
     * @param publicExponent the public exponent.
     * @param publicModulus the public modulus.
     * @param privateKey the private key.
     */
    public RSA (BigInteger publicExponent, BigInteger publicModulus, BigInteger privateKey) {
        this.pubExp = publicExponent;
        this.pubMod = publicModulus;
        this.privKey = privateKey;
    }

    public BigInteger sign (BigInteger msg) {
        if (privKey.equals (new BigInteger ("0"))) {
            throw new RSAException ("Can't sign a message without private key!");
        }

        BigInteger signat;
        signat = msg.modPow (privKey, pubMod);

        return signat;
    }

    public String sign (String plainText) {
        if (privKey.equals (new BigInteger ("0"))) {
            throw new RSAException ("Can't sign a message without private key!");
        }

        BigInteger cipher;
        BigInteger msg;

        try {
            msg = new BigInteger (plainText.getBytes ("UTF8"));
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace ();
            return null;
        }

        cipher = msg.modPow (privKey, pubMod);

        return Base64.encode (cipher.toByteArray ());
    }

    public BigInteger verify (BigInteger signat) {
        BigInteger veri;
        veri = signat.modPow (pubExp, pubMod);

        return veri;
    }

    public String verify (String cryptText) {
        BigInteger cipher = new BigInteger (Base64.decode (cryptText));

        BigInteger msg;
        msg = cipher.modPow (pubExp, pubMod);

        try {
            return new String (msg.toByteArray (), "UTF8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

    public String encrypt (String plainText) {
        BigInteger cipher;
        BigInteger msg;

        try {
            msg = new BigInteger (plainText.getBytes ("UTF8"));
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace ();
            return null;
        }

        if (this.pubMod.compareTo (msg) == 1) {
            cipher = msg.modPow (this.pubExp, this.pubMod);
        } else {
            throw new RSAException ("Public modulus must be greater than the message!");
        }

        return Base64.encode (cipher.toByteArray ());
    }

    public String decrypt (String cryptText) {
        BigInteger cipher = new BigInteger (Base64.decode (cryptText));
        if (privKey == null || privKey.equals (new BigInteger ("0"))) {
            throw new RSAException ("No decryption without private key!");
        }

        BigInteger msg;
        msg = cipher.modPow (this.privKey, this.pubMod);

        try {
            return new String (msg.toByteArray (), "UTF8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

    public byte[] encrypt (byte[] plainText) {
        BigInteger msg = new BigInteger (plainText);
        BigInteger cipher;

        if (pubMod.compareTo (msg) == 1) {
            cipher = msg.modPow (pubExp, pubMod);
        } else {
            throw new RSAException ("Public modulus must be greater than the message!");
        }

        return cipher.toByteArray ();
    }

    public byte[] decrypt (byte[] cryptText) {
        BigInteger cipher = new BigInteger (cryptText);

        if (privKey == null || privKey.equals (new BigInteger ("0"))) {
            throw new RSAException ("No decryption without private key!");
        }

        BigInteger msg = cipher.modPow (privKey, pubMod);
        return msg.toByteArray ();
    }

    private int generateKeys (int keyL) {
        SecureRandom rand = new SecureRandom ();
        boolean found = false;
        int keyLen = keyL;
        if (keyL <= 0) {
            keyLen = rand.nextInt (49);
            keyLen = 512 + (keyLen * 32);
        }
        this.keyLength = keyLen;

        BigInteger p, q, p1, q1, phi, e;

        p = new BigInteger (keyLen / 2, certainty, new SecureRandom ()); // searching first prime
        q = new BigInteger (keyLen / 2 + 1, certainty, new SecureRandom ()); // searching second prime

        do {
            e = new BigInteger (keyLen, certainty, new SecureRandom ()); // searching prime for public exponent
            p1 = p.subtract (one);
            q1 = q.subtract (one);
            phi = p1.multiply (q1); // totient
            if (phi.gcd (e).equals (one)) {
                found = true;
            }
        } while (!found); // public exponent has to be coprime of phi

        pubMod = p.multiply (q); // public key modulus
        pubExp = e; // public key exponent
        privKey = pubExp.modInverse (phi); // the private key

        return this.keyLength;
    }

    public BigInteger getPublicModulus () {
        return pubMod;
    }

    public BigInteger getPublicExponent () {
        return pubExp;
    }

    public BigInteger getPrivateExponent () {
        return this.privKey;
    }

    public String getPrivateKeySigned () {
        return sign (Checksum.MD5 (privKey.toString ()));
    }

    public String getPublicKeyBase64 () {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance ("RSA");
            RSAPublicKeySpec ks = new RSAPublicKeySpec (getPublicModulus (), getPublicExponent ());
            PublicKey pubKey = (PublicKey) keyFactory.generatePublic (ks);

            return Base64.encode (pubKey.getEncoded ());
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace ();
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

    public String getPublicKeyPEM () {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance ("RSA");
            RSAPublicKeySpec ks = new RSAPublicKeySpec (getPublicModulus (), getPublicExponent ());
            PublicKey pubKey = (PublicKey) keyFactory.generatePublic (ks);

            String publicKey = Base64.encode (pubKey.getEncoded ());

            StringBuilder keyStrBuilder = new StringBuilder ();

            keyStrBuilder.append ("-----BEGIN PUBLIC KEY-----\n");
            for (String subSeq : splitArray (publicKey, 64)) {
                keyStrBuilder.append (subSeq).append ("\n");
            }
            keyStrBuilder.append ("-----END PUBLIC KEY-----");

            return keyStrBuilder.toString ();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace ();
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

    public String getPrivateKeyBase64 () {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance ("RSA");
            RSAPrivateKeySpec privKs = new RSAPrivateKeySpec (getPublicModulus (), getPrivateExponent ());
            PrivateKey rsaPrivKey = (PrivateKey) keyFactory.generatePrivate (privKs);

            return Base64.encode (rsaPrivKey.getEncoded ());
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace ();
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

    public String getPrivateKeyPEM () {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance ("RSA");

            RSAPrivateKeySpec privKs = new RSAPrivateKeySpec (getPublicModulus (), getPrivateExponent ());
            PrivateKey rsaPrivKey = (PrivateKey) keyFactory.generatePrivate (privKs);

            String privateKey = Base64.encode (rsaPrivKey.getEncoded ());
            StringBuilder keyStrBuilder = new StringBuilder ();

            keyStrBuilder.append ("-----BEGIN PRIVATE KEY-----\n");
            for (String subSeq : splitArray (privateKey, 64)) {
                keyStrBuilder.append (subSeq).append ("\n");
            }
            keyStrBuilder.append ("-----END PRIVATE KEY-----");

            return keyStrBuilder.toString ();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace ();
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace ();
        }

        return null;
    }

    private static List<String> splitArray (String message, int subarrLen) {
        List<String> result = new ArrayList<String> ();
        int i = 0;
        while (i < message.length ()) {
            result.add (message.substring (i, Math.min (message.length (), i + subarrLen)));
            i = i + subarrLen;
        }
        return result;
    }

}
