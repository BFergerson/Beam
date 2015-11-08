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

import com.codebrig.beam.SystemCommunicator;
import com.codebrig.beam.handlers.SystemHandler;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.SystemMessageType;
import com.codebrig.beam.utils.Generator;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class RawChannelHandler extends SystemHandler
{

    private final RawDataChannel rawSocket;
    private long blockRequestCode = -1;
    private final HashMap<Integer, RawDataMessage> rawMessageMap;
    private final HashSet<Long> fulFilledRequests;
    private boolean waitForResponse;

    protected RawChannelHandler (RawDataChannel rawSocket) {
        super (SystemMessageType.RAW_DATA);

        this.rawSocket = rawSocket;
        rawMessageMap = new HashMap<Integer, RawDataMessage> ();
        fulFilledRequests = new HashSet<Long> ();
        fulFilledRequests.add (-1L);

        waitForResponse = true;
    }

    @Override
    public BeamMessage messageReceived (SystemCommunicator comm, BeamMessage message) {
        if (message != null && message.getMessageId () == rawSocket.getRawChannelId ()) {
            RawDataMessage dataMessage = new RawDataMessage (message);
            int blockNum = rawSocket.getNextBlockNumber ();
            boolean validChecksum = true;

            if (dataMessage.isBlockRequest () && !fulFilledRequests.contains (dataMessage.getBlockRequestCode ())) {
                rawSocket.resendBlock (blockRequestCode);
            } else {
                if (dataMessage.getBlockNumber () == blockNum) {
                    //validate data
                    validChecksum = dataMessage.isValidChecksum ();
                    if (validChecksum) {
                        //queue data
                        rawSocket.appendData (dataMessage.getRawData ());
                        rawSocket.setNextBlockNumber (blockNum + 1);
                    }
                } else if (rawMessageMap.containsKey (blockNum)) {
                    //queue from saved data
                    dataMessage = rawMessageMap.remove (blockNum);
                    rawSocket.appendData (dataMessage.getRawData ());
                    rawSocket.setNextBlockNumber (blockNum + 1);
                } else {
                    //request block
                    if (dataMessage.getLatestBlockRequestCode () == blockRequestCode) {
                        //already fulfilled request. make new one
                        blockRequestCode = Generator.randomLong ();
                        rawSocket.requestResendBlock (blockRequestCode);
                    }

                    //validate data
                    validChecksum = dataMessage.isValidChecksum ();
                    if (validChecksum) {
                        //save misplaced block
                        rawMessageMap.put (dataMessage.getBlockNumber (), dataMessage);
                    }
                }
            }

            //check if we have anything laying around to be appended
            blockNum = rawSocket.getNextBlockNumber ();
            while (rawMessageMap.containsKey (blockNum)) {
                //queue data
                dataMessage = rawMessageMap.remove (blockNum);
                rawSocket.appendData (dataMessage.getRawData ());
                rawSocket.setNextBlockNumber (blockNum + 1);

                blockNum = rawSocket.getNextBlockNumber ();
            }

            if (waitForResponse) {
                dataMessage.clearRawData ();
                return dataMessage.setSuccessful (validChecksum);
            }
        }

        return null;
    }

    public void setWaitForResponse (boolean wait) {
        waitForResponse = wait;
    }

}
