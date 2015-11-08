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
import java.util.logging.Logger;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class FileTransferChannel extends SystemHandler
{

    private static final Logger log = Logger.getLogger (FileTransferChannel.class.getName ());

    private final static int BUFFER_SIZE = 1024 * 256; //256KB
    private final static int DEFAULT_BURST_SIZE = 10; //2.5MB
    private final static int BURST_CONFIRMATION_WAIT_TIME = 1000 * 15; //15 seconds
    private final static int BLOCK_INTERVAL_WAIT_TIME = 1000 * 30; //30 seconds

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
        capturedBlockSet = Collections.synchronizedSet (new HashSet<FileDataMessage> ());
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

        log.finest (String.format ("Sending file: %s ; Burst size: %s, Block count %s, Block size: %s, Last block size: %s",
                file.getPath (), burstSize, blockCount, blockSize, lastBlockSize));

        //send block ids/sizes
        BeamMessage responseMessage = comm.getCommunicator ().send (burstMessage);
        if (responseMessage == null || !responseMessage.isSuccessful ()) {
            comm.getCommunicator ().removeHandler (this);
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
                    dataMessage.setFileData (readBuffer);

                    comm.getCommunicator ().queue (dataMessage);
                    log.finest (String.format ("Sent - FileDataMessage; Block number: %s, Block size: %s",
                            blockNumber, dataMessage.getData ().length));

                    burstCount++;
                    if (burstCount >= burstSize && burstCount != 0) {
                        break;
                    }
                }

                //sleep awhile incase data message hasn't finished flushing
                try {
                    Thread.sleep (250);
                } catch (InterruptedException ex) {
                }

                burstMessage.clear ();
                burstMessage.setBurstConfirmationMessage (true);
                do {
                    //send burst confirmation
                    responseMessage = comm.getCommunicator ().send (burstMessage, 2500);

                    if (!comm.getCommunicator ().isRunning ()) {
                        comm.getCommunicator ().removeHandler (this);
                        return -1; //communicator went down
                    }
                } while (responseMessage == null && !stop); //wait for burst confirmation and request blocks

                long endTime = System.currentTimeMillis () - cost;
                cost = System.currentTimeMillis ();
                burstMessage = new FileBurstMessage (responseMessage);
                if (burstMessage.isBurstComplete ()) {
                    totalSentData = fileSize;
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

                        log.finest (String.format ("Sucessfully sent block: %s, Block size: %s", blockNum, blockSize));
                    }

                    //confirmed sent data tracker
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

        log.finest (String.format ("Finished sending file: %s ; Total data sent: %s", file.getPath (), totalSentData));

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

        log.finest (String.format ("Receiving file: %s", file.getPath ()));

        stop = false;
        long fileSize = file.length ();
        long lastProcessedBlockTime = System.currentTimeMillis ();
        long recievedData = 0;
        int lastOutputSize = 0;

        try (RandomAccessFile raf = new RandomAccessFile (file, "rw")) {
            while (!stop) {
                int size = capturedBlockSet.size ();
                FileDataMessage[] capturedBlocks;
                if (size == 0) {
                    capturedBlocks = new FileDataMessage[0];
                } else {
                    capturedBlocks = capturedBlockSet.toArray (new FileDataMessage[size]);
                }

                if (capturedBlocks.length > 0) {
                    int[] captured = new int[capturedBlocks.length];
                    for (int i = 0; i < captured.length; i++) {
                        captured[i] = capturedBlocks[i].getBlockNumber ();
                    }
                    log.finest (String.format ("Captured blocks: %s", Arrays.toString (captured)));
                }

                for (FileDataMessage dataMessage : capturedBlocks) {
                    long startPos = (long) dataMessage.getBlockNumber () * (long) receiveBlockSize;

                    log.finest (String.format ("Parsing FileDataMessage; Block number: %s, File write position: %s",
                            dataMessage.getBlockNumber (), startPos));

                    byte[] fileData = dataMessage.getFileData ();
                    raf.seek (startPos);
                    raf.write (fileData);
                    raf.getFD ().sync ();
                    recievedData += fileData.length;

                    capturedBlockSet.remove (dataMessage);
                    downloadedBlockSet.add (dataMessage.getBlockNumber ());

                    if (tracker != null) {
                        try {
                            tracker.updateStats (fileSize, recievedData, fileData.length, System.currentTimeMillis () - lastProcessedBlockTime);
                        } catch (Exception ex) {
                            ex.printStackTrace ();
                        }
                    }

                    lastProcessedBlockTime = System.currentTimeMillis ();
                }

                if (!downloadedBlockSet.isEmpty () && lastOutputSize < downloadedBlockSet.size ()) {
                    lastOutputSize = downloadedBlockSet.size ();
                    log.finest (String.format ("Downloaded blocks: %s", Arrays.toString (downloadedBlockSet.toArray ())));
                }

                if (receiveFinished) {
                    break;
                } else if (!comm.getCommunicator ().isRunning ()) {
                    stop = true;
                }

                long timeWaited = System.currentTimeMillis () - lastProcessedBlockTime;
                if (receiveBlockCount == -1 && size == 0 && timeWaited >= BURST_CONFIRMATION_WAIT_TIME) {
                    log.warning ("Timed out receieving burst message. Closing file receive transfer...");
                    stop = true;
                } else if (timeWaited >= BLOCK_INTERVAL_WAIT_TIME) {
                    log.warning ("Timed out waiting for any file block message. Closing file receive transfer...");
                    stop = true;
                }
            }
        }

        log.finest (String.format ("Finished receiving file: %s ; Total data received: %s", file.getPath (), recievedData));

        //if it reached here we either transfered the whole file or it was stopped prematurely.
        //if it was stopped it's not good. if it wasn't, it's all good.
        comm.getCommunicator ().removeHandler (this);
        return !stop;
    }

    @Override
    public BeamMessage messageReceived (SystemCommunicator comm, BeamMessage message) {
        if (message.getMessageId () != transferChannelId) {
            log.finest (String.format ("Ignored invalid message; Message id: %s, Message type: %s",
                    message.getMessageId (), message.getType ()));
            //message is not for me
            return null;
        }

        if (message.getType () == SystemMessageType.FILE_DATA) {
            FileDataMessage fdm = new FileDataMessage (message);
            log.finest (String.format ("Received - FileDataMessage; Block number: %s, Block size: %s",
                    fdm.getBlockNumber (), fdm.getFileData ().length));
            capturedBlockSet.add (fdm);
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

                log.finest (String.format ("Received - FileBurstMessage; isBurstConfirmationMessage: true, Confirmed block list size: %s",
                        burstMessage.getConfirmedBlockList ().size ()));

                return burstMessage.setSuccessful (true);
            } else {
                //receive/confirm block calculations
                receiveBurstSize = burstMessage.getBurstSize ();
                receiveBlockCount = burstMessage.getBlockCount ();
                receiveBlockSize = burstMessage.getBlockSize ();
                receiveLastBlockSize = burstMessage.getLastBlockSize ();

                log.finest (String.format ("Received - FileBurstMessage; Burst size: %s, Block count %s, Block size: %s, Last block size: %s",
                        receiveBurstSize, receiveBlockCount, receiveBlockSize, receiveLastBlockSize));

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
