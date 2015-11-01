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

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class LegacyMessage<T extends LegacyMessage> extends BeamMessage
{

    protected HashMap<String, List<String>> messageMap = new HashMap<> ();

    public LegacyMessage () {
        this (0);
    }

    /**
     * Data message constructor
     *
     * @param type the type of message
     */
    public LegacyMessage (int type) {
        super (type, false, true);
    }

    public LegacyMessage (BeamMessage message) {
        super (message.getType (), message.getData (), message.isSystemMessage (), message.isRawData ());

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
    }

    public LegacyMessage (LegacyMessage message) {
        super (message);

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
    }

    /**
     * Data message constructor
     *
     * @param type the type of message
     * @param data the raw data to associate with the message
     */
    public LegacyMessage (int type, byte[] data) {
        this (type, data, true, true);
    }

    /**
     * Data message constructor
     *
     * @param type the type of message
     * @param data the data to associate with the message
     * @param rawData whether or not the data is raw
     */
    public LegacyMessage (int type, byte[] data, boolean rawData) {
        this (type, data, false, rawData, false);
    }

    /**
     * Data message constructor
     *
     * @param type the type of message
     * @param data the data to associate with the message
     * @param rawData whether or not the data is raw
     * @param parseData
     */
    public LegacyMessage (int type, byte[] data, boolean rawData, boolean parseData) {
        this (type, data, false, rawData, parseData);
    }

    LegacyMessage (int type, boolean systemMessage) {
        super (type, systemMessage);
    }

    LegacyMessage (int type, byte[] data, boolean systemMessage, boolean rawData, boolean parseData) {
        super (type, data, systemMessage, rawData);

        if (parseData) {
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
        }
    }

    @Override
    public byte[] getData () {
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

    public T setMessage (String key, LegacyMessage... value) {
        if (value.length == 0 || (value.length == 1 && value[0] == null)) {
            remove (key);
        } else {
            String[] strArray = new String[value.length];
            for (int i = 0; i < strArray.length; i++) {
                if (value[i] != null) {
                    byte[] messageData = value[i].getData ();
                    byte[] header = getHeader (messageData.length);

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

    public LegacyMessage getMessage (String key) {
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

            LegacyMessage message = new LegacyMessage (type, messageData);
            return message;
        }

        return null;
    }

    public List<LegacyMessage> getMessages (String key) {
        List<String> strList = getList (key);
        if (strList == null) {
            return null;
        }

        List<LegacyMessage> messageList = new ArrayList<LegacyMessage> ();
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

                LegacyMessage message = new LegacyMessage (type, messageData, rawData);
                message.setMessageId (id);
                messageList.add (message);
            }
        }

        return messageList;
    }

    public T setBytes (String key, byte[] bytes) {
        set (key, Base64.encode (bytes));
        return (T) this;
    }

    public byte[] getBytes (String key) {
        return Base64.decode (getString (key));
    }

    public T setByte (String key, byte value) {
        set (key, Byte.toString (value));
        return (T) this;
    }

    public Byte getByte (String key) {
        Object ob = get (key);
        if (ob == null) {
            return null;
        } else if (ob instanceof Byte) {
            return (Byte) ob;
        }

        return ((Integer) ob).byteValue ();
    }

    public T setBoolean (String key, Boolean... value) {
        set (key, toStringArray ((Object[]) value));
        return (T) this;
    }

    public Boolean getBoolean (String key) {
        Object ob = get (key);
        if (ob == null) {
            return null;
        } else if (ob instanceof Boolean) {
            return ((Boolean) ob);
        } else {
            return Boolean.parseBoolean ((String) ob);
        }
    }

    public List<Boolean> getBooleans (String key) {
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

    public T setInt (String key, Integer... value) {
        set (key, toStringArray ((Object[]) value));
        return (T) this;
    }

    public Integer getInt (String key) {
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

    public List<Integer> getInts (String key) {
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

    public T setString (String key, String... value) {
        set (key, value);
        return (T) this;
    }

    public String getString (String key) {
        Object ob = get (key);
        if (ob == null) {
            return null;
        }

        return (String) ob;
    }

    public List<String> getStrings (String key) {
        return getList (key);
    }

    public T setDate (String key, Date... value) {
        set (key, toStringArray ((Object[]) value));
        return (T) this;
    }

    public Date getDate (String key) {
        return Date.valueOf (get (key));
    }

    public List<Date> getDates (String key) {
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

    public T setTimestamp (String key, Timestamp... value) {
        set (key, toStringArray ((Object[]) value));
        return (T) this;
    }

    public Timestamp getTimestamp (String key) {
        return Timestamp.valueOf (get (key));
    }

    public List<Timestamp> getTimestamps (String key) {
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

    public T setLong (String key, Long... value) {
        set (key, toStringArray ((Object[]) value));
        return (T) this;
    }

    public Long getLong (String key) {
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

    public List<Long> getLongs (String key) {
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

    @Override
    public T response () {
        return (T) this;
    }

    public T emptyResponse () {
        return (T) response ().clear ();
    }

    @Override
    public T successResponse () {
        T msg = response ();
        return (T) msg.setSuccessful (true);
    }

    public T emptySuccessResponse () {
        return (T) clear ().successResponse ();
    }

    @Override
    public T errorResponse () {
        T msg = response ();
        return (T) msg.setSuccessful (false);
    }

    public T emptyErrorResponse () {
        return (T) clear ().errorResponse ();
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

    private byte[] getHeader (int messageSize) {
        ByteBuffer header = ByteBuffer.allocate (HEADER_SIZE);
        header.putInt (getType ()); //message type
        header.putInt (messageSize);//message size
        header.putLong (getMessageId ());//message id

        //data type
        if (isRawData ()) {
            header.put ((byte) 1);
        } else {
            header.put ((byte) 0);
        }

        return header.array ();
    }

}
