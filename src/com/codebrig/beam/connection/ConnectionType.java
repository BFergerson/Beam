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
package com.codebrig.beam.connection;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class ConnectionType
{

    private Incoming[] incoming;
    private Outgoing[] outgoing;

    public ConnectionType () {
        this ((Incoming) null, (Outgoing) null);
    }

    public ConnectionType (Incoming... incoming) {
        this (incoming, null);
    }

    public ConnectionType (Outgoing... outgoing) {
        this (null, outgoing);
    }

    public ConnectionType (Incoming incoming, Outgoing outgoing) {
        if (incoming == null) {
            setIncoming (Incoming.DIRECT); //default to direct
        } else {
            this.incoming = new Incoming[1];
            this.incoming[0] = incoming;
        }

        if (outgoing == null) {
            setOutgoing (Outgoing.DIRECT); //default to direct
        } else {
            this.outgoing = new Outgoing[1];
            this.outgoing[0] = outgoing;
        }
    }

    public ConnectionType (Incoming[] incoming, Outgoing[] outgoing) {
        if (incoming == null || incoming.length == 0) {
            setIncoming (Incoming.DIRECT); //default to direct
        } else {
            this.incoming = incoming;
        }

        if (outgoing == null || outgoing.length == 0) {
            setOutgoing (Outgoing.DIRECT); //default to direct
        } else {
            this.outgoing = outgoing;
        }
    }

    public final void setIncoming (Incoming... incoming) {
        if (incoming != null && incoming.length != 0) {
            for (Incoming in : incoming) {
                if (in == null) {
                    return;
                }
            }

            this.incoming = incoming;
        }
    }

    public final void setOutgoing (Outgoing... outgoing) {
        if (outgoing != null && outgoing.length != 0) {
            for (Outgoing out : outgoing) {
                if (out == null) {
                    return;
                }
            }

            this.outgoing = outgoing;
        }
    }

    public Incoming getTopIncoming () {
        return incoming[0];
    }

    public Outgoing getTopOutgoing () {
        return outgoing[0];
    }

    public Incoming[] getAllIncoming () {
        return incoming;
    }

    public Outgoing[] getAllOutgoing () {
        return outgoing;
    }

    public static enum Incoming
    {

        DIRECT (0),
        HTTP_HTTPS_TUNNEL (1),
        NAT_PMP (2),
        UPNP (3),
        UDP_HOLE_PUNCH (4);

        public static Incoming getIncoming (int connectionType) {
            Incoming[] incoming = Incoming.values ();
            for (Incoming in : incoming) {
                if (in.ordinal () == connectionType) {
                    return in;
                }
            }

            return null;
        }

        private final int type;

        private Incoming (int type) {
            this.type = type;
        }

        public int getType () {
            return type;
        }

    }

    public static enum Outgoing
    {

        DIRECT (0),
        HTTP_HTTPS_TUNNEL (1),
        UDP_HOLE_PUNCH (4);

        public static Outgoing getOutgoing (int connectionType) {
            Outgoing[] outgoing = Outgoing.values ();
            for (Outgoing out : outgoing) {
                if (out.ordinal () == connectionType) {
                    return out;
                }
            }

            return null;
        }

        private final int type;

        private Outgoing (int type) {
            this.type = type;
        }

        public int getType () {
            return type;
        }

    }

}
