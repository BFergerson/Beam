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

import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.SystemMessage;
import com.codebrig.beam.messages.SystemMessageType;
import com.codebrig.beam.utils.CRC64;
import java.util.Arrays;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class FileDataMessage extends SystemMessage
{

    private int blockNumber;
    private byte[] fileData;
    private byte[] checksum;

    public FileDataMessage (long transferChannelId) {
        super (SystemMessageType.FILE_DATA);

        setMessageId (transferChannelId);
    }

    public FileDataMessage (BeamMessage message) {
        super (message);
    }

    public FileDataMessage setBlockNumber (int blockNumber) {
        this.blockNumber = blockNumber;
        return this;
    }

    public int getBlockNumber () {
        return blockNumber;
    }

    public FileDataMessage setFileData (byte[] fileData) {
        this.fileData = fileData;
        return this;
    }

    public byte[] getFileData () {
        return fileData;
    }

    public FileDataMessage setChecksum (byte[] checksum) {
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

        byte[] fileData = getFileData ();
        CRC64 crc = new CRC64 ();
        crc.update (fileData, 0, fileData.length);
        return Arrays.equals (checksum, crc.finish ());
    }

}
