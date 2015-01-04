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
package com.codebrig.beam.connection.raw;

import com.codebrig.beam.Communicator;
import com.codebrig.beam.SystemCommunicator;
import com.codebrig.beam.utils.ByteFIFO;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class RawDataChannel
{

    private final SystemCommunicator comm;
    private final long rawChannelId;
    private final ByteFIFO byteFifo;

    private long remoteRawChannelId;
    private InputStream inputStream;
    private RawOutputStream outputStream;
    private boolean connected = false;
    private int nextBlockNumber = 1;
    private RawChannelHandler rawHandler;

    public RawDataChannel (SystemCommunicator comm, long rawChannelId) {
        this.comm = comm;
        this.rawChannelId = rawChannelId;

        byteFifo = new ByteFIFO (1024 * 1024 * 10); //10MB
    }

    public RawDataChannel (SystemCommunicator comm, long rawChannelId, int bufferSize) {
        this.comm = comm;
        this.rawChannelId = rawChannelId;

        byteFifo = new ByteFIFO (bufferSize);
    }

    public void connect (long remoteRawChannelId) {
        this.remoteRawChannelId = remoteRawChannelId;

        rawHandler = new RawChannelHandler (this);
        comm.getCommunicator ().addHandler (rawHandler);
        inputStream = byteFifo.getInputStream ();
        outputStream = new RawOutputStream (comm.getCommunicator (), remoteRawChannelId);
        connected = true;
    }

    public void close () {
        try {
            if (inputStream != null) {
                inputStream.close ();
            }
        } catch (IOException ex) {
            ex.printStackTrace ();
        }
        try {
            if (outputStream != null) {
                outputStream.close ();
            }
        } catch (IOException ex) {
            ex.printStackTrace ();
        }

        if (byteFifo != null) {
            byteFifo.kill ();
        }

        comm.getCommunicator ().removeHandler (rawHandler);
        connected = false;
    }

    public void setWaitForResponse (boolean wait) {
        rawHandler.setWaitForResponse (wait);
        outputStream.setWaitForResponse (wait);
    }

    public boolean isConnected () {
        return connected;
    }

    public Communicator getCommunicator () {
        return comm.getCommunicator ();
    }

    public InputStream getInputStream () {
        return inputStream;
    }

    public OutputStream getOutputStream () {
        return outputStream;
    }

    public long getRawChannelId () {
        return rawChannelId;
    }

    public long getRemoteRawChannelId () {
        return remoteRawChannelId;
    }

    protected int getNextBlockNumber () {
        return nextBlockNumber;
    }

    protected void setNextBlockNumber (int nextBlockNumber) {
        this.nextBlockNumber = nextBlockNumber;
    }

    protected void requestResendBlock (long blockRequestNumber) {
        outputStream.requestResendBlock (nextBlockNumber, blockRequestNumber);
    }

    protected void resendBlock (long blockRequestNumber) {
        outputStream.resendBlock (nextBlockNumber, blockRequestNumber);
    }

    protected void appendData (byte[] data) {
        byteFifo.add (data);
    }

}
