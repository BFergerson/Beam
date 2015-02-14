/*
 * Copyright (c) 2014-2015 CodeBrig, LLC.
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

import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.SystemMessage;
import com.codebrig.beam.messages.SystemMessageType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class FileBurstMessage extends SystemMessage<FileBurstMessage>
{

    public FileBurstMessage (long transferChannelId) {
        super (SystemMessageType.FILE_BURST);

        setMessageId (transferChannelId);
    }

    public FileBurstMessage (BeamMessage message) {
        super (message);
    }

    public FileBurstMessage setBlockCount (int blockCount) {
        return setInt ("block_count", blockCount);
    }

    public int getBlockCount () {
        return getInt ("block_count");
    }

    public FileBurstMessage setBurstSize (int burstSize) {
        return setInt ("burst_size", burstSize);
    }

    public int getBurstSize () {
        return getInt ("burst_size");
    }

    public FileBurstMessage setBlockSize (int blockSize) {
        return setInt ("block_size", blockSize);
    }

    public int getBlockSize () {
        return getInt ("block_size");
    }

    public FileBurstMessage setLastBlockSize (int lastBlockSize) {
        return setInt ("last_block_size", lastBlockSize);
    }

    public int getLastBlockSize () {
        return getInt ("last_block_size");
    }

    public FileBurstMessage setBurstConfirmationMessage (boolean burstConfirmation) {
        return setBoolean ("burst_confirmation", burstConfirmation);
    }

    public boolean isBurstConfirmationMessage () {
        Object burstOb = getBoolean ("burst_confirmation");
        if (burstOb == null) {
            return false;
        }

        return (Boolean) burstOb;
    }

    public FileBurstMessage setConfirmedBlockList (List<Integer> confirmedBlocks) {
        return setInt ("confirmed_blocks", confirmedBlocks.toArray (new Integer[confirmedBlocks.size ()]));
    }

    public List<Integer> getConfirmedBlockList () {
        List<Integer> confirmedBlocksList = getInts ("confirmed_blocks");
        if (confirmedBlocksList == null) {
            return new ArrayList<> ();
        }

        return confirmedBlocksList;
    }

    public FileBurstMessage setBurstComplete (boolean burstComplete) {
        return setBoolean ("burst_complete", burstComplete);
    }

    public boolean isBurstComplete () {
        Object burstOb = getBoolean ("burst_complete");
        if (burstOb == null) {
            return false;
        }

        return (Boolean) burstOb;
    }

}
