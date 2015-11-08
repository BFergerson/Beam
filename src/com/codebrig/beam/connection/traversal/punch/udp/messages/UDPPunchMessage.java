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

    private String peerIdentifier;
    private String accessCode;
    private String requestPeerIdentifier;
    private String ipAddress;
    private int listenPort;

    public UDPPunchMessage (int messageType, String peerIdentifier, String accessCode, int listenPort) {
        super (messageType);

        this.peerIdentifier = peerIdentifier;
        this.accessCode = accessCode;
        this.listenPort = listenPort;
    }

    public UDPPunchMessage (BeamMessage message) {
        super (message);
    }

    public String getPeerIdentifier () {
        return peerIdentifier;
    }

    public UDPPunchMessage setRequestPeerIdentifier (String requestPeerIdentifier) {
        this.requestPeerIdentifier = requestPeerIdentifier;
        return this;
    }

    public String getRequestPeerIdentifier () {
        return requestPeerIdentifier;
    }

    public String getAccessCode () {
        return accessCode;
    }

    public UDPPunchMessage setIpAddress (String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public String getIpAddress () {
        return ipAddress;
    }

    public UDPPunchMessage setListenPort (int listenPort) {
        this.listenPort = listenPort;
        return this;
    }

    public int getListenPort () {
        return listenPort;
    }

    public void clear () {
        peerIdentifier = null;
        accessCode = null;
        requestPeerIdentifier = null;
        ipAddress = null;
        listenPort = -1;
    }

}
