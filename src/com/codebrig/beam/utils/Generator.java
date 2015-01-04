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

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class Generator
{

    private static final String capNumCharset
            = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String charset
            = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String charsetExtraNumbers
            = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String makeString (int length) {
        final SecureRandom sr = new SecureRandom ();
        final StringBuilder rtnString = new StringBuilder ("");

        for (int i = 0; i < length; i++) {
            rtnString.append (charset.charAt (
                    sr.nextInt (charset.length ())));
        }

        return rtnString.toString ();
    }

    public static String makeStringExtraNumbers (int length) {
        final SecureRandom sr = new SecureRandom ();
        final StringBuilder rtnString = new StringBuilder ("");

        for (int i = 0; i < length; i++) {
            rtnString.append (charsetExtraNumbers.charAt (
                    sr.nextInt (charsetExtraNumbers.length ())));
        }

        return rtnString.toString ();
    }

    public static String makeStringCapNum (int length) {
        final SecureRandom sr = new SecureRandom ();
        final StringBuilder rtnString = new StringBuilder ("");

        for (int i = 0; i < length; i++) {
            rtnString.append (capNumCharset.charAt (
                    sr.nextInt (capNumCharset.length ())));
        }

        return rtnString.toString ();
    }

    public static String getNumericString (int length) {
        final SecureRandom sr = new SecureRandom ();
        final StringBuilder rtnString = new StringBuilder ("");

        for (int i = 0; i < length; i++) {
            rtnString.append (sr.nextInt (10));
        }

        return rtnString.toString ();
    }

    public static int makeIdent () {
        return new SecureRandom ().nextInt (Integer.MAX_VALUE);
    }

    public static long randomLong () {
        return new BigInteger (64, new SecureRandom ()).longValue ();
    }

}
