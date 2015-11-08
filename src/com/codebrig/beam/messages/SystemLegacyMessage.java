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

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class SystemLegacyMessage<MessageT extends LegacyMessage> extends LegacyMessage<LegacyMessage>
{

    public SystemLegacyMessage (int type) {
        super (type, true);
    }

    public SystemLegacyMessage (int type, byte[] data) {
        super (type, data, true);
    }

    public SystemLegacyMessage (boolean rawData, int type, byte[] data, boolean systemMessage) {
        super (type, data, systemMessage);
    }

    public SystemLegacyMessage (BeamMessage message) {
        super (message);
    }

    public LegacyMessage toBeamMessage () {
        //used to cast down SystemMessage instance
        return new LegacyMessage (this);
    }

}
