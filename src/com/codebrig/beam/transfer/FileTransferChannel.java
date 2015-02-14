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

import com.codebrig.beam.SystemCommunicator;
import com.codebrig.beam.handlers.SystemHandler;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.SystemMessageType;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
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

    private final static int BUFFER_SIZE = 1024 * 1024 * 5; //5MB
    private final static int DEFAULT_BURST_SIZE = 10; //50MB

    private boolean stop = false;
    private boolean connected = false;
    private final long transferChannelId;
    private long remoteTransferChannelId;
    private final SystemCommunicator comm;
    private final Set<Integer> downloadedBlockSet;
    private final Set<FileDataMessage> capturedBlockSet;
    private int receiveBurstSize = -1;
    private int receiveBlockSize = -1;
    private int receiveBlockCount = -1;
    private int receiveLastBlockSize = -1;
    private boolean receiveFinished = false;

    public FileTransferChannel (SystemCommunicator comm, long transferChannelId) {
        super (SystemMessageType.FILE_DATA, SystemMessageType.FILE_BURST);

        this.comm = comm;
        this.transferChannelId = transferChannelId;
        downloadedBlockSet = new HashSet<> ();
        capturedBlockSet = Collections.newSetFromMap (new ConcurrentHashMap<FileDataMessage, Boolean> ());
    }

    public void connect (long remoteTransferChannelId) {
        this.remoteTransferChannelId = remoteTransferChannelId;

        comm.getCommunicator ().addHandler (this);
        connected = true;
    }

    public void close () {
        stop = true;
        comm.getCommunicator ().removeHandler (this);
        connected = false;
    }

    public long sendFile (File file) throws IOException {
        return sendFile (file, null);
    }

    public long sendFile (File file, TransferTracker tracker) throws IOException {
        if (!connected) {
            throw new TransferException ("File transfer channel is not connected!");
        }

        stop = false;
        long fileSize = file.length ();
        long totalSentData = 0;
        HashSet<Integer> neededBlockSet = new HashSet<> ();

        //calculate block ids/sizes
        int blockSize = BUFFER_SIZE;
        int blockCount = (int) (fileSize / blockSize);

        double burstRatio = fileSize / (blockSize * DEFAULT_BURST_SIZE);
        int burstSize;
        if (burstRatio < 1.00) {
            burstSize = 0;
        } else {
            burstSize = (int) ((1.00 / burstRatio) * blockCount);
        }

        int lastBlockSize = (int) (fileSize % blockSize);
        if (lastBlockSize == 0) {
            //file is evenly divisible so last block size = block size
            lastBlockSize = blockSize;
        } else {
            blockCount++;
        }

        FileBurstMessage burstMessage = new FileBurstMessage (remoteTransferChannelId);
        burstMessage.setBurstSize (burstSize);
        burstMessage.setBlockCount (blockCount);
        burstMessage.setBlockSize (blockSize);
        burstMessage.setLastBlockSize (lastBlockSize);

        for (int i = 0; i < blockCount; i++) {
            neededBlockSet.add (i);
        }

        //send block ids/sizes
        BeamMessage responseMessage = comm.getCommunicator ().send (burstMessage);
        if (responseMessage == null || !responseMessage.isSuccessful ()) {
            throw new TransferException ("Unable to receive file burst confirmation!");
        }

        long cost = System.currentTimeMillis ();
        try (RandomAccessFile raf = new RandomAccessFile (file, "r")) {
            //repeat
            while (!stop) {
                //  burst file
                int burstCount = 0;
                Integer[] requiredBlocks = neededBlockSet.toArray (new Integer[neededBlockSet.size ()]);
                for (int blockNumber : requiredBlocks) {
                    long seekPosition = (long) blockNumber * (long) blockSize;
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
                    FileDataMessage dataMessage = new FileDataMessage (remoteTransferChannelId);
                    dataMessage.setBlockNumber (blockNumber);
                    dataMessage.setRawData (readBuffer);
                    comm.getCommunicator ().queue (dataMessage);
                    burstCount++;

                    if (burstCount >= burstSize && burstCount != 0) {
                        break;
                    }
                }

                burstMessage.clear ();
                burstMessage.setBurstConfirmationMessage (true);
                do {
                    //send burst confirmation
                    responseMessage = comm.getCommunicator ().send (burstMessage, 5000);
                } while (responseMessage == null && !stop); //wait for burst confirmation and request blocks

                long endTime = System.currentTimeMillis () - cost;
                cost = System.currentTimeMillis ();
                burstMessage = new FileBurstMessage (responseMessage);
                if (burstMessage.isBurstComplete ()) {
                    break; //file finished transferring
                } else {
                    long sentData = 0;
                    for (Integer blockNum : burstMessage.getConfirmedBlockList ()) {
                        if (blockNum == blockCount) {
                            sentData += lastBlockSize;
                        } else {
                            sentData += blockSize;
                        }
                        neededBlockSet.remove (blockNum);
                    }

                    if (tracker != null) {
                        int sentDelta = (int) (sentData - totalSentData);
                        if (sentDelta != 0) {
                            try {
                                tracker.updateStats (fileSize, sentData, sentDelta, endTime);
                            } catch (Exception ex) {
                                ex.printStackTrace ();
                            }

                            totalSentData = sentData;
                        }
                    }
                }
            }
        }

        comm.getCommunicator ().removeHandler (this);
        return totalSentData;
    }

    public boolean receiveFile (File file) throws IOException {
        return receiveFile (file, null);
    }

    public boolean receiveFile (File file, TransferTracker tracker) throws IOException {
        if (!connected) {
            throw new TransferException ("File transfer channel is not connected!");
        }

        stop = false;
        long fileSize = file.length ();
        long cost = System.currentTimeMillis ();
        long recievedData = 0;

        try (RandomAccessFile raf = new RandomAccessFile (file, "rw")) {
            while (!stop) {
                Object[] capturedBlocks = capturedBlockSet.toArray ();
                for (Object ob : capturedBlocks) {
                    FileDataMessage dataMessage = (FileDataMessage) ob;
                    long startPos = (long) dataMessage.getBlockNumber () * (long) receiveBlockSize;

                    byte[] rawData = dataMessage.getRawData ();
                    raf.seek (startPos);
                    raf.write (rawData);
                    raf.getFD ().sync ();
                    recievedData += rawData.length;

                    capturedBlockSet.remove (dataMessage);
                    downloadedBlockSet.add (dataMessage.getBlockNumber ());

                    if (tracker != null) {
                        try {
                            tracker.updateStats (fileSize, recievedData, rawData.length, System.currentTimeMillis () - cost);
                        } catch (Exception ex) {
                            ex.printStackTrace ();
                        }
                    }

                    cost = System.currentTimeMillis ();
                }

                if (receiveFinished) {
                    break;
                }
            }
        }

        //if it reached here we either transfered the whole file or it was stopped prematurely.
        //if it was stopped it's not good. if it wasn't, it's all good.
        comm.getCommunicator ().removeHandler (this);
        return !stop;
    }

    @Override
    public BeamMessage messageRecieved (SystemCommunicator comm, BeamMessage message) {
        if (message.getMessageId () != transferChannelId) {
            //message is not for me
            return null;
        }

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
                    List<Integer> confirmedList = Arrays.asList (downloadedBlockSet.toArray (
                            new Integer[downloadedBlockSet.size ()]));
                    burstMessage.setConfirmedBlockList (confirmedList);
                }

                return burstMessage.setSuccessful (true);
            } else {
                //receive/confirm block calculations
                receiveBurstSize = burstMessage.getBurstSize ();
                receiveBlockCount = burstMessage.getBlockCount ();
                receiveBlockSize = burstMessage.getBlockSize ();
                receiveLastBlockSize = burstMessage.getLastBlockSize ();

                return burstMessage.setSuccessful (true);
            }
        }

        return null;
    }

    public long getTransferChannelId () {
        return transferChannelId;
    }

    public long getRemoteTransferChannelId () {
        return remoteTransferChannelId;
    }

    public boolean isConnected () {
        return connected;
    }

}
