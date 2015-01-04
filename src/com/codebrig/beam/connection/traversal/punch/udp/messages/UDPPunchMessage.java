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
package com.codebrig.beam.connection.traversal.punch.udp.messages;

import com.codebrig.beam.messages.BeamMessage;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class UDPPunchMessage extends BeamMessage
{

    public UDPPunchMessage (int messageType, String peerIdentifier, String accessCode, int listenPort) {
        super (messageType);

        setString ("peer_identifier", peerIdentifier);
        setString ("access_code", accessCode);
        setInt ("listen_port", listenPort);
    }

    public UDPPunchMessage (BeamMessage message) {
        super (message);
    }

    public String getPeerIdentifier () {
        return getString ("peer_identifier");
    }

    public UDPPunchMessage setRequestPeerIdentifier (String requestPeerIdentifier) {
        setString ("request_peer_identifier", requestPeerIdentifier);
        return this;
    }

    public String getRequestPeerIdentifier () {
        return getString ("request_peer_identifier");
    }

    public String getAccessCode () {
        return getString ("access_code");
    }

    public UDPPunchMessage setIPAddress (String ipAddress) {
        setString ("ip_address", ipAddress);
        return this;
    }

    public String getIPAddress () {
        return getString ("ip_address");
    }

    public UDPPunchMessage setListenPort (int listenPort) {
        setInt ("listen_port", listenPort);
        return this;
    }

    public int getListenPort () {
        return getInt ("listen_port");
    }

}
