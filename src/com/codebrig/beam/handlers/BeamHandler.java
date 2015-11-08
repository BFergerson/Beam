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
package com.codebrig.beam.handlers;

import com.codebrig.beam.Communicator;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.InvalidBeamMessage;
import java.util.Arrays;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public abstract class BeamHandler<MessageT extends BeamMessage>
{

    private final boolean systemHandler;
    private final int[] types;
    private boolean blockingHandler = false;

    public BeamHandler (int... types) {
        this (false, types);
    }

    BeamHandler (boolean systemHandler, int... types) {
        if (!systemHandler) {
            for (int type : types) {
                //Beam uses special internal system messages with types less than 0.
                //This restricts users from listening for these system messages.
                if (type < 0) {
                    throw new InvalidBeamMessage (String.format (
                            "Invalid message type: %s! Beam messages must have a type >= 0.", type));
                }
            }
        }

        this.systemHandler = systemHandler;
        this.types = types;
    }

    public boolean acceptsType (final int... type) {
        if (type != null) {
            for (int i = 0; i < types.length; i++) {
                for (int z = 0; z < type.length; z++) {
                    if (types[i] == type[z]) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public int[] getTypes () {
        return Arrays.copyOf (types, types.length);
    }

    public boolean isSystemHandler () {
        return systemHandler;
    }

    public boolean isBlockingHandler () {
        return blockingHandler;
    }

    public void setBlockingHandler (boolean blockingHandler) {
        this.blockingHandler = blockingHandler;
    }

    /**
     * Used by a Server to give an object to a BeamHandler.
     *
     * @param passObject the Object to pass to a BeamHandler.
     */
    public void passObject (final Object passObject) {
        //meant to be overridden
    }

    public final BeamMessage processMessage (final Communicator comm, final BeamMessage message) {
        BeamMessage processedMessage = processIncomingMessage (comm, message);
        MessageT convertedMessage = convertMessage (processedMessage);
        convertedMessage.setMessageId (message.getMessageId ());

        processedMessage = messageReceived (comm, convertedMessage);
        if (processedMessage != null) {
            processedMessage = processOutgoingMessage (comm, message, processedMessage);

            if (processedMessage != null && processedMessage.getMessageId () == -1) {
                processedMessage.setMessageId (message.getMessageId ());
            }
        }

        return processedMessage;
    }

    public BeamMessage processIncomingMessage (final Communicator comm, final BeamMessage message) {
        //no processing here; available to be overridden
        return message;
    }

    public BeamMessage processOutgoingMessage (final Communicator comm,
            final BeamMessage originalMessage, final BeamMessage responseMessage) {
        //no processing here; available to be overridden
        return responseMessage;
    }

    public abstract MessageT messageReceived (final Communicator comm, final MessageT message);

    public MessageT convertMessage (BeamMessage message) {
        //no processing here; available to be overridden
        return (MessageT) message;
    }

}
