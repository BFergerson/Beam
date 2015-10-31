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
package com.codebrig.beam.pool;

import com.codebrig.beam.BeamClient;
import com.codebrig.beam.messages.LegacyMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class DefaultClientPool<ClientT extends BeamClient> implements ClientPool<ClientT>
{

    private String name;
    private final Map<Long, ClientT> clients;

    public DefaultClientPool () {
        name = "DefaultClientPool";
        clients = new ConcurrentHashMap<> ();
    }

    public DefaultClientPool (Map<Long, ClientT> clients) {
        name = "DefaultClientPool";
        this.clients = clients;
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
    public void addClient (ClientT client) {
        clients.put (client.getCommunicator ().getUID (), client);
    }

    @Override
    public ClientT removeClient (long clientUID) {
        return clients.remove (clientUID);
    }

    @Override
    public boolean sendDirectMessage (long clientUID, LegacyMessage message) {
        ClientT comm = clients.get (clientUID);
        if (comm != null && comm.getCommunicator ().isRunning ()) {
            comm.getCommunicator ().queue (message);
            return true;
        }

        return false;
    }

    @Override
    public int broadcastMessage (LegacyMessage message) {
        final Iterator<ClientT> itr = clients.values ().iterator ();
        final ArrayList<Long> purgeList = new ArrayList<> ();
        int sendCount = 0;

        while (itr.hasNext ()) {
            final ClientT comm = itr.next ();
            if (comm != null) {
                if (comm.getCommunicator ().isRunning ()) {
                    comm.getCommunicator ().queue (message);
                    sendCount++;
                } else if (!comm.getCommunicator ().isRunning ()) {
                    //old client, need to purge it
                    purgeList.add (comm.getCommunicator ().getUID ());
                }
            }
        }

        if (!purgeList.isEmpty ()) {
            //got some clients we need to get rid of
            for (long oldCommUID : purgeList) {
                removeClient (oldCommUID);
            }
        }

        return sendCount;
    }

    @Override
    public void close () {
        Iterator<ClientT> itr = clients.values ().iterator ();
        while (itr.hasNext ()) {
            ClientT comm = itr.next ();
            if (comm != null) {
                comm.close ();
            }
        }

        clients.clear ();
    }

    @Override
    public boolean hasClient (long clientUID) {
        ClientT comm = getClient (clientUID);
        return comm != null;
    }

    @Override
    public ClientT getClient (long clientUID) {
        ClientT comm = clients.get (clientUID);
        if (comm != null && !comm.getCommunicator ().isRunning ()) {
            //comm no longer running
            removeClient (clientUID);
            return null;
        }

        return comm;
    }

    @Override
    public int size () {
        return clients.size ();
    }

    @Override
    public Map<Long, ClientT> getAllClients () {
        return new HashMap<> (clients);
    }

}
