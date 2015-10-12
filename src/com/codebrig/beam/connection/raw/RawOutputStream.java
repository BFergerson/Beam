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
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.utils.CRC64;
import com.codebrig.beam.utils.LimitedQueue;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
class RawOutputStream extends OutputStream
{

    private final Communicator comm;
    private final long remoteRawChannelId;
    private byte[] _buf;
    private int blockNumber = 1;
    private long latestBlockRequestCode = -1;
    private final LimitedQueue<RawDataMessage> dataMessageQueue;
    private boolean flushed = true;
    private boolean waitForResponse;
    private boolean messageChecksum;

    protected RawOutputStream (Communicator comm, long remoteRawChannelId) {
        this.comm = comm;
        this.remoteRawChannelId = remoteRawChannelId;

        dataMessageQueue = new LimitedQueue<RawDataMessage> (25);
        waitForResponse = true;
        messageChecksum = true;
    }

    protected synchronized void requestResendBlock (int blockNumber, long blockRequestCode) {
        RawDataMessage dataMessage = new RawDataMessage (remoteRawChannelId);
        dataMessage.setBlockNumber (blockNumber);
        dataMessage.setBlockRequestCode (blockRequestCode);
        dataMessage.setBlockRequest (true);

        sendDataMessage (dataMessage);
    }

    protected synchronized void resendBlock (int blockNumber, long blockRequestCode) {
        RawDataMessage dataMessage = dataMessageQueue.peek ();

        if (dataMessage.getBlockNumber () > blockNumber) {
            //don't have block
        } else if (dataMessage.getBlockNumber () == blockNumber) {
            //got block
            sendDataMessage (dataMessage);
            latestBlockRequestCode = blockRequestCode;
        } else {
            //block is further down, get rid of anything with a smaller block number as other side won't need them
            RawDataMessage poll = dataMessageQueue.poll ();
            while (poll != null) {
                if (poll == null) {
                    //couldn't find
                    throw new RuntimeException ("Couldn't retransfer block: " + blockNumber);
                } else if (poll.getBlockNumber () != blockNumber) {
                    poll = dataMessageQueue.poll ();
                } else {
                    //found block
                    break;
                }
            }

            if (poll != null && poll.getBlockNumber () == blockNumber) {
                //got block
                dataMessageQueue.remove (dataMessage);
                sendDataMessage (dataMessage);
                latestBlockRequestCode = blockRequestCode;
            }
        }
    }

    @Override
    public synchronized void write (int b)
            throws IOException {
        _buf = new byte[1];
        _buf[0] = (byte) (b & 0xFF);
        flushed = false;
        flush ();
    }

    @Override
    public synchronized void write (byte[] b)
            throws IOException {
        write (b, 0, b.length);
    }

    @Override
    public synchronized void write (byte[] b, int off, int len)
            throws IOException {
        if (b == null) {
            throw new NullPointerException ();
        }

        if (off < 0 || len < 0 || (off + len) > b.length) {
            throw new IndexOutOfBoundsException ();
        }

        _buf = new byte[len];
        System.arraycopy (b, off, _buf, 0, len);
        flushed = false;
        flush ();
    }

    @Override
    public synchronized void flush ()
            throws IOException {
        if (!flushed) {
            RawDataMessage dataMessage = new RawDataMessage (remoteRawChannelId);
            dataMessage.setRawData (_buf);
            dataMessage.setBlockNumber (blockNumber++);

            sendDataMessage (dataMessage);
            flushed = true;
        }
    }

    private void sendDataMessage (RawDataMessage dataMessage) {
        dataMessage.setLatestBlockRequestCode (latestBlockRequestCode);

        if (messageChecksum && !dataMessage.isBlockRequest ()) {
            byte[] rawData = dataMessage.getRawData ();
            CRC64 crc = new CRC64 ();
            crc.update (rawData, 0, rawData.length);
            dataMessage.setChecksum (crc.finish ());
        }

        if (waitForResponse) {
            BeamMessage response = comm.send (dataMessage);
            while (response == null || !response.isSuccessful ()) {
                response = comm.send (dataMessage);
            }
        } else {
            comm.queue (dataMessage);
        }

        //add to queue
        dataMessageQueue.add (dataMessage);
    }

    public void setWaitForResponse (boolean wait) {
        waitForResponse = wait;
    }

    public void enableMessageChecksum (boolean messageChecksum) {
        this.messageChecksum = messageChecksum;
    }

}
