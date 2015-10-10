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

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class BasicMessage<T extends BasicMessage> extends BeamMessage<T>
{

    public BasicMessage () {
    }

    public BasicMessage (BeamMessage message) {
        super (message);
    }

    public BasicMessage (int type) {
        super (type);
    }

    public BasicMessage (BeamMessage message, boolean rawData) {
        super (message, rawData);
    }

    public BasicMessage (int type, byte[] data) {
        super (type, data);
    }

    public BasicMessage (BeamMessage message, boolean rawData, boolean parse) {
        super (message, rawData, parse);
    }

    public BasicMessage (int type, byte[] data, boolean parse) {
        super (type, data, parse);
    }

    @Override
    protected T setMessage (String key, BeamMessage... value) {
        return super.setMessage (key, value);
    }

    @Override
    public T setBoolean (String key, Boolean... value) {
        return super.setBoolean (key, value);
    }

    @Override
    public T setByte (String key, byte value) {
        return super.setByte (key, value);
    }

    @Override
    public T setBytes (String key, byte[] bytes) {
        return super.setBytes (key, bytes);
    }

    @Override
    public T setDate (String key, Date... value) {
        return super.setDate (key, value);
    }

    @Override
    public T setInt (String key, Integer... value) {
        return super.setInt (key, value);
    }

    @Override
    public T setLong (String key, Long... value) {
        return super.setLong (key, value);
    }

    @Override
    public T setString (String key, String... value) {
        return super.setString (key, value);
    }

    @Override
    public T setTimestamp (String key, Timestamp... value) {
        return super.setTimestamp (key, value);
    }

    @Override
    public BeamMessage getMessage (String key) {
        return super.getMessage (key);
    }

    @Override
    public List<BeamMessage> getMessages (String key) {
        return super.getMessages (key);
    }

    @Override
    public Boolean getBoolean (String key) {
        return super.getBoolean (key);
    }

    @Override
    public List<Boolean> getBooleans (String key) {
        return super.getBooleans (key);
    }

    @Override
    public Byte getByte (String key) {
        return super.getByte (key);
    }

    @Override
    public byte[] getBytes (String key) {
        return super.getBytes (key);
    }

    @Override
    public Date getDate (String key) {
        return super.getDate (key);
    }

    @Override
    public List<Date> getDates (String key) {
        return super.getDates (key);
    }

    @Override
    public Integer getInt (String key) {
        return super.getInt (key);
    }

    @Override
    public List<Integer> getInts (String key) {
        return super.getInts (key);
    }

    @Override
    public Long getLong (String key) {
        return super.getLong (key);
    }

    @Override
    public List<Long> getLongs (String key) {
        return super.getLongs (key);
    }

    @Override
    public String getString (String key) {
        return super.getString (key);
    }

    @Override
    public List<String> getStrings (String key) {
        return super.getStrings (key);
    }

    @Override
    public Timestamp getTimestamp (String key) {
        return super.getTimestamp (key);
    }

    @Override
    public List<Timestamp> getTimestamps (String key) {
        return super.getTimestamps (key);
    }

    @Override
    public T setSuccessful (boolean successful) {
        return super.setSuccessful (successful);
    }

    @Override
    public T setErrorMessage (String errorMessage) {
        return super.setErrorMessage (errorMessage);
    }

    @Override
    public T clear () {
        return super.clear ();
    }

    @Override
    public T copy (T message) {
        return super.copy (message);
    }

    @Override
    public T response () {
        return super.response ();
    }

    @Override
    public T emptyResponse () {
        return super.emptyResponse ();
    }

    @Override
    public T successResponse () {
        return super.successResponse ();
    }

    @Override
    public T emptySuccessResponse () {
        return super.emptySuccessResponse ();
    }

    @Override
    public T errorResponse () {
        return super.errorResponse ();
    }

    @Override
    public T emptyErrorResponse () {
        return super.emptyErrorResponse ();
    }

}
