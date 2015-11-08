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

import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.SystemMessage;
import com.codebrig.beam.messages.SystemMessageType;
import com.codebrig.beam.utils.CRC64;
import java.util.Arrays;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class RawDataMessage extends SystemMessage
{

    private int blockNumber;
    private boolean blockRequest;
    private long blockRequestCode;
    private long latestBlockRequestCode;
    private byte[] _rawData;
    private byte[] checksum;

    public RawDataMessage (long rawChannelId) {
        super (SystemMessageType.RAW_DATA);
        setMessageId (rawChannelId);
    }

    public RawDataMessage (BeamMessage message) {
        super (message);
    }

    public RawDataMessage setBlockNumber (int blockNumber) {
        this.blockNumber = blockNumber;
        return this;
    }

    public int getBlockNumber () {
        return blockNumber;
    }

    public RawDataMessage setBlockRequest (boolean blockRequest) {
        this.blockRequest = blockRequest;
        return this;
    }

    public boolean isBlockRequest () {
        return blockRequest;
    }

    public RawDataMessage setBlockRequestCode (long blockRequestCode) {
        this.blockRequestCode = blockRequestCode;
        return this;
    }

    public long getBlockRequestCode () {
        return blockRequestCode;
    }

    public RawDataMessage setLatestBlockRequestCode (long latestBlockRequestCode) {
        this.latestBlockRequestCode = latestBlockRequestCode;
        return this;
    }

    public long getLatestBlockRequestCode () {
        return latestBlockRequestCode;
    }

    public RawDataMessage setRawData (byte[] rawData) {
        this._rawData = rawData;
        return this;
    }

    public byte[] getRawData () {
        return _rawData;
    }

    public void clearRawData () {
        _rawData = null;
    }

    public RawDataMessage setChecksum (byte[] checksum) {
        this.checksum = checksum;
        return this;
    }

    public byte[] getChecksum () {
        return checksum;
    }

    public boolean isValidChecksum () {
        byte[] checksum = getChecksum ();
        if (checksum == null) {
            return true;
        }

        byte[] rawData = getRawData ();
        CRC64 crc = new CRC64 ();
        crc.update (rawData, 0, rawData.length);
        return Arrays.equals (checksum, crc.finish ());
    }

}
