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

    private int blockCount;
    private int burstSize;
    private int blockSize;
    private int lastBlockSize;
    private boolean burstConfirmation;
    private List<Integer> confirmedBlocks;
    private boolean burstComplete;

    public FileBurstMessage (long transferChannelId) {
        super (SystemMessageType.FILE_BURST);

        setMessageId (transferChannelId);
    }

    public FileBurstMessage (BeamMessage message) {
        super (message);
    }

    public FileBurstMessage setBlockCount (int blockCount) {
        this.blockCount = blockCount;
        return this;
    }

    public int getBlockCount () {
        return blockCount;
    }

    public FileBurstMessage setBurstSize (int burstSize) {
        this.burstSize = burstSize;
        return this;
    }

    public int getBurstSize () {
        return burstSize;
    }

    public FileBurstMessage setBlockSize (int blockSize) {
        this.blockSize = blockSize;
        return this;
    }

    public int getBlockSize () {
        return blockSize;
    }

    public FileBurstMessage setLastBlockSize (int lastBlockSize) {
        this.lastBlockSize = lastBlockSize;
        return this;
    }

    public int getLastBlockSize () {
        return lastBlockSize;
    }

    public FileBurstMessage setBurstConfirmationMessage (boolean burstConfirmation) {
        this.burstConfirmation = burstConfirmation;
        return this;
    }

    public boolean isBurstConfirmationMessage () {
        return burstConfirmation;
    }

    public FileBurstMessage setConfirmedBlockList (List<Integer> confirmedBlocks) {
        this.confirmedBlocks = new ArrayList<> (confirmedBlocks);
        return this;
    }

    public List<Integer> getConfirmedBlockList () {
        if (confirmedBlocks == null) {
            return new ArrayList<> ();
        }
        return confirmedBlocks;
    }

    public FileBurstMessage setBurstComplete (boolean burstComplete) {
        this.burstComplete = burstComplete;
        return this;
    }

    public boolean isBurstComplete () {
        return burstComplete;
    }

    public void clear () {
        blockCount = -1;
        blockSize = -1;
        lastBlockSize = -1;
        burstConfirmation = false;
        confirmedBlocks = null;
        burstComplete = false;
    }

}
