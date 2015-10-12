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
package com.codebrig.beam.connection.traversal.upnp;

import com.codebrig.beam.connection.ConnectionProtocol;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import net.sbbi.upnp.Discovery;
import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.UPNPResponseException;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class UPNPControl
{

    private final ConnectionProtocol protocol;
    private final InternetGatewayDevice dev;
    private final InetAddress localAddress;
    private final String localHost;

    public UPNPControl (ConnectionProtocol protocol)
            throws IOException, UPNPException {
        this (protocol, Discovery.DEFAULT_TIMEOUT);
    }

    public UPNPControl (ConnectionProtocol protocol, int timeout)
            throws IOException, UPNPException {
        DiscoveryThread discovery = new DiscoveryThread (timeout);
        discovery.start ();

        int timeoutCountdown;
        if (timeout > 15000) {
            timeoutCountdown = timeout;
        } else {
            timeoutCountdown = 15000; //15 seconds (in case it hangs)
        }
        while (timeoutCountdown > 0 && discovery.isAlive () && discovery.devices == null) {
            try {
                Thread.sleep (250);
            } catch (InterruptedException ex) {
            }
            timeoutCountdown -= 250;
        }
        if (discovery.devices == null || discovery.devices.length == 0) {
            //throw error 
            throw new UPNPException ("Could not find UPNP compatible router!");
        }

        InternetGatewayDevice compatibleDevice = null;
        for (InternetGatewayDevice device : discovery.devices) {
            if (device.supportsPortMapping ()) {
                compatibleDevice = device;
                break;
            }
        }

        if (compatibleDevice == null) {
            //throw error 
            throw new UPNPException ("Could not find port mapping compatible router!");
        }

        this.protocol = protocol;
        this.dev = compatibleDevice;
        this.localAddress = getLocalHostAddressFromSocket ();
        this.localHost = localAddress.getHostAddress ();
    }

    public UPNPControl (ConnectionProtocol protocol, InternetGatewayDevice device)
            throws UPNPException {
        if (device == null) {
            //throw error 
            throw new UPNPException ("Invalid UPNP compatible router!");
        } else if (!device.supportsPortMapping ()) {
            //throw error 
            throw new UPNPException ("Invalid UPNP compatible router. Must support port mapping!");
        }

        this.protocol = protocol;
        this.dev = device;
        this.localAddress = getLocalHostAddressFromSocket ();
        this.localHost = localAddress.getHostAddress ();
    }

    public boolean addPortMapping (String description, int localPort, int remotePort)
            throws IOException, UPNPResponseException {
        return addPortMapping (null, description, localPort, remotePort, 0);
    }

    public boolean addPortMapping (String remoteHost, String description, int localPort, int remotePort, int leaseDuration)
            throws IOException, UPNPResponseException {
        return dev.addPortMapping (description, protocol.name (), remoteHost, remotePort,
                localHost, localPort, leaseDuration);
    }

    public boolean removePortMapping (int remotePort)
            throws IOException, UPNPResponseException {
        return dev.deletePortMapping (null, remotePort, protocol.name ());
    }

    public boolean removePortMapping (String remoteHost, int remotePort)
            throws IOException, UPNPResponseException {
        return dev.deletePortMapping (remoteHost, remotePort, protocol.name ());
    }

    public String getExternalIPAddress ()
            throws UPNPException {
        String ipAddress;
        try {
            ipAddress = dev.getExternalIPAddress ();
        } catch (UPNPResponseException ex) {
            throw new UPNPException ("Could not get external IP!", ex);
        } catch (IOException ex) {
            throw new UPNPException ("Could not get external IP!", ex);
        }

        return ipAddress;
    }

    public InetAddress getLocalAddress () {
        return localAddress;
    }

    public String getLocalHost () {
        return localHost;
    }

    private InetAddress getLocalHostAddressFromSocket ()
            throws UPNPException {
        InetAddress localHostIP;
        try {
            int routerInternalPort = getInternalPort ();

            if (routerInternalPort > 0) {
                Socket socket;
                try {
                    socket = new Socket (getInternalHostName (), routerInternalPort);
                } catch (UnknownHostException ex) {
                    throw new UPNPException (String.format ("Could not create socked to %s:%s",
                            getInternalHostName (), routerInternalPort), ex);
                }

                localHostIP = socket.getLocalAddress ();
            } else {
                throw new UPNPException (String.format ("Invalid UPNP router port: %s!", routerInternalPort));
            }

            if (localHostIP == null) {
                localHostIP = InetAddress.getLocalHost ();
            }
        } catch (IOException ex) {
            throw new UPNPException ("Invalid localhost IP!", ex);
        }

        return localHostIP;
    }

    private String getInternalHostName ()
            throws UPNPException {
        URL presentationURL = dev.getIGDRootDevice ().getPresentationURL ();

        if (presentationURL == null) {
            throw new UPNPException ("Invalid UPNP presentation URL!");
        }

        String host = presentationURL.getHost ();
        return host;
    }

    private int getInternalPort ()
            throws UPNPException {
        URL presentationURL = dev.getIGDRootDevice ().getPresentationURL ();

        if (presentationURL != null) {
            int presentationUrlPort = presentationURL.getPort ();

            if (presentationUrlPort > 0) {
                return presentationUrlPort;
            }
        } else {
            throw new UPNPException ("Invalid UPNP presentation URL!");
        }

        int port = dev.getIGDRootDevice ().getPresentationURL ().getPort ();
        return port;
    }

    class DiscoveryThread extends Thread implements Runnable
    {

        private final int timeout;
        volatile InternetGatewayDevice[] devices;

        public DiscoveryThread (int timeout) {
            this.timeout = timeout;
        }

        @Override
        public void run () {
            try {
                devices = InternetGatewayDevice.getDevices (timeout);
            } catch (IOException ex) {
                ex.printStackTrace ();
            }
        }

    }

}
