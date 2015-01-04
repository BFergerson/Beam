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
package com.codebrig.beam.connection.traversal.punch.udp.server;

import com.codebrig.beam.BeamServer;
import com.codebrig.beam.connection.traversal.punch.udp.server.handlers.ConnectionHandler;
import com.codebrig.beam.connection.traversal.punch.udp.server.handlers.HoleRequestHandler;
import com.codebrig.beam.connection.traversal.punch.udp.server.handlers.PunchRequestHandler;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class UDPPunchServer
{

    private final BeamServer server;
    private final NATDeviceHolder deviceHolder;

    public UDPPunchServer (String serverName, int port) {
        server = new BeamServer (serverName, port);
        deviceHolder = new NATDeviceHolder ();
    }

    public void start () {
        //add handlers
        server.addHandler (HoleRequestHandler.class, this);
        server.addHandler (PunchRequestHandler.class, this);
        server.addHandler (ConnectionHandler.class, this);

        //start it up
        server.start ();
    }

    public void stop () {
        server.close ();
    }

    public BeamServer getServer () {
        return server;
    }

    public NATDeviceHolder getNATDeviceHolder () {
        return deviceHolder;
    }

}
