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
package com.codebrig.beam.example.chat;

import com.codebrig.beam.messages.BeamMessage;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class ExampleChatMessage extends BeamMessage<ExampleChatMessage>
{

    public static final int CHAT_MESSAGE_ID = 1000;

    private String username;
    private String message;

    public ExampleChatMessage () {
        super (CHAT_MESSAGE_ID);
    }

    public ExampleChatMessage (BeamMessage message) {
        super (message);
    }

    public ExampleChatMessage setUsername (String username) {
        this.username = username;
        return this;
    }

    public String getUsername () {
        return username;
    }

    public ExampleChatMessage setMessage (String message) {
        this.message = message;
        return this;
    }

    public String getMessage () {
        return message;
    }

}
