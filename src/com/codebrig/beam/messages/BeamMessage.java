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
package com.codebrig.beam.messages;

import io.protostuff.JsonIOUtil;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class BeamMessage<MessageT extends BeamMessage>
{

    public static final int HEADER_SIZE = 20; //20 bytes
    public static final int MAX_MESSAGE_SIZE = 52428800; //50 MB

    protected boolean systemMessage;
    protected int type;
    protected byte[] data;
    protected long createdTimestamp;
    protected long sentTimestamp;
    protected long receivedTimestamp;
    protected boolean rawData = false;
    protected long messageId = -1;
    protected boolean successful;
    protected String errorMessage;

    /**
     * Data message constructor
     *
     * @param type the type of message
     */
    public BeamMessage (int type) {
        this (type, false);
    }

    public BeamMessage (BeamMessage message) {
        this (message, message.isSystemMessage ());
    }

    /**
     * Data message constructor
     *
     * @param type the type of message
     * @param data the data to send with the message
     */
    public BeamMessage (int type, byte[] data) {
        if (type < 0) {
            throw new InvalidBeamMessage (String.format (
                    "Invalid message type: %s! Beam messages must have a type >= 0.", type));
        }

        this.type = type;
        this.data = data;
        this.rawData = true;
        this.createdTimestamp = System.currentTimeMillis ();
    }

    BeamMessage (int type, byte[] data, boolean systemMessage) {
        if (type < 0 && !systemMessage) {
            throw new InvalidBeamMessage (String.format (
                    "Invalid message type: %s! Beam messages must have a type >= 0.", type));
        }

        this.type = type;
        this.data = data;
        this.systemMessage = systemMessage;
        this.rawData = true;
        this.createdTimestamp = System.currentTimeMillis ();
    }

    BeamMessage (int type, byte[] data, boolean systemMessage, boolean rawData) {
        if (type < 0 && !systemMessage) {
            throw new InvalidBeamMessage (String.format (
                    "Invalid message type: %s! Beam messages must have a type >= 0.", type));
        }

        this.type = type;
        this.data = data;
        this.systemMessage = systemMessage;
        this.rawData = rawData;
        this.createdTimestamp = System.currentTimeMillis ();
    }

    BeamMessage (int type, boolean systemMessage) {
        if (type < 0 && !systemMessage) {
            throw new InvalidBeamMessage (String.format (
                    "Invalid message type: %s! Beam messages must have a type >= 0.", type));
        }

        this.type = type;
        this.systemMessage = systemMessage;
        this.createdTimestamp = System.currentTimeMillis ();
    }

    BeamMessage (int type, boolean systemMessage, boolean rawData) {
        if (type < 0 && !systemMessage) {
            throw new InvalidBeamMessage (String.format (
                    "Invalid message type: %s! Beam messages must have a type >= 0.", type));
        }

        this.type = type;
        this.systemMessage = systemMessage;
        this.rawData = rawData;
        this.createdTimestamp = System.currentTimeMillis ();
    }

    BeamMessage (BeamMessage message, boolean systemMessage) {
        if (message.getType () < 0 && !systemMessage) {
            throw new InvalidBeamMessage (String.format (
                    "Invalid message type: %s! Beam messages must have a type >= 0.", message.getType ()));
        }

        this.data = message.data;
        this.type = message.getType ();
        this.systemMessage = systemMessage;
        this.createdTimestamp = System.currentTimeMillis ();
        this.rawData = message.isRawData ();
        this.messageId = message.getMessageId ();
        this.successful = message.isSuccessful ();
        this.errorMessage = message.getErrorMessage ();

        if (rawData) {
            this.data = message.getData ();
        } else {
            if (data != null) {
                autoDeserialize (data);
            } else {
                autoDeserialize (message.getData ());
            }
        }
    }

    private void autoDeserialize (byte[] data) {
        if (data != null && data.length > 0) {
            LinkedBuffer buffer = BeamMessage.getMessageBuffer ();
            Schema<MessageT> schema = (Schema<MessageT>) RuntimeSchema.getSchema (getClass ());
            try {
                ProtostuffIOUtil.mergeFrom (data, (MessageT) this, schema);
            } finally {
                buffer.clear ();
            }
        }
    }

    public BeamMessage copy () {
        return new BeamMessage (this);
    }

    public void copy (MessageT message) {
        this.data = message.data;
        this.type = message.type;
        this.systemMessage = message.systemMessage;
        this.createdTimestamp = message.createdTimestamp;
        this.rawData = message.rawData;
        this.messageId = message.messageId;
        this.successful = message.successful;
        this.errorMessage = message.errorMessage;

        if (rawData) {
            this.data = message.getData ();
        } else {
            if (data != null) {
                autoDeserialize (data);
            } else {
                autoDeserialize (message.getData ());
            }
        }
    }

    public MessageT response () {
        return (MessageT) this;
    }

    public MessageT successResponse () {
        MessageT msg = response ();
        return (MessageT) msg.setSuccessful (true);
    }

    public MessageT errorResponse () {
        MessageT msg = response ();
        return (MessageT) msg.setSuccessful (false);
    }

    public MessageT setSuccessful (boolean successful) {
        this.successful = successful;
        return (MessageT) this;
    }

    public boolean isSuccessful () {
        return successful;
    }

    public MessageT setErrorMessage (String errorMessage) {
        this.errorMessage = errorMessage;
        return (MessageT) this;
    }

    public String getErrorMessage () {
        return errorMessage;
    }

    public boolean hasErrorMessage () {
        return errorMessage != null && !errorMessage.isEmpty ();
    }

    /**
     * @return time message was created
     */
    public long getCreatedTimestamp () {
        return createdTimestamp;
    }

    public boolean isRawData () {
        return rawData;
    }

    public void setReceivedTimestamp (long receivedTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
    }

    /**
     * @return time message was received
     */
    public long getReceivedTimestamp () {
        return receivedTimestamp;
    }

    public void setSentTimestamp (long sentTimestamp) {
        this.sentTimestamp = sentTimestamp;
    }

    /**
     * @return time message was sent
     */
    public long getSentTimestamp () {
        return sentTimestamp;
    }

    public void setMessageId (long messageId) {
        this.messageId = messageId;
    }

    /**
     * @return message id
     */
    public long getMessageId () {
        return messageId;
    }

    /**
     * @return message type
     */
    public int getType () {
        return type;
    }

    public boolean isSystemMessage () {
        return systemMessage;
    }

    public byte[] getData () {
        byte[] rtnData = null;
        if (rawData) {
            return data;
        } else {
            //auto-serialize
            LinkedBuffer buffer = getMessageBuffer ();
            Schema<MessageT> schema = (Schema<MessageT>) RuntimeSchema.getSchema (getClass ());
            MessageT message = (MessageT) this;
            try {
                rtnData = ProtostuffIOUtil.toByteArray (message, schema, buffer);
            } finally {
                buffer.clear ();
            }
        }

        return rtnData;
    }

    @Override
    public int hashCode () {
        int hash = 3;
        hash = 59 * hash + (this.systemMessage ? 1 : 0);
        hash = 59 * hash + this.type;
        hash = 59 * hash + Arrays.hashCode (this.data);
        hash = 59 * hash + (this.rawData ? 1 : 0);
        hash = 59 * hash + (int) (this.messageId ^ (this.messageId >>> 32));
        return hash;
    }

    @Override
    public boolean equals (Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass () != obj.getClass ()) {
            return false;
        }
        final BeamMessage<?> other = (BeamMessage<?>) obj;
        if (this.systemMessage != other.systemMessage) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        if (!Arrays.equals (this.data, other.data)) {
            return false;
        }
        if (this.rawData != other.rawData) {
            return false;
        }
        return this.messageId == other.messageId;
    }

    @Override
    public String toString () {
        if (rawData) {
            return Arrays.toString (data);
        } else {
            //auto-serialize
            StringWriter writer = new StringWriter ();
            Schema<MessageT> schema = (Schema<MessageT>) RuntimeSchema.getSchema (getClass ());
            MessageT message = (MessageT) this;
            try {
                JsonIOUtil.writeTo (writer, message, schema, false);
            } catch (IOException ex) {
                ex.printStackTrace ();
            }
            return writer.toString ();
        }
    }

    private static final ThreadLocal<LinkedBuffer> localBuffer = new ThreadLocal<LinkedBuffer> ()
    {
        @Override
        public LinkedBuffer initialValue () {
            return LinkedBuffer.allocate (MAX_MESSAGE_SIZE);
        }
    };

    public static LinkedBuffer getMessageBuffer () {
        return localBuffer.get ();
    }

}
