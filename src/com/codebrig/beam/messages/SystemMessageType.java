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
package com.codebrig.beam.messages;

/**
 * System messages that belong to the Beam library irregardless of the software
 * implementing it.
 *
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class SystemMessageType implements BeamMessageType
{

    /**
     *
     */
    public static final int BEAM_HANDSHAKE = -1;

    /**
     * Used to signify that the opposite end of the connection has closed the
     * connection and that the receiving end should gracefully close its side of
     * the connection as well.
     */
    public static final int CLOSE_CONNECTION = -2;

    /**
     *
     */
    public static final int SHUTDOWN_NOTICE = -3;

    /**
     *
     */
    public static final int PING_PONG = -4;

    /**
     *
     */
    public static final int TEST_CONNECTION = -5;

    /**
     *
     */
    public static final int RSA_CONNECTION_HANDSHAKE = -6;

    /**
     *
     */
    public static final int RAW_DATA_CONNECTION = -7;

    /**
     *
     */
    public static final int RAW_DATA = -8;

    /**
     *
     */
    public static final int RAW_DATA_RESEND = -9;

    /**
     *
     */
    public static final int FILE_TRANSFER_CONNECTION = -10;

    /**
     *
     */
    public static final int FILE_DATA = -11;

    /**
     *
     */
    public static final int FILE_BURST = -12;

    @Override
    public String getName (int messageType) {
        switch (messageType) {
            case BEAM_HANDSHAKE:
                return "BEAM_HANDSHAKE";
            case CLOSE_CONNECTION:
                return "CLOSE_CONNECTION";
            case SHUTDOWN_NOTICE:
                return "SHUTDOWN_NOTICE";
            case PING_PONG:
                return "PING_PONG";
            case TEST_CONNECTION:
                return "TEST_CONNECTION";
            case RSA_CONNECTION_HANDSHAKE:
                return "RSA_CONNECTION_HANDSHAKE";
            case RAW_DATA_CONNECTION:
                return "RAW_DATA_CONNECTION";
            case RAW_DATA:
                return "RAW_DATA";
            case RAW_DATA_RESEND:
                return "RAW_DATA_RESEND";
            case FILE_TRANSFER_CONNECTION:
                return "FILE_TRANSFER_CONNECTION";
            case FILE_DATA:
                return "FILE_DATA";
            case FILE_BURST:
                return "FILE_BURST";
        }
        return null;
    }

}
