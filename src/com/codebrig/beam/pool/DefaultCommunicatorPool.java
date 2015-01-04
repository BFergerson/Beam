/*
 * Copyright © 2014 CodeBrig, LLC.
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
package com.codebrig.beam.pool;

import com.codebrig.beam.Communicator;
import com.codebrig.beam.messages.BeamMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class DefaultCommunicatorPool implements CommunicatorPool
{

    private String name;
    private final Map<Long, Communicator> communicators;

    public DefaultCommunicatorPool () {
        name = "DefaultCommunicatorPool";
        communicators = new ConcurrentHashMap<Long, Communicator> ();
    }

    public DefaultCommunicatorPool (Map<Long, Communicator> communicators) {
        name = "DefaultCommunicatorPool";
        this.communicators = communicators;
    }

    @Override
    public void setName (String name) {
        this.name = name;
    }

    @Override
    public String getName () {
        return name;
    }

    @Override
    public void addCommunicator (Communicator communicator) {
        final Object prevCommunicator = communicators.put (communicator.getUID (), communicator);

        if (prevCommunicator == null) {
            System.out.println (String.format ("%s: %s added to pool.", name, communicator.getUID ()));
        }
    }

    @Override
    public Communicator removeCommunicator (long communicatorUID) {
        final Communicator prevCommunicator = communicators.remove (communicatorUID);

        if (prevCommunicator != null) {
            System.out.println (String.format ("%s: %s removed from pool.", name, prevCommunicator.getUID ()));
        }

        return prevCommunicator;
    }

    @Override
    public boolean sendDirectMessage (long communicatorUID, BeamMessage message) {
        Communicator comm = communicators.get (communicatorUID);

        if (comm != null && comm.isRunning ()) {
            comm.queue (message);

            return true;
        }

        return false;
    }

    @Override
    public int broadcastMessage (BeamMessage message) {
        final Iterator<Communicator> itr = communicators.values ().iterator ();
        final ArrayList<Long> purgeList = new ArrayList<Long> ();
        int sendCount = 0;

        while (itr.hasNext ()) {
            final Communicator comm = itr.next ();

            if (comm != null) {
                if (comm.isRunning ()) {
                    comm.queue (message);
                    sendCount++;
                } else if (!comm.isRunning ()) {
                    //old communicator, need to purge it
                    purgeList.add (comm.getUID ());
                }
            }
        }

        if (!purgeList.isEmpty ()) {
            //got some communicators we need to get rid of
            for (long oldCommUID : purgeList) {
                removeCommunicator (oldCommUID);
            }
        }

        return sendCount;
    }

    @Override
    public void close () {
        Iterator<Communicator> itr = communicators.values ().iterator ();

        while (itr.hasNext ()) {
            Communicator comm = itr.next ();

            if (comm != null) {
                comm.close ();
            }
        }

        communicators.clear ();
    }

    @Override
    public boolean hasCommunicator (long commmunicatorUID) {
        return communicators.containsKey (commmunicatorUID);
    }

    @Override
    public Communicator getCommunicator (long communicatorUID) {
        return communicators.get (communicatorUID);
    }

    @Override
    public int size () {
        return communicators.size ();
    }

    @Override
    public Map<Long, Communicator> getAllCommunicators () {
        return new HashMap<Long, Communicator> (communicators);
    }

}
