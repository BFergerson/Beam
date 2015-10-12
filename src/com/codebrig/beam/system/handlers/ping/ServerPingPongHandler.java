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
package com.codebrig.beam.system.handlers.ping;

import com.codebrig.beam.Communicator;
import com.codebrig.beam.SystemCommunicator;
import com.codebrig.beam.handlers.SystemHandler;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.SystemMessage;
import com.codebrig.beam.messages.SystemMessageType;
import com.codebrig.beam.pool.CommunicatorPool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class ServerPingPongHandler extends SystemHandler implements Runnable
{

    private final Object lock = new Object ();
    private final CommunicatorPool pool;
    private final BeamMessage pingMessage;

    private volatile boolean keepGoing = true;
    private volatile Set<Long> responders;

    public ServerPingPongHandler (CommunicatorPool pool) {
        super (SystemMessageType.PING_PONG);

        this.pool = pool;
        this.pingMessage = new SystemMessage (SystemMessageType.PING_PONG);
    }

    @Override
    public void run () {
        while (keepGoing) {
            if (pool.size () != 0) {
                responders = new HashSet<Long> ();

                //get list of online users (that aren't claimed) and ping everyone
                Map<Long, Communicator> allCommun
                        = removeClaimedCommunicators (pool.getAllCommunicators ());
                broadcastMessage (pingMessage, allCommun);

                //broadcasted ping. lets wait 10 seconds and see who responds
                rest (10);
                if (!keepGoing) {
                    //quit early
                    break;
                }

                //purge responders and try again
                allCommun = purgeCommunicators (allCommun);
                broadcastMessage (pingMessage, allCommun);

                //broadcasted ping. lets wait 10 seconds and see who responds
                rest (10);
                if (!keepGoing) {
                    //quit early
                    break;
                }

                //purge responders once more and try again
                allCommun = purgeCommunicators (allCommun);
                broadcastMessage (pingMessage, allCommun);

                //broadcasted ping. lets wait 10 seconds and see who responds
                rest (10);
                if (!keepGoing) {
                    //quit early
                    break;
                }

                //whoever hasn't responded by this point is no longer connected
                //kick them off
                responders = null;
                if (allCommun.size () > 0) {
                    for (Communicator comm : allCommun.values ()) {
                        pool.removeCommunicator (comm.getUID ());

                        if (comm.isRunning ()) {
                            comm.close ();
                        }
                    }

                    System.out.println (String.format ("Purged %s sleeping communicators", allCommun.size ()));
                }
            }

            rest (60);
        }

        //System.out.println ("ThreadedPingPong Handler Ended!");
    }

    public void kill () {
        //System.out.println ("Killing ThreadedPingPong Handler...");
        keepGoing = false;

        synchronized (lock) {
            lock.notifyAll (); //wake self up
        }
    }

    private void broadcastMessage (BeamMessage message, Map<Long, Communicator> commun) {
        final Iterator<Communicator> itr = commun.values ().iterator ();
        final ArrayList<Long> purgeList = new ArrayList<Long> ();

        while (itr.hasNext ()) {
            final Communicator comm = itr.next ();

            if (comm != null) {
                if (comm.isRunning ()) {
                    if (comm.isClaimed ()) {
                        //user is busy, we can't talk to them.
                        //since they're busy we don't need to check if they are still connected
                        purgeList.add (comm.getUID ());
                    } else {
                        //they are avail for send
                        comm.queue (message);
                    }
                } else if (!comm.isRunning ()) {
                    //old communicator, need to purge it
                    purgeList.add (comm.getUID ());
                }
            }
        }

        if (!purgeList.isEmpty ()) {
            //got some communicators we need to fetch rid of
            //in the map that was given to us.
            for (long oldCommUID : purgeList) {
                commun.remove (oldCommUID);
            }
        }
    }

    private Map<Long, Communicator> removeClaimedCommunicators (Map<Long, Communicator> communicators) {
        Map<Long, Communicator> unclaimedCommunicators = new HashMap<Long, Communicator> ();
        Iterator<Communicator> itr = communicators.values ().iterator ();
        while (itr.hasNext ()) {
            Communicator next = itr.next ();
            if (!next.isClaimed ()) {
                unclaimedCommunicators.put (next.getUID (), next);
            }
        }

        return unclaimedCommunicators;
    }

    private Map<Long, Communicator> purgeCommunicators (Map<Long, Communicator> communicators) {
        Map<Long, Communicator> noRespCommunicators = new HashMap<Long, Communicator> ();

        Iterator<Communicator> itr = communicators.values ().iterator ();
        while (itr.hasNext ()) {
            Communicator next = itr.next ();

            if (next.isClaimed ()) {
                //in use
                continue;
            }

            if (responders != null && !responders.contains (next.getUID ())) {
                //didn't respond. add to noResponders 
                noRespCommunicators.put (next.getUID (), next);
            }
        }

        return noRespCommunicators;
    }

    private void rest (int seconds) {
        synchronized (lock) {
            try {
                if (keepGoing) {
                    lock.wait (seconds * 1000);
                }
            } catch (InterruptedException ex) {
                //eat it
            }
        }
    }

    @Override
    public BeamMessage messageReceived (SystemCommunicator comm, BeamMessage commMessage) {
        if (responders == null) {
            //this guy is too late. ignore
            return null;
        }

        responders.add (comm.getCommunicator ().getUID ());
        return null;
    }

}
