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

import com.codebrig.beam.connection.raw.RawDataChannel;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import net.jpountz.lz4.LZ4Factory;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class FileTransferChannel
{

    private final static LZ4Factory factory = LZ4Factory.safeInstance ();

    private final static int BUFFER_SIZE = 1024 * 1024; //1MB
    private boolean stop;
    private final RawDataChannel rawChannel;
    private final InputStream inputStream;
    private final OutputStream outStream;

    public FileTransferChannel (RawDataChannel rawChannel) {
        this.rawChannel = rawChannel;
        inputStream = rawChannel.getInputStream ();
        outStream = rawChannel.getOutputStream ();

        rawChannel.setWaitForResponse (true);
    }

    public void close () {
        rawChannel.close ();
    }

    public long sendFile (String fileName) throws IOException {
        return sendFile (fileName, 0, null, false);
    }

    public long sendFile (String fileName, long startPosition) throws IOException {
        return sendFile (fileName, startPosition, null, false);
    }

    public long sendFile (String fileName, long startPosition,
            TransferTracker tracker, boolean compress) throws IOException {
        stop = false;
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedInputStream fileStream = null;
        long sentData = 0;

        try {
            File f = new File (fileName);
            fileStream = new BufferedInputStream (new FileInputStream (f));
            if (startPosition == 0) {
                //0 or -1 = start at start!
                startPosition = -1;
            }
            if (startPosition != -1) {
                if (startPosition == f.length ()) {
                    //already sent all of it. nothing to do here
                    return 0;
                }
                fileStream.skip (startPosition);
            }

            ByteBuffer buf = ByteBuffer.allocate (20);

            //no exceptions
            buf.put (new byte[] {0, 0, 0, 0});

            //send if its a resume or not
            boolean resume = startPosition != -1;
            if (resume) {
                buf.put (new byte[] {1, 1, 1, 1});
            } else {
                buf.put (new byte[] {0, 0, 0, 0});
            }

            //send if its compressed or not
            if (compress) {
                buf.put (new byte[] {1, 1, 1, 1});
            } else {
                buf.put (new byte[] {0, 0, 0, 0});
            }

            //send file size - start position
            long fileSize = getFileSize (fileName) - (resume ? startPosition : 0);
            byte[] fSize = toLongByteArray (fileSize);

            buf.put (fSize);
            outStream.write (buf.array ());
            outStream.flush ();

//            if (compress) {
//                //turn outStream into a LZ4 stream
//                outStream = new LZ4BlockOutputStream (outStream, 1 << 16, factory.fastCompressor ());
//            }
            //send full file
            int size;
            long cost;

            if (buffer.length > fileSize) {
                buffer = new byte[(int) fileSize];
            }

            while (!stop && (size = fileStream.read (buffer)) > 0) {
                cost = System.currentTimeMillis ();
                outStream.write (buffer, 0, size);
                outStream.flush ();
                sentData += size;

                cost = (System.currentTimeMillis () - cost);
                //update tracker
                if (tracker != null) {
                    try {
                        tracker.updateStats (fileSize,
                                //add start position to sent if resume
                                sentData + ((resume) ? startPosition : 0),
                                size, cost);
                    } catch (Exception e) {
                        //idc
                    }
                }

                if (sentData < fileSize) {
                    if (sentData + BUFFER_SIZE > fileSize) {
                        //use a smaller buffer as to not overread!
                        buffer = new byte[((int) (fileSize - sentData))];
                    } else {
                        buffer = new byte[BUFFER_SIZE];
                    }
                }
            }

            outStream.flush ();

            if (tracker != null) {
                tracker.finished ();
            }
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace ();

            return -1;
        } finally {
            try {
                if (fileStream != null) {
                    fileStream.close ();
                }
            } catch (IOException ex) {
                //ignore
            }
        }

//        if (compress) {
//            return ((LZ4BlockOutputStream) outStream).getActualSentData ();
//        } else {
//            return sentData;
//        }
        return sentData;
    }

    public boolean receiveFile (RandomAccessFile outputFileStream) throws TransferException, IOException {
        return receiveFile (outputFileStream, null, false);
    }

    public boolean receiveFile (RandomAccessFile outputFileStream,
            TransferTracker tracker, boolean decompress) throws TransferException, IOException {
        stop = false;

        byte[] buffer = new byte[4];
        try {
            //see if we got any exceptions
            inputStream.read (buffer);

            if (buffer[0] == 1) {
                //we have an exception! see if we have an error message
                String message = null;

                buffer = new byte[1];
                inputStream.read (buffer);
                if (buffer[0] == 1) {
                    //we have a message. get length
                    buffer = new byte[4];
                    inputStream.read (buffer);

                    int messageLen = intFromByteArray (buffer);
                    buffer = new byte[messageLen];
                    inputStream.read (buffer);

                    message = new String (buffer, "UTF-8");
                }

                throw new TransferException ("Sender: " + message);
            }

            //get if resume or not
            buffer = new byte[4];
            inputStream.read (buffer);
            if (buffer[0] == 1) {
                //append data to the end
                outputFileStream.seek (outputFileStream.length ());
            } else {
                //clean file
                outputFileStream.setLength (0);
            }

            //get if compressed or not
            boolean isCompressed = false;
            buffer = new byte[4];
            inputStream.read (buffer);
            if (buffer[0] == 1) {
                isCompressed = true;
            }

            //get filesize
            byte[] lenBuffer = new byte[8];
            inputStream.read (lenBuffer);
            long fileSize = longFromByteArray (lenBuffer);

//            if (isCompressed && decompress) {
//                //turn inputStream into a LZ4 stream
//                inputStream = new LZ4BlockInputStream (inputStream, factory.decompressor ());
//            }
            //find out how much we already have here
            long cost;
            long receivedData = 0;

            while (!stop && receivedData < fileSize) {
                cost = System.currentTimeMillis ();
                if (receivedData + BUFFER_SIZE > fileSize) {
                    //use a smaller buffer as to not overread!
                    buffer = new byte[((int) (fileSize - receivedData))];
                } else {
                    buffer = new byte[BUFFER_SIZE];
                }

                int size = inputStream.read (buffer);
                outputFileStream.write (buffer, 0, size);
                receivedData += size;

                cost = System.currentTimeMillis () - cost;
                //update tracker if valid
                if (tracker != null) {
                    try {
                        tracker.updateStats (fileSize, receivedData, size, cost);
                    } catch (Exception ex) {
                        //ignore
                    }
                }
            }

            if (tracker != null) {
                tracker.finished ();
            }
        } catch (TransferException ex) {
            throw ex;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace ();

            return false;
        } finally {
            try {
                if (outputFileStream != null) {
                    outputFileStream.close ();
                }
            } catch (IOException ex) {
                //ignore
            }
        }

        //if it reached here it either transfered the whole file or it was stopped prematurely.
        //if it was stopped it's not good. if it wasn't, it's all good.
        return !stop;
    }

    public void stop () {
        this.stop = true;
    }

    private long getFileSize (String fileName) {
        return new File (fileName).length ();
    }

    private byte[] toLongByteArray (long value) {
        return ByteBuffer.allocate (8).putLong (value).array ();
    }

    private long longFromByteArray (byte[] bytes) {
        return ByteBuffer.wrap (bytes).getLong ();
    }

    private int intFromByteArray (byte[] bytes) {
        return ByteBuffer.wrap (bytes).getInt ();
    }

    private byte[] toIntByteArray (int value) {
        return ByteBuffer.allocate (4).putInt (value).array ();
    }

}
