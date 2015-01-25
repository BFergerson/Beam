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
package com.codebrig.beam.transfer;

import com.codebrig.beam.Communicator;
import com.codebrig.beam.SystemCommunicator;
import com.codebrig.beam.connection.raw.RawDataChannel;
import com.codebrig.beam.handlers.SystemHandler;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.SystemMessageType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class FileTransferChannel extends SystemHandler
{

    private final static int BUFFER_SIZE = 1024 * 1024; //1MB
    private boolean stop;
    private final Communicator comm;
    private final RawDataChannel rawChannel;
    private final InputStream inputStream;
    private final OutputStream outStream;
    private final List<Integer> downloadedBlockSet = new ArrayList<> ();
    private final Set<FileDataMessage> capturedBlockSet;
    private int receiveBlockSize = -1;
    private int receiveBlockCount = -1;
    private boolean receiveFinished = false;

    public FileTransferChannel (RawDataChannel rawChannel) {
        super (SystemMessageType.FILE_DATA, SystemMessageType.FILE_BURST);

        this.rawChannel = rawChannel;
        rawChannel.getCommunicator ().addHandler (this);

        comm = rawChannel.getCommunicator ();
        inputStream = rawChannel.getInputStream ();
        outStream = rawChannel.getOutputStream ();

        capturedBlockSet = Collections.newSetFromMap (new ConcurrentHashMap<FileDataMessage, Boolean> ());
    }

    public long sendFile (File file) throws IOException {
        return sendFile (file, null);
    }

    public long sendFile (File file, TransferTracker tracker) throws IOException {
        stop = false;
        long fileSize = file.length ();
        long cost;
        long sentData = 0;
        HashSet<Integer> neededBlockSet = new HashSet<> ();

        //calculate block ids/sizes
        int blockSize = BUFFER_SIZE;
        int blockCount = (int) (fileSize / blockSize);
        int lastBlockSize = (int) (fileSize % blockSize);
        if (lastBlockSize != 0) {
            blockCount++;
        }

        FileBurstMessage burstMessage = new FileBurstMessage ();
        burstMessage.setBlockCount (blockCount);
        burstMessage.setBlockSize (blockSize);

        for (int i = 0; i < blockCount; i++) {
            neededBlockSet.add (i);
        }

        //send block ids/sizes
        BeamMessage responseMessage = comm.send (burstMessage);
        if (responseMessage == null || !responseMessage.isSuccessful ()) {
            //throw error
        }

        try (RandomAccessFile raf = new RandomAccessFile (file, "rw")) {
            //repeat
            while (!stop) {
                //  burst file
                Integer[] requiredBlocks = neededBlockSet.toArray (new Integer[neededBlockSet.size ()]);
                for (int blockNumber : requiredBlocks) {
                    long seekPosition = blockNumber * blockSize;
                    raf.seek (seekPosition);

                    byte[] readBuffer;
                    if (seekPosition + blockSize > fileSize) {
                        //use smaller buffer
                        readBuffer = new byte[lastBlockSize];
                    } else {
                        readBuffer = new byte[blockSize];
                    }

                    raf.read (readBuffer);

                    //send data
                    FileDataMessage dataMessage = new FileDataMessage (rawChannel.getRemoteRawChannelId ());
                    dataMessage.setBlockNumber (blockNumber);
                    dataMessage.setRawData (readBuffer);
                    comm.queue (dataMessage);
                }

                burstMessage.clear ();
                burstMessage.setBurstConfirmationMessage (true);
                do {
                    //send burst confirmation
                    responseMessage = comm.send (burstMessage, 5000);
                } while (responseMessage == null && !stop); //wait for burst confirmation and request blocks

                burstMessage = new FileBurstMessage (responseMessage);
                if (burstMessage.isBurstComplete ()) {
                    break; //file finished transferring
                } else {
                    neededBlockSet.removeAll (burstMessage.getConfirmedBlockList ());
                }
            }
        }

        rawChannel.getCommunicator ().removeHandler (this);
        return sentData;
    }

    public boolean receiveFile (File file) throws IOException {
        return receiveFile (file, null);
    }

    public boolean receiveFile (File file, TransferTracker tracker) throws IOException {
        stop = false;
        long fileSize = file.length ();
        long cost;
        long recievedData = 0;

        //receive/confirm block calculations
        try (RandomAccessFile raf = new RandomAccessFile (file, "rw")) {
            while (!stop) {
                Object[] capturedBlocks = capturedBlockSet.toArray ();
                for (Object ob : capturedBlocks) {
                    FileDataMessage dataMessage = (FileDataMessage) ob;
                    int startPos = dataMessage.getBlockNumber () * receiveBlockSize;

                    raf.seek (startPos);
                    raf.write (dataMessage.getRawData ());

                    capturedBlockSet.remove (dataMessage);
                    downloadedBlockSet.add (dataMessage.getBlockNumber ());
                }

                if (receiveFinished) {
                    break;
                }
            }
        }

        //if it reached here we either transfered the whole file or it was stopped prematurely.
        //if it was stopped it's not good. if it wasn't, it's all good.
        rawChannel.getCommunicator ().removeHandler (this);
        return !stop;
    }

    public void close () {
        stop = true;
        rawChannel.getCommunicator ().removeHandler (this);
        rawChannel.close ();
    }

    @Override
    public BeamMessage messageRecieved (SystemCommunicator comm, BeamMessage message) {
        if (message.getType () == SystemMessageType.FILE_DATA) {
            capturedBlockSet.add (new FileDataMessage (message));
        } else {
            FileBurstMessage burstMessage = new FileBurstMessage (message);
            if (burstMessage.isBurstConfirmationMessage ()) {

                burstMessage.clear ();

                if (downloadedBlockSet.size () >= receiveBlockCount) {
                    receiveFinished = true;
                    burstMessage.setBurstComplete (true);
                } else {
                    burstMessage.setConfirmedBlockList (downloadedBlockSet);
                }

                return burstMessage.setSuccessful (true);
            } else {
                receiveBlockCount = burstMessage.getBlockCount ();
                receiveBlockSize = burstMessage.getBlockSize ();

                return burstMessage.setSuccessful (true);
            }
        }

        return null;
    }

}
