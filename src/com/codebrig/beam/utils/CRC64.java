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

/**
 * @author Lasse Collin <lasse.collin@tukaani.org>
 */
public class CRC64
{

    private static final long poly = 0xC96C5795D7870F42L;
    private static final long crcTable[] = new long[256];

    static {
        for (int b = 0; b < crcTable.length; ++b) {
            long r = b;
            for (int i = 0; i < 8; ++i) {
                if ((r & 1) == 1) {
                    r = (r >>> 1) ^ poly;
                } else {
                    r >>>= 1;
                }
            }
            crcTable[b] = r;
        }
    }

    private long crc;

    public CRC64 () {
        crc = -1;
    }

    public void update (byte[] buf, int off, int len) {
        final int end = off + len;

        while (off < end) {
            crc = crcTable[(buf[off++] ^ (int) crc) & 0xFF] ^ (crc >>> 8);
        }
    }

    public byte[] finish () {
        final long value = ~crc;
        crc = -1;

        byte[] buf = new byte[8];
        for (int i = 0; i < buf.length; ++i) {
            buf[i] = (byte) (value >> (i * 8));
        }

        return buf;
    }

}
