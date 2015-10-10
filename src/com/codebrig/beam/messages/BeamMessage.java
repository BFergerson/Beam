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
package com.codebrig.beam.messages;

import com.codebrig.beam.utils.Base64;
import com.google.protobuf.InvalidProtocolBufferException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class BeamMessage<T extends BeamMessage>
{

    public static final int HEADER_SIZE = 20;

    protected boolean systemMessage;
    protected int type;
    protected byte[] data;
    protected HashMap<String, List<String>> messageMap;
    protected long createdTimestamp;
    protected long sentTimestamp;
    protected long receivedTimestamp;
    protected boolean rawData = false;
    protected long messageId = -1;

    /**
     * Data message constructor
     */
    public BeamMessage () {
        this (0, false);
    }

    /**
     * Data message constructor
     *
     * @param type the type of message
     */
    public BeamMessage (int type) {
        this (type, false);
    }

    BeamMessage (int type, boolean systemMessage) {
        if (type < 0 && !systemMessage) {
            throw new InvalidBeamMessage (String.format (
                    "Invalid message type: %s! Beam messages must have a type >= 0.", type));
        }

        this.data = null;
        this.type = type;
        this.systemMessage = type < 0;
        this.messageMap = new HashMap<> ();
        this.createdTimestamp = System.currentTimeMillis ();
    }

    /**
     * Data message constructor
     *
     * @param type the type of message
     * @param data the data to send with the message
     */
    public BeamMessage (int type, byte[] data) {
        this (type, data, false);
    }

    public BeamMessage (int type, byte[] data, boolean parse) {
        this (type, data, false, parse);
    }

    BeamMessage (int type, byte[] data, boolean systemMessage, boolean parse) {
        if (type < 0 && !systemMessage) {
            throw new InvalidBeamMessage (String.format (
                    "Invalid message type: %s! Beam messages must have a type >= 0.", type));
        }

        this.type = type;
        this.systemMessage = type < 0;
        this.messageMap = new HashMap<> ();
        this.createdTimestamp = System.currentTimeMillis ();

        if (parse) {
            ProtobufMessage.MessageEntrySet entrySet;
            try {
                entrySet = ProtobufMessage.MessageEntrySet.parseFrom (data);
            } catch (InvalidProtocolBufferException ex) {
                throw new RuntimeException (ex);
            }

            List<ProtobufMessage.MessageEntry> entriesList = entrySet.getEntriesList ();
            for (ProtobufMessage.MessageEntry entry : entriesList) {
                messageMap.put (entry.getKey (), entry.getValueList ());
            }

            this.data = null;
        } else {
            rawData = true;
            if (data != null) {
                this.data = Arrays.copyOf (data, data.length);
            } else {
                this.data = new byte[0];
            }
        }
    }

    public BeamMessage (BeamMessage message) {
        this (message, message.isRawData (), !message.isRawData ());
    }

    public BeamMessage (BeamMessage message, boolean rawData) {
        this (message, message.isSystemMessage (), rawData, !rawData);
    }

    public BeamMessage (BeamMessage message, boolean rawData, boolean parse) {
        this (message, message.isSystemMessage (), rawData, parse);
    }

    BeamMessage (BeamMessage message, boolean systemMessage, boolean rawData, boolean parse) {
        if (message.getType () < 0 && !systemMessage) {
            throw new InvalidBeamMessage (String.format (
                    "Invalid message type: %s! Beam messages must have a type >= 0.", message.getType ()));
        }

        this.type = message.getType ();
        this.systemMessage = type < 0;
        this.messageMap = new HashMap<> ();
        this.createdTimestamp = System.currentTimeMillis ();
        this.rawData = rawData;

        if (!parse) {
            this.data = Arrays.copyOf (message.getData (), message.getData ().length);
        } else {
            ProtobufMessage.MessageEntrySet entrySet;
            try {
                entrySet = ProtobufMessage.MessageEntrySet.parseFrom (message.getData ());
            } catch (InvalidProtocolBufferException ex) {
                throw new RuntimeException (ex);
            }

            List<ProtobufMessage.MessageEntry> entriesList = entrySet.getEntriesList ();
            for (ProtobufMessage.MessageEntry entry : entriesList) {
                messageMap.put (entry.getKey (), entry.getValueList ());
            }

            this.data = null;
        }

        if (message.getMessageId () != -1) {
            messageId = message.getMessageId ();
        }
    }

    public void setMessageId (long messageId) {
        this.messageId = messageId;
    }

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

    /**
     * @return message size
     */
    public int getSize () {
        return getData ().length;
    }

    public byte[] getData () {
        if (data != null && messageMap.isEmpty ()) {
            return data;
        }

        ProtobufMessage.MessageEntrySet.Builder entrySetBuilder = ProtobufMessage.MessageEntrySet.newBuilder ();
        Iterator<Map.Entry<String, List<String>>> entryItr = messageMap.entrySet ().iterator ();

        while (entryItr.hasNext ()) {
            Map.Entry<String, List<String>> entry = entryItr.next ();
            ProtobufMessage.MessageEntry mapEntry = ProtobufMessage.MessageEntry.newBuilder ()
                    .setKey (entry.getKey ())
                    .addAllValue (entry.getValue ()).build ();

            entrySetBuilder.addEntries (mapEntry);
        }

        return entrySetBuilder.build ().toByteArray ();
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

    /**
     * @return time message was created
     */
    public long getCreatedTimestamp () {
        return createdTimestamp;
    }

    public T remove (String key) {
        messageMap.remove (key);
        return (T) this;
    }

    public T set (String key, String... value) {
        if (value.length == 0 || (value.length == 1 && value[0] == null)) {
            remove (key);
        } else {
            List<String> valueList = Arrays.asList (value);
            messageMap.put (key, valueList);
        }
        return (T) this;
    }

    public String get (String key) {
        List<String> valueList = messageMap.get (key);
        if (valueList != null && !valueList.isEmpty ()) {
            return valueList.get (0);
        }

        return null;
    }

    public List<String> getList (String key) {
        return messageMap.get (key);
    }

    protected T setMessage (String key, BeamMessage... value) {
        if (value.length == 0 || (value.length == 1 && value[0] == null)) {
            remove (key);
        } else {
            String[] strArray = new String[value.length];
            for (int i = 0; i < strArray.length; i++) {
                if (value[i] != null) {
                    byte[] header = getHeader ();
                    byte[] messageData = value[i].getData ();

                    byte[] result = new byte[header.length + messageData.length];
                    System.arraycopy (header, 0, result, 0, header.length);
                    System.arraycopy (messageData, 0, result, header.length, messageData.length);

                    strArray[i] = Base64.encode (result);
                }
            }

            set (key, strArray);
        }
        return (T) this;
    }

    protected BeamMessage getMessage (String key) {
        String str = getString (key);
        if (str != null) {
            byte[] fullData = Base64.decode (str);
            byte[] header = new byte[HEADER_SIZE];
            byte[] messageData = new byte[fullData.length - HEADER_SIZE];
            System.arraycopy (fullData, 0, header, 0, header.length);
            System.arraycopy (fullData, HEADER_SIZE, messageData, 0, messageData.length);

            ByteBuffer buff = ByteBuffer.wrap (header);
            int type = buff.getInt (); //message type
            int size = buff.getInt (); //message size
            long id = buff.getLong (); //message id
            boolean rawData = buff.get () == 1;

            BeamMessage message = new SystemMessage (rawData, type, messageData, type < 0).toBeamMessage ();
            return message;
        }

        return null;
    }

    protected List<BeamMessage> getMessages (String key) {
        List<String> strList = getList (key);
        if (strList == null) {
            return null;
        }

        List<BeamMessage> messageList = new ArrayList<BeamMessage> ();
        for (String str : strList) {
            if (str != null) {
                byte[] fullData = Base64.decode (str);
                byte[] header = new byte[HEADER_SIZE];
                byte[] messageData = new byte[fullData.length - HEADER_SIZE];
                System.arraycopy (fullData, 0, header, 0, header.length);
                System.arraycopy (fullData, HEADER_SIZE, messageData, 0, messageData.length);

                ByteBuffer buff = ByteBuffer.wrap (header);
                int type = buff.getInt (); //message type
                int size = buff.getInt (); //message size
                long id = buff.getLong (); //message id
                boolean rawData = buff.get () == 1;

                BeamMessage message = new SystemMessage (rawData, type, messageData, type < 0).toBeamMessage ();
                messageList.add (message);
            }
        }

        return messageList;
    }

    protected T setBytes (String key, byte[] bytes) {
        set (key, Base64.encode (bytes));
        return (T) this;
    }

    protected byte[] getBytes (String key) {
        return Base64.decode (getString (key));
    }

    protected T setByte (String key, byte value) {
        set (key, Byte.toString (value));
        return (T) this;
    }

    protected Byte getByte (String key) {
        Object ob = get (key);
        if (ob == null) {
            return null;
        } else if (ob instanceof Byte) {
            return (Byte) ob;
        }

        return ((Integer) ob).byteValue ();
    }

    protected T setBoolean (String key, Boolean... value) {
        set (key, toStringArray ((Object[]) value));
        return (T) this;
    }

    protected Boolean getBoolean (String key) {
        Object ob = get (key);
        if (ob == null) {
            return null;
        } else if (ob instanceof Boolean) {
            return ((Boolean) ob);
        } else {
            return Boolean.parseBoolean ((String) ob);
        }
    }

    protected List<Boolean> getBooleans (String key) {
        List<String> strList = getList (key);
        if (strList == null) {
            return null;
        }

        List<Boolean> boolList = new ArrayList<Boolean> ();
        for (String str : strList) {
            boolList.add (Boolean.parseBoolean (str));
        }

        return boolList;
    }

    protected T setInt (String key, Integer... value) {
        set (key, toStringArray ((Object[]) value));
        return (T) this;
    }

    protected Integer getInt (String key) {
        Object l = get (key);
        if (l == null) {
            return null;
        } else {
            if (l instanceof Integer) {
                return (Integer) l;
            } else if (l instanceof Long) {
                return ((Long) l).intValue ();
            } else if (l instanceof String) {
                return Integer.parseInt ((String) l);
            }
        }

        return -1;
    }

    protected List<Integer> getInts (String key) {
        List<String> strList = getList (key);
        if (strList == null) {
            return null;
        }

        List<Integer> intList = new ArrayList<Integer> ();
        for (String str : strList) {
            intList.add (Integer.parseInt (str));
        }

        return intList;
    }

    protected T setString (String key, String... value) {
        set (key, value);
        return (T) this;
    }

    protected String getString (String key) {
        Object ob = get (key);
        if (ob == null) {
            return null;
        }

        return (String) ob;
    }

    protected List<String> getStrings (String key) {
        return getList (key);
    }

    protected T setDate (String key, Date... value) {
        set (key, toStringArray ((Object[]) value));
        return (T) this;
    }

    protected Date getDate (String key) {
        return Date.valueOf (get (key));
    }

    protected List<Date> getDates (String key) {
        List<String> strList = getList (key);
        if (strList == null) {
            return null;
        }

        List<Date> dateList = new ArrayList<Date> ();
        for (String str : strList) {
            dateList.add (Date.valueOf (str));
        }

        return dateList;
    }

    protected T setTimestamp (String key, Timestamp... value) {
        set (key, toStringArray ((Object[]) value));
        return (T) this;
    }

    protected Timestamp getTimestamp (String key) {
        return Timestamp.valueOf (get (key));
    }

    protected List<Timestamp> getTimestamps (String key) {
        List<String> strList = getList (key);
        if (strList == null) {
            return null;
        }

        List<Timestamp> timestampList = new ArrayList<Timestamp> ();
        for (String str : strList) {
            timestampList.add (Timestamp.valueOf (str));
        }

        return timestampList;
    }

    protected T setLong (String key, Long... value) {
        set (key, toStringArray ((Object[]) value));
        return (T) this;
    }

    protected Long getLong (String key) {
        Object l = get (key);
        if (l == null) {
            return null;
        } else {
            if (l instanceof BigInteger) {
                return ((BigInteger) l).longValue ();
            } else if (l instanceof String) {
                return Long.parseLong ((String) l);
            } else {
                return (Long) l;
            }
        }
    }

    protected List<Long> getLongs (String key) {
        List<String> strList = getList (key);
        if (strList == null) {
            return null;
        }

        List<Long> longList = new ArrayList<Long> ();
        for (String str : strList) {
            longList.add (Long.parseLong (str));
        }

        return longList;
    }

    public T setSuccessful (boolean successful) {
        setBoolean ("successful", successful);
        return (T) this;
    }

    public boolean isSuccessful () {
        Boolean success = getBoolean ("successful");
        if (success == null) {
            return false;
        }

        return success;
    }

    public T setErrorMessage (String errorMessage) {
        setString ("error_message", errorMessage);
        return (T) this;
    }

    public String getErrorMessage () {
        return getString ("error_message");
    }

    public boolean hasErrorMessage () {
        String errorMessage = getString ("error_message");
        return errorMessage != null && !errorMessage.isEmpty ();
    }

    public boolean isRawData () {
        return rawData;
    }

    public T clear () {
        messageMap.clear ();
        return (T) this;
    }

    public T copy (T message) {
        clear ();
        this.systemMessage = message.systemMessage;
        this.type = message.type;

        this.data = (message.data != null) ? Arrays.copyOf (message.data, message.data.length) : null;
        this.messageMap = new HashMap<> (message.messageMap);
        for (String key : messageMap.keySet ()) {
            List<String> strList = messageMap.get (key);
            messageMap.put (key, new ArrayList<> (strList));
        }

        this.createdTimestamp = message.createdTimestamp;
        this.sentTimestamp = message.sentTimestamp;
        this.receivedTimestamp = message.receivedTimestamp;
        this.rawData = message.rawData;
        this.messageId = message.messageId;
        return (T) this;
    }

    public T response () {
        return (T) this;
    }

    public T emptyResponse () {
        return (T) response ().clear ();
    }

    public T successResponse () {
        T msg = response ();
        return (T) msg.setSuccessful (true);
    }

    public T emptySuccessResponse () {
        T msg = successResponse ();
        return (T) msg.clear ();
    }

    public T errorResponse () {
        T msg = response ();
        return (T) msg.setSuccessful (false);
    }

    public T emptyErrorResponse () {
        T msg = errorResponse ();
        return (T) msg.clear ();
    }

    private String[] toStringArray (Object... value) {
        if (value.length == 1 && value[0] == null) {
            return null;
        }

        String[] strArray = new String[value.length];
        for (int i = 0; i < strArray.length; i++) {
            if (value[i] != null) {
                strArray[i] = value[i].toString ();
            }
        }

        return strArray;
    }

    private byte[] getHeader () {
        ByteBuffer header = ByteBuffer.allocate (HEADER_SIZE);
        header.putInt (getType ()); //message type
        header.putInt (getSize ());//message size
        header.putLong (getMessageId ());//message id

        //data type
        if (isRawData ()) {
            header.put ((byte) 1);
        } else {
            header.put ((byte) 0);
        }

        return header.array ();
    }

    @Override
    public int hashCode () {
        int hash = 3;
        hash = 59 * hash + (this.systemMessage ? 1 : 0);
        hash = 59 * hash + this.type;
        hash = 59 * hash + Arrays.hashCode (this.data);
        hash = 59 * hash + Objects.hashCode (this.messageMap);
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
        if (!Objects.equals (this.messageMap, other.messageMap)) {
            return false;
        }
        if (this.rawData != other.rawData) {
            return false;
        }
        return this.messageId == other.messageId;
    }

}
