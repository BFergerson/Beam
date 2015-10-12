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
package com.codebrig.beam.system.handlers;

import com.codebrig.beam.SystemCommunicator;
import com.codebrig.beam.handlers.SystemHandler;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.SystemMessage;
import com.codebrig.beam.messages.SystemMessageType;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class TestConnectionHandler extends SystemHandler
{

    public TestConnectionHandler () {
        super (SystemMessageType.TEST_CONNECTION);
    }

    @Override
    public BeamMessage messageReceived (SystemCommunicator comm, BeamMessage message) {
        if (comm.isTestingConnection ()) {
            //we are the one testing connection, handle response message
            comm.handleMessage (new SystemMessage (message).toBeamMessage ());
            return null;
        }

        //got message. connection must be good
        return new SystemMessage (SystemMessageType.TEST_CONNECTION).toBeamMessage ();
    }

}
