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

import com.codebrig.beam.utils.CRC64;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.SystemMessage;
import com.codebrig.beam.messages.SystemMessageType;
import java.util.Arrays;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class RawDataMessage extends SystemMessage
{

    public RawDataMessage (long rawChannelId) {
        super (SystemMessageType.RAW_DATA);
        setMessageId (rawChannelId);
    }

    public RawDataMessage (BeamMessage message) {
        super (message);
    }

    public RawDataMessage setRawData (byte[] rawData) {
        setBytes ("raw_data", rawData);
        return this;
    }

    public byte[] getRawData () {
        return getBytes ("raw_data");
    }

    public RawDataMessage setBlockNumber (int blockNumber) {
        setInt ("block_number", blockNumber);
        return this;
    }

    public Integer getBlockNumber () {
        return getInt ("block_number");
    }

    public RawDataMessage setBlockRequestCode (long latestBlockRequestCode) {
        setLong ("request_code", latestBlockRequestCode);
        return this;
    }

    public Long getBlockRequestCode () {
        return getLong ("request_code");
    }

    public RawDataMessage setLatestBlockRequestCode (long latestBlockRequestCode) {
        setLong ("latest_request_code", latestBlockRequestCode);
        return this;
    }

    public Long getLatestBlockRequestCode () {
        return getLong ("latest_request_code");
    }

    public RawDataMessage setBlockRequest (boolean blockRequest) {
        setBoolean ("block_request", blockRequest);
        return this;
    }

    public boolean isBlockRequest () {
        Boolean blockRequest = getBoolean ("block_request");
        if (blockRequest == null) {
            return false;
        }

        return blockRequest;
    }

    public RawDataMessage setChecksum (byte[] crc64Checksum) {
        setBytes ("checksum", crc64Checksum);
        return this;
    }

    public byte[] getChecksum () {
        return getBytes ("checksum");
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
