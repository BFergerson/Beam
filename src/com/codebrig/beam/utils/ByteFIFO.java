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
package com.codebrig.beam.utils;

import java.io.InputStream;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class ByteFIFO
{

    private final byte[] queue;
    private final int capacity;
    private int size;
    private int head;
    private int tail;
    private final Object lock = new Object ();
    private final Object addLock = new Object ();
    private final Object removeLock = new Object ();
    private boolean keepGoing = true;

    public ByteFIFO (int cap) {
        capacity = (cap > 0) ? cap : 1; // at least 1
        queue = new byte[capacity];
        head = 0;
        tail = 0;
        size = 0;
    }

    public int getCapacity () {
        return capacity;
    }

    public int getSize () {
        return size;
    }

    public boolean isEmpty () {
        return (size == 0);
    }

    public boolean isFull () {
        return (size == capacity);
    }

    public void add (byte b) {
        synchronized (addLock) {
            waitWhileFull ();

            queue[head] = b;
            head = (head + 1) % capacity;
            size++;

            wakeUp (); // let any waiting threads know about change
        }
    }

    public void add (byte[] list) {
        synchronized (addLock) {
            // For efficiency, the bytes are copied in blocks
            // instead of one at a time. As space becomes available,
            // more bytes are copied until all of them have been
            // added.
            int ptr = 0;

            while (ptr < list.length) {
                // If full, the lock will be released to allow 
                // another thread to come in and remove bytes.
                waitWhileFull ();

                int space = capacity - size;
                int distToEnd = capacity - head;
                int blockLen = Math.min (space, distToEnd);

                int bytesRemaining = list.length - ptr;
                int copyLen = Math.min (blockLen, bytesRemaining);

                System.arraycopy (list, ptr, queue, head, copyLen);
                head = (head + copyLen) % capacity;
                size += copyLen;
                ptr += copyLen;

                // Keep the lock, but let any waiting threads 
                // know that something has changed.
                wakeUp ();
            }
        }
    }

    public byte remove () {
        byte b;
        synchronized (removeLock) {
            waitWhileEmpty ();

            b = queue[tail];
            tail = (tail + 1) % capacity;
            size--;

            wakeUp (); // let any waiting threads know about change
        }

        return b;
    }

    public byte[] removeAll () {
        byte[] list;
        synchronized (removeLock) {
            // For efficiency, the bytes are copied in blocks
            // instead of one at a time. 

            if (isEmpty ()) {
                // Nothing to remove, return a zero-length
                // array and do not bother with notification
                // since nothing was removed.
                return new byte[0];
            }

            // based on the current size
            list = new byte[size];

            // copy in the block from tail to the end
            int distToEnd = capacity - tail;
            int copyLen = Math.min (size, distToEnd);
            System.arraycopy (queue, tail, list, 0, copyLen);

            // If data wraps around, copy the remaining data
            // from the front of the array.
            if (size > copyLen) {
                System.arraycopy (
                        queue, 0, list, copyLen, size - copyLen);
            }

            tail = (tail + size) % capacity;
            size = 0; // everything has been removed

            // Signal any and all waiting threads that 
            // something has changed.
            wakeUp ();
        }

        return list;
    }

    public byte[] removeAll (int maxSize) {
        byte[] list;
        synchronized (removeLock) {
            // For efficiency, the bytes are copied in blocks
            // instead of one at a time. 

            if (isEmpty ()) {
                // Nothing to remove, return a zero-length
                // array and do not bother with notification
                // since nothing was removed.
                return new byte[0];
            }

            // based on the current size
            if (maxSize > size) {
                list = new byte[size];
            } else {
                list = new byte[maxSize];
            }

            // copy in the block from tail to the end
            int distToEnd = capacity - tail;
            int copyLen = Math.min (list.length, distToEnd);
            System.arraycopy (queue, tail, list, 0, copyLen);

            // If data wraps around, copy the remaining data
            // from the front of the array.
            if (list.length > copyLen) {
                System.arraycopy (
                        queue, 0, list, copyLen, list.length - copyLen);
            }

            tail = (tail + list.length) % capacity;

            if (maxSize > size) {
                size = 0; // everything has been removed
            } else {
                size = size - maxSize;
            }

            // Signal any and all waiting threads that 
            // something has changed.
            wakeUp ();
        }

        return list;
    }

    public byte[] removeAtLeastOne () {
        waitWhileEmpty (); // wait for a least one to be in FIFO
        return removeAll ();
    }

    public boolean waitUntilEmpty (long msTimeout) {
        if (msTimeout == 0L) {
            waitUntilEmpty ();  // use other method
            return true;
        }

        // wait only for the specified amount of time
        long endTime = System.currentTimeMillis () + msTimeout;
        long msRemaining = msTimeout;

        while (!isEmpty () && (msRemaining > 0L)) {
            sleep (msRemaining);
            msRemaining = endTime - System.currentTimeMillis ();
        }

        // May have timed out, or may have met condition, 
        // calc return value.
        return isEmpty ();
    }

    public void waitUntilEmpty () {
        while (!isEmpty () && keepGoing) {
            sleep ();
        }
    }

    public void waitWhileEmpty () {
        while (isEmpty () && keepGoing) {
            sleep ();
        }
    }

    public void waitUntilFull () {
        while (!isFull () && keepGoing) {
            sleep ();
        }
    }

    public void waitWhileFull () {
        while (isFull () && keepGoing) {
            sleep ();
        }
    }

    public InputStream getInputStream () {
        return new InputStream ()
        {
            @Override
            public int read () {
                return ByteFIFO.this.remove ();
            }

            @Override
            public int read (byte[] b, int off, int len) {
                for (int i = 0; i < len; i++) {
                    b[off + i] = (byte) read ();
                    if (!keepGoing) {
                        return -1;
                    }
                }
                return len - off;
            }
        };
    }

    public void sleep () {
        synchronized (lock) {
            try {
                lock.wait (1000);
            } catch (InterruptedException ex) {
            }
        }
    }

    public void sleep (long ms) {
        synchronized (lock) {
            try {
                lock.wait (ms);
            } catch (InterruptedException ex) {
            }
        }
    }

    public void kill () {
        keepGoing = false;
        wakeUp ();
    }

    public void wakeUp () {
        synchronized (lock) {
            lock.notifyAll ();
        }
    }

}
