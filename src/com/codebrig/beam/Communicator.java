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
package com.codebrig.beam;

import com.codebrig.beam.connection.raw.RawDataChannel;
import com.codebrig.beam.crypt.EncryptedBeamMessage;
import com.codebrig.beam.handlers.BeamHandler;
import com.codebrig.beam.handlers.SystemHandler;
import com.codebrig.beam.messages.BeamMessage;
import com.codebrig.beam.messages.BeamMessageType;
import com.codebrig.beam.messages.SystemMessage;
import com.codebrig.beam.messages.SystemMessageType;
import com.codebrig.beam.system.handlers.HandshakeHandler;
import com.codebrig.beam.system.handlers.TestConnectionHandler;
import com.codebrig.beam.system.handlers.ping.ClientPingPongHandler;
import com.codebrig.beam.system.messages.HandshakeMessage;
import com.codebrig.beam.transfer.FileTransferChannel;
import com.codebrig.beam.utils.Generator;
import com.jcraft.jhttptunnel.JHttpTunnelClient;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.SSLSocket;
import net.rudp.ReliableSocket;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class Communicator implements Runnable
{

    public static final int UNDEFINED_WAIT = -2;
    public static final int WAIT_FOREVER = -1;
    public static final int DEFAULT_MESSAGE_WAIT_TIME = 7500; //7.5 seconds

    private static int GLOBAL_DEFAULT_WAIT_TIME = DEFAULT_MESSAGE_WAIT_TIME;
    private int individualWaitTime = UNDEFINED_WAIT;

    private static long UIDCounter = 0;
    private static final Object uidLock = new Object ();
    private final static int BUFFER_SIZE = 1024 * 100;
    private Socket socket;
    private InputStream in;
    private final Object inLock = new Object ();
    private OutputStream out;
    private final Object outLock = new Object ();
    private boolean running;
    private final ArrayList<ImmediateHandler> immediateHandlers;
    private final ArrayList<BeamHandler> systemHandlers;
    private final ArrayList<BeamHandler> handlers;
    private final HashSet<Integer> registeredHandlerIDs;
    private long uid;
    private Thread commThread;
    private boolean serverCommunicator;
    private HashMap<String, Object> attributes = new HashMap<> ();
    private final ArrayList<ShutdownListener> shutdownListeners;
    private final List<BeamMessage> unhandledMessages;
    private final ArrayList<ConnectionStateListener> statusListeners;
    private String communicatorName;
    private volatile boolean claimed;
    private Queue<BeamMessage> queue = new LinkedList<> ();
    private boolean testingConnection = false;
    private boolean userClosed = false;
    private boolean openStreamFailure = false;
    private boolean debugOutput = false;
    private boolean performingHandshake = false;
    private boolean handshakeComplete = false;
    private JHttpTunnelClient tunnelClient;
    private SystemMessageType systemMessageType = new SystemMessageType ();
    private BeamMessageType messageType;

    private String clientVersion;
    private long clientTimeDiff;
    private boolean clientTunneled;

    public Communicator (Socket socket, String communicatorName, boolean serverCommunicator) {
        this.socket = socket;
        this.communicatorName = communicatorName;
        this.serverCommunicator = serverCommunicator;

        try {
            //switch up which stream loads first based on type
            //to avoid server and client deadlocking
            if (serverCommunicator) {
                in = new DataInputStream (socket.getInputStream ());
                out = new DataOutputStream (socket.getOutputStream ());
            } else {
                out = new DataOutputStream (socket.getOutputStream ());
                in = new DataInputStream (socket.getInputStream ());
            }
        } catch (Exception ex) {
            ex.printStackTrace ();
            openStreamFailure = true;
        }

        immediateHandlers = new ArrayList<> ();
        systemHandlers = new ArrayList<> ();
        handlers = new ArrayList<> ();
        registeredHandlerIDs = new HashSet<> ();
        unhandledMessages = new CopyOnWriteArrayList<> ();
        statusListeners = new ArrayList<> ();
        shutdownListeners = new ArrayList<> ();

        attachSystemHandlers ();

        //only clients init; server will init after it adds its listeners
        if (!serverCommunicator) {
            init ();
        }
    }

    public Communicator (JHttpTunnelClient tunnelClient, String communicatorName, boolean serverCommunicator) {
        this.tunnelClient = tunnelClient;
        this.communicatorName = communicatorName;
        this.serverCommunicator = serverCommunicator;

        //switch up which stream loads first based on type
        //to avoid server and client deadlocking
        if (serverCommunicator) {
            in = new DataInputStream (tunnelClient.getInputStream ());
            out = new DataOutputStream (tunnelClient.getOutputStream ());
        } else {
            out = new DataOutputStream (tunnelClient.getOutputStream ());
            in = new DataInputStream (tunnelClient.getInputStream ());
        }

        immediateHandlers = new ArrayList<> ();
        systemHandlers = new ArrayList<> ();
        handlers = new ArrayList<> ();
        registeredHandlerIDs = new HashSet<> ();
        unhandledMessages = new CopyOnWriteArrayList<> ();
        statusListeners = new ArrayList<> ();
        shutdownListeners = new ArrayList<> ();

        attachSystemHandlers ();

        //only clients init; server will init after it adds its listeners
        if (!serverCommunicator) {
            init ();
        }
    }

    private void attachSystemHandlers () {
        systemHandlers.add (new HandshakeHandler ());
        systemHandlers.add (new TestConnectionHandler ());

        //server communicators have global ping pong handlers and don't need one per communicator
        if (!serverCommunicator) {
            systemHandlers.add (new ClientPingPongHandler ());
        }
    }

    public BeamMessageType getMessageType () {
        return messageType;
    }

    public void setMessageType (BeamMessageType messageType) {
        this.messageType = messageType;
    }

    public void setOpenStreamFailure (boolean openStreamFailure) {
        this.openStreamFailure = openStreamFailure;
    }

    public boolean isOpenStreamFailure () {
        return openStreamFailure;
    }

    protected final void init () {
        running = false;

        synchronized (uidLock) {
            //set unique id
            uid = Communicator.UIDCounter++;
        }

        //alert connection about to be established
        preConnection ();

        commThread = new Thread (this);
        if (communicatorName == null) {
            communicatorName = String.format ("Communicator UID: %s", uid);
        }

        commThread.setName (String.format ("Communicator: %s; UID: %s", communicatorName, uid));
        commThread.setDaemon (true);
        commThread.start ();
    }

    public void performBeamHandshake () {
        if (!handshakeComplete) {
            performingHandshake = true;

            HandshakeMessage handshakeMessage
                    = new HandshakeMessage (Beam.VERSION, System.currentTimeMillis (), false);
            queue (handshakeMessage);

            while (performingHandshake) {
                try {
                    Thread.sleep (250);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    /**
     * Specify how long Communicator will wait for a response.
     *
     * @param waitTime time Communicator will wait in ms.
     */
    public static void setGlobalDefaultWaitTime (int waitTime) {
        if (waitTime == UNDEFINED_WAIT) {
            resetGlobalDefaultWaitTime ();
        } else {
            GLOBAL_DEFAULT_WAIT_TIME = waitTime;
        }
    }

    public void setIndividualWaitTime (int waitTime) {
        if (waitTime == UNDEFINED_WAIT) {
            resetIndividualWaitTime ();
        } else {
            individualWaitTime = waitTime;
        }
    }

    public void resetIndividualWaitTime () {
        individualWaitTime = UNDEFINED_WAIT;
    }

    public static void resetGlobalDefaultWaitTime () {
        GLOBAL_DEFAULT_WAIT_TIME = DEFAULT_MESSAGE_WAIT_TIME;
    }

    public int getIndividualWaitTime () {
        return individualWaitTime;
    }

    public String getName () {
        return communicatorName;
    }

    /**
     * Indicates how long Communicator will wait for a response.
     *
     * @return time Communicator will wait in ms.
     */
    public static int getGlobalDefaultWaitTime () {
        return GLOBAL_DEFAULT_WAIT_TIME;
    }

    public final int waitTime () {
        if (individualWaitTime != UNDEFINED_WAIT) {
            return individualWaitTime;
        } else {
            return GLOBAL_DEFAULT_WAIT_TIME;
        }
    }

    /**
     * Set thread name of Communicator for easier debugging.
     *
     * @param name Communicator's thread name.
     */
    public void setName (String name) {
        if (commThread != null && name != null) {
            commThread.setName (name);
        }
    }

    public String getRemoteIPAddress () {
        if (socket != null) {
            SocketAddress remote = socket.getRemoteSocketAddress ();
            if (remote != null) {
                return ((InetSocketAddress) remote).getHostString ();
            }
        }

        return null;
    }

    public String getHostIPAddress () {
        if (socket == null) {
            return null;
        }

        return socket.getInetAddress ().getHostAddress ();
    }

    @Override
    public void run () {
        running = true;

        try {
            while (running) {
                BeamMessage msg;

                //read next message
                if ((msg = readCommMessage ()) != null) {
                    if (msg.isSystemMessage () && msg.getType () == SystemMessageType.CLOSE_CONNECTION) {
                        //system closed connection
                        break;
                    } else if (msg.getType () == SystemMessageType.SHUTDOWN_NOTICE
                            && serverCommunicator == false) {
                        //see if shutdown message
                        int messageLen = intFromBytes (readStream (4));
                        String message = null;

                        if (messageLen > 0) {
                            message = new String (readStream (messageLen), "UTF-8");
                        }

                        //and let any listeners know
                        if (!shutdownListeners.isEmpty ()) {
                            for (ShutdownListener shutdownListener : shutdownListeners) {
                                shutdownListener.shutdownNotice (message);
                            }
                        }

                        //close connection
                        break;
                    }

                    //process message
                    processMessage (msg);
                }
            }
        } catch (EOFException ex) {
            if (!(socket instanceof ReliableSocket)) {
                //ignore reliable socket EOF as it is normal when connection is closed
                System.out.println (String.format ("%s EOF. Socket closed.", ex.getMessage ()));
            }
        } catch (SocketException ex) {
            //if running is false it was gracefully closed. ignore exceptions
            //else log exceptions
            if (running == true) {
                if (ex.getMessage ().contains ("Connection reset")) {
                    System.out.println (String.format (
                            "Connection reset error; Communicator: %s, Thread: %s",
                            uid, commThread.getName ()));
                } else if (ex.getMessage ().contains ("socket closed")) {
                    System.out.println (String.format (
                            "Socket closed error; Communicator: %s, Thread: %s",
                            uid, commThread.getName ()));
                } else {
                    ex.printStackTrace ();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace ();
        } catch (Exception ex) {
            //catch any exception so the below code will always run
            ex.printStackTrace ();
        }

        //if user already closed they won't need the following to run
        if (!userClosed) {
            running = false;

            //send null (a.k.a alert) to any waiting listeners
            for (ImmediateHandler immediateHandler : immediateHandlers) {
                immediateHandler.messageReceived (this, null);
            }

            postConnection ();

            try {
                out.close ();
            } catch (IOException ex) {
                ex.printStackTrace ();
            }

            try {
                in.close ();
            } catch (IOException ex) {
                ex.printStackTrace ();
            }
        }
    }

    private boolean checkValid (BeamHandler listener) {
        for (int type : listener.getTypes ()) {
            if (registeredHandlerIDs.contains (type)) {
                if (type < 0) {
                    System.out.println (String.format (
                            "Communicator already has listener registered for: %s", systemMessageType.getName (type)));
                } else if (messageType != null && messageType.getName (type) != null) {
                    System.out.println (String.format (
                            "Communicator already has listener registered for: %s", messageType.getName (type)));
                } else {
                    System.out.println (String.format (
                            "Communicator already has listener registered for: %s", type));
                }

                return false;
            }
        }

        return true;
    }

    private BeamMessage readCommMessage () throws IOException {
        BeamMessage msg;
        int messageSize;

        synchronized (inLock) {
            final byte[] header = readStream (BeamMessage.HEADER_SIZE);
            ByteBuffer headerBuf = ByteBuffer.wrap (header);

            final int type = headerBuf.getInt (); //message type
            final int size = headerBuf.getInt (); //message size
            final long id = headerBuf.getLong (); //message id
            final boolean rawData = headerBuf.get () == 1;

            if (size > BeamMessage.MAX_MESSAGE_SIZE || size < 0) {
                //message too big or invalid; other end isn't playing nice. drop connection
                close ();
                return null;
            }

            //final long sentTime = longFromBytes (readStream (8)); //message time
            //final int version = intFromBytes (readStream (4)); //message version
            //final int messageId = intFromBytes (readStream (4)); //message id
            byte[] data = readStream (size);
            msg = new SystemMessage (type, data, type < 0, rawData).toBeamMessage (data);
            msg.setMessageId (id);
            msg.setReceivedTimestamp (System.currentTimeMillis ());
            messageSize = size;
        }

        if (debugOutput) {
            if (msg.isSystemMessage ()) {
                System.out.println (String.format ("Received message: %s - Size: %s - Timestamp: %s",
                        systemMessageType.getName (msg.getType ()), messageSize, new Timestamp (System.currentTimeMillis ())));
            } else if (messageType != null && messageType.getName (msg.getType ()) != null) {
                System.out.println (String.format ("Received message: %s - Size: %s - Timestamp: %s",
                        messageType.getName (msg.getType ()), messageSize, new Timestamp (System.currentTimeMillis ())));
            } else {
                System.out.println (String.format ("Received message: %s - Size: %s - Timestamp: %s",
                        msg.getType (), messageSize, new Timestamp (System.currentTimeMillis ())));
            }
        }

        return msg;
    }

    public void writeStream (byte[] data) throws IOException {
        ByteArrayInputStream dataStream = new ByteArrayInputStream (data);
        int sent = 0;
        byte[] buffer;

        while (sent < data.length) {
            //setup buffer
            if (data.length - sent < BUFFER_SIZE) {
                //use a smaller buffer as to not overwrite!
                buffer = new byte[(data.length - sent)];
            } else {
                buffer = new byte[BUFFER_SIZE];
            }

            dataStream.read (buffer);

            out.write (buffer);
            out.flush ();

            sent += buffer.length;
        }
    }

    public byte[] readStream (int length) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate (length);
        int received = 0;
        byte[] buffer;

        while (received < length) {
            //setup buffer
            if (received + BUFFER_SIZE > length) {
                //use a smaller buffer as to not overread!
                buffer = new byte[(length - received)];
            } else {
                buffer = new byte[BUFFER_SIZE];
            }

            int read = in.read (buffer);
            if (read == -1) {
                //this connection has been closed
                throw new EOFException ("readStream() == -1");
            } else {
                buf.put (buffer, 0, read);
                received += read;
            }
        }

        return buf.array ();
    }

    public void addShutdownListener (ShutdownListener shutdownListener) {
        if (shutdownListener != null) {
            shutdownListeners.add (shutdownListener);
        }
    }

    /**
     * Sends test message to test if Communicators have successfully connected.
     *
     * @return false if test acknowledge response is not received; true
     * otherwise.
     */
    public boolean testConnection () {
        testingConnection = true;
        BeamMessage msg = send (new SystemMessage (SystemMessageType.TEST_CONNECTION), waitTime ());
        testingConnection = false;

        return msg != null;
    }

    private byte[] getHeader (BeamMessage msg, int messageSize) {
        ByteBuffer header = ByteBuffer.allocate (BeamMessage.HEADER_SIZE);
        header.putInt (msg.getType ()); //message type
        header.putInt (messageSize);//message size
        header.putLong (msg.getMessageId ());//message id

        //data type
        if (msg.isRawData ()) {
            header.put ((byte) 1);
        } else {
            header.put ((byte) 0);
        }

        return header.array ();
    }

    private void send0 (BeamMessage msg, boolean outputException) {
        if (msg == null) {
            throw new NullPointerException ();
        }

        try {
            synchronized (outLock) {
                byte[] data = msg.getData ();
                byte[] header = getHeader (msg, data.length);
                byte[] headerWithData = new byte[BeamMessage.HEADER_SIZE + data.length];

                System.arraycopy (header, 0, headerWithData, 0, header.length);
                System.arraycopy (data, 0, headerWithData, BeamMessage.HEADER_SIZE, data.length);

                writeStream (headerWithData);

                if (debugOutput) {
                    if (msg.isSystemMessage ()) {
                        System.out.println (String.format ("Sent message: %s - Size: %s - Timestamp: %s",
                                systemMessageType.getName (msg.getType ()), data.length, new Timestamp (System.currentTimeMillis ())));
                    } else if (messageType != null) {
                        System.out.println (String.format ("Sent message: %s - Size: %s - Timestamp: %s",
                                messageType.getName (msg.getType ()), data.length, new Timestamp (System.currentTimeMillis ())));
                    } else {
                        System.out.println (String.format ("Sent message: %s - Size: %s - Timestamp: %s",
                                msg.getType (), data.length, new Timestamp (System.currentTimeMillis ())));
                    }
                }
            }
        } catch (IOException ex) {
            if (outputException) {
                ex.printStackTrace ();
            }
        }

        if (tunnelClient != null) {
            tunnelClient.getInBound ().resetBackoffTime ();
        }
    }

    public BeamMessage send (BeamMessage msg) {
        return send (msg, waitTime ());
    }

    public BeamMessage send (BeamMessage msg, int waitTime) {
        return send (msg, waitTime, 0);
    }

    public BeamMessage send (BeamMessage msg, int waitTime, int retryCount) {
        return send (msg, waitTime, retryCount, new int[] {msg.getType ()});
    }

    public BeamMessage send (BeamMessage msg, int waitTime, int retryCount, int... responseTypes) {
        return send0 (msg, waitTime, retryCount, responseTypes);
    }

    private BeamMessage send0 (BeamMessage msg, int waitTime, int retryCount, int... responseTypes) {
        if (msg == null) {
            return null;
        }

        if (msg.getMessageId () == -1) {
            msg.setMessageId (getUnusedMessageId ());
        }

        BeamMessage rtnMsg = null;
        for (int i = -1; i < retryCount; i++) {
            //add ImmediateListener first
            final ImmediateHandler listen = new ImmediateHandler (
                    msg.getMessageId (), msg.isSystemMessage (), waitTime, responseTypes);
            immediateHandlers.add (listen);

            //queue out msg
            queue (msg);

            //wait for response
            rtnMsg = listen.waitForMessage ();

            //now remove and return
            immediateHandlers.remove (listen);

            //check listener once more just in case
            if (rtnMsg == null && listen.message != null) {
                rtnMsg = listen.message;
            }

            if (rtnMsg != null) {
                if (msg instanceof EncryptedBeamMessage) {
                    //use encryption method in msg to decrypt rtnMsg
                    EncryptedBeamMessage encryptedMessage = (EncryptedBeamMessage) msg;
                    rtnMsg = encryptedMessage.decryptBeamMessage (rtnMsg);
                }

                //got return message
                break;
            }
        }

        return rtnMsg;
    }

    public RawDataChannel createRawDataChannel () {
        long rawChannelId = getUnusedMessageId ();
        RawDataChannel rawChannel = new RawDataChannel (new SystemCommunicator (this), rawChannelId);

        return rawChannel;
    }

    public FileTransferChannel createFileTransferChannel () {
        long rawChannelId = getUnusedMessageId ();
        FileTransferChannel transferChannel = new FileTransferChannel (new SystemCommunicator (this), rawChannelId);

        return transferChannel;
    }

    public BeamMessage fetch (int... responseTypes) {
        if (responseTypes == null || responseTypes.length == 0) {
            return null;
        }

        //look through unhandled messages first
        BeamMessage rtnMessage = null;
        int index = 0;
        boolean found = false;

        for (BeamMessage msg : getUnhandledMessages ()) {
            for (int type : responseTypes) {
                if (msg.getType () == type) {
                    found = true;
                    rtnMessage = msg;
                    break;
                }
            }
            if (found) {
                break;
            }
            index++;
        }

        if (found) {
            //found their message in the unhandled messages.
            //remove it and return it
            unhandledMessages.remove (index);

            return rtnMessage;
        }

        return null;
    }

    private long getUnusedMessageId () {
        boolean gotId = false;
        long messageId = -1;

        while (!gotId) {
            messageId = Generator.randomLong ();
            boolean inUse = false;

            //ensure message id isn't in use
            List<ImmediateHandler> handlerList = new ArrayList<> (immediateHandlers);
            for (ImmediateHandler handler : handlerList) {
                if (handler.acceptsMessageId (messageId)) {
                    inUse = true;
                    break;
                }
            }

            if (!inUse) {
                gotId = true;
            }
        }

        return messageId;
    }

    public void clearUnhandledMessages (int... messageTypes) {
        for (BeamMessage msg : getUnhandledMessages ()) {
            for (int type : messageTypes) {
                if (msg.getType () == type) {
                    unhandledMessages.remove (msg);
                    break;
                }
            }
        }
    }

    public BeamMessage fetchWithWait (int waitTime, int... responseTypes) {
        if (responseTypes == null || responseTypes.length == 0) {
            return null;
        }

        BeamMessage msg = fetch (responseTypes);
        if (msg != null) {
            return msg;
        } else if (waitTime != 0 || waitTime != UNDEFINED_WAIT) {
            //add ImmediateListener first
            final ImmediateHandler listen = new ImmediateHandler (
                    -1, false, waitTime, responseTypes);
            immediateHandlers.add (listen);

            final BeamMessage rtnMsg = listen.waitForMessage ();

            //now remove and return
            immediateHandlers.remove (listen);

            return rtnMsg;
        }

        return null;
    }

    /**
     * Send a message without waiting for a response.
     *
     * @param msg message to send.
     */
    public void queue (BeamMessage msg) {
        if (msg != null) {
            send0 (msg, true);
        }
    }

    public boolean addHandler (BeamHandler handler) {
        if (checkValid (handler)) {
            for (int type : handler.getTypes ()) {
                registeredHandlerIDs.add (type);
            }
            handlers.add (handler);

            BeamMessage msg = fetch (handler.getTypes ());
            while (msg != null) {
                processMessage (msg);
                msg = fetch (handler.getTypes ());
            }

            return true;
        }

        BeamMessage msg = fetch (handler.getTypes ());
        while (msg != null) {
            processMessage (msg);
            msg = fetch (handler.getTypes ());
        }
        return false;
    }

    protected void addSystemHandler (BeamHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException ("Invalid BeamHandler!");
        }

        systemHandlers.add (handler);
    }

    public void removeHandler (BeamHandler handler) {
        if (handler == null) {
            return;
        }

        handlers.remove (handler);

        for (int type : handler.getTypes ()) {
            registeredHandlerIDs.remove (type);
        }
    }

    /**
     * @return unique ID to specify this particular Communicator.
     */
    public long getUID () {
        return uid;
    }

    boolean isHandshakeComplete () {
        return handshakeComplete;
    }

    boolean isPerformingHandshake () {
        return performingHandshake;
    }

    private void processMessage (final BeamMessage message) {
        if (message.isSystemMessage ()) {
            handleSystemMessage (message);
        } else {
            handleMessage (message);
        }
    }

    private void handleSystemMessage (final BeamMessage message) {
        for (final BeamHandler handler : systemHandlers) {
            BeamMessage rtnMsg;
            if (handler.acceptsType (message.getType ())) {
                if (!handshakeComplete && message.getType () != SystemMessageType.BEAM_HANDSHAKE) {
                    //invalid message; handshake not complete!
                    System.out.println ("Handshake not complete! Invalid message: " + message.getType ());
                    return;
                }

                claimCommunicator ();
                if (isTunneled () || !handler.isBlockingHandler ()) {
                    new Thread (new Runnable ()
                    {

                        @Override
                        public void run () {
                            BeamMessage rtnMsg;
                            if ((rtnMsg = handler.processMessage (Communicator.this, message)) != null) {
                                queue (rtnMsg);
                            }
                        }
                    }).start ();
                } else {
                    if ((rtnMsg = handler.processMessage (this, message)) != null) {
                        queue (rtnMsg);
                    }
                }
                unclaimCommunicator ();
                return;
            }
        }

        //system couldn't handle message. pass to program
        handleMessage (message);
    }

    void handleMessage (final BeamMessage message) {
        if (!handshakeComplete) {
            //invalid message; handshake not complete!
            System.out.println ("Handshake not complete! Invalid message: " + message.getType ());
            return;
        }

        for (final ImmediateHandler immediateHandler : immediateHandlers) {
            if (immediateHandler.acceptsType (message.getType ())
                    && immediateHandler.acceptsMessageId (message.getMessageId ())) {
                claimCommunicator ();
                if (isTunneled () || !immediateHandler.isBlockingHandler ()) {
                    new Thread (new Runnable ()
                    {

                        @Override
                        public void run () {
                            immediateHandler.messageReceived (Communicator.this, message);
                        }
                    }).start ();
                } else {
                    immediateHandler.messageReceived (this, message);
                }
                unclaimCommunicator ();
                return;
            }
        }

        for (final BeamHandler handler : handlers) {
            BeamMessage rtnMsg;
            if (handler.acceptsType (message.getType ())) {
                claimCommunicator ();
                if (isTunneled () || !handler.isBlockingHandler ()) {
                    new Thread (new Runnable ()
                    {

                        @Override
                        public void run () {
                            BeamMessage rtnMsg;
                            if ((rtnMsg = handler.processMessage (Communicator.this, message)) != null) {
                                queue (rtnMsg);
                            }
                        }
                    }).start ();
                } else {
                    if ((rtnMsg = handler.processMessage (this, message)) != null) {
                        queue (rtnMsg);
                    }
                }
                unclaimCommunicator ();
                return;
            }
        }

        //if it reaches here then we couldn't find anywhere to put the message.
        //add it to "lost & found" and hope someone comes to pick it up (fetch()).
        if (debugOutput) {
            if (message.isSystemMessage ()) {
                System.out.println (String.format (
                        "Unable to find handler for system message: %s", systemMessageType.getName (message.getType ())));
            } else if (messageType != null) {
                String type = messageType.getName (message.getType ());
                if (type != null) {
                    System.out.println (String.format (
                            "Unable to find handler for message: %s", type));
                } else {
                    System.out.println (String.format (
                            "Unable to find handler for message of type: %s", message.getType ()));
                }
            } else {
                System.out.println (String.format (
                        "Unable to find handler for message of type: %s", message.getType ()));
            }
        }

        unhandledMessages.add (message);
    }

    public List<BeamMessage> getUnhandledMessages () {
        return new ArrayList<> (unhandledMessages);
    }

    public boolean isRunning () {
        return running;
    }

    public boolean isServerCommunicator () {
        return serverCommunicator;
    }

    private void preConnection () {
        for (ConnectionStateListener sl : statusListeners) {
            sl.preConnection (this);
        }
    }

    private void postConnection () {
        for (ConnectionStateListener sl : statusListeners) {
            sl.postConnection (this);
        }

        if (tunnelClient != null) {
            tunnelClient.close ();
        }
    }

    public void setDebugOutput (boolean debugOutput) {
        this.debugOutput = debugOutput;
    }

    public boolean isDebugOutput () {
        return debugOutput;
    }

    public void setAttribute (String name, Object objectToStore) {
        attributes.put (name, objectToStore);
    }

    public Object getAttribute (String name) {
        return attributes.get (name);
    }

    public HashMap<String, Object> getAttributes () {
        return new HashMap<String, Object> (attributes);
    }

    public void close () {
        if (running) {
            userClosed = true;
            running = false;

            if (!socket.isOutputShutdown ()) {
                final BeamMessage closeMsg
                        = new SystemMessage (SystemMessageType.CLOSE_CONNECTION);
                send0 (closeMsg, false); //same as queue without exception output
            }

            //send null (a.k.a alert) to any waiting listeners
            for (ImmediateHandler immediateHandler : immediateHandlers) {
                immediateHandler.messageReceived (this, null);
            }

            try {
                out.close ();
            } catch (IOException ex) {
                //ignore
            }

            try {
                in.close ();
            } catch (IOException ex) {
                //ignore
            }

            postConnection ();
        }
    }

    public void shutdownNotice (String message) {
        if (!serverCommunicator) {
            //only server can do
            throw new IllegalStateException ("Only a server can send a shutdown notice!");
        }

        try {
            synchronized (outLock) {
                final BeamMessage closeMsg
                        = new SystemMessage (SystemMessageType.SHUTDOWN_NOTICE);
                queue (closeMsg);

                if (message == null || message.length () == 0) {
                    //no message to send
                    out.write (toBytes (0)); //size
                    out.flush ();
                } else {
                    out.write (toBytes (message.length ())); //size
                    out.write (message.getBytes ("UTF-8")); //message
                    out.flush ();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace ();
        }
    }

    public void clearHandlers () {
        handlers.clear ();
        registeredHandlerIDs.clear ();
    }

    public void addConnectionStateListener (ConnectionStateListener stateListener) {
        statusListeners.add (stateListener);
    }

    public boolean isTunneled () {
        if (serverCommunicator) {
            return clientTunneled;
        } else {
            return tunnelClient != null;
        }
    }

    public boolean isUserClosed () {
        return userClosed;
    }

    public boolean isSSLSocket () {
        return socket instanceof SSLSocket;
    }

    public synchronized boolean isClaimed () {
        return claimed;
    }

    private synchronized boolean claimCommunicator () {
        if (!isClaimed ()) {
            claimed = true;

            return true;
        }

        return false;
    }

    private synchronized void unclaimCommunicator () {
        claimed = false;

        //communicator unclaimed. send out any queued messages
        if (!queue.isEmpty ()) {
            BeamMessage msg;
            while ((msg = queue.poll ()) != null) {
                queue (msg);
            }
        }
    }

    public InetAddress getInetAddress () {
        return socket.getInetAddress ();
    }

    public int getPort () {
        return socket.getPort ();
    }

    public static long getUIDCounter () {
        return UIDCounter;
    }

    public void setSocketTimeout (int timeout) {
        try {
            if (socket != null) {
                socket.setSoTimeout (timeout);
            }
        } catch (SocketException ex) {
            ex.printStackTrace ();
        }
    }

    boolean isTestingConnection () {
        return testingConnection;
    }

    void captureHandshake (String clientVersion, long clientTimeDiff, boolean clientTunneled) {
        this.clientVersion = clientVersion;
        this.clientTimeDiff = clientTimeDiff;
        this.clientTunneled = clientTunneled;

        performingHandshake = false;
        handshakeComplete = true;
    }

    public static String getExternalIPAddress () {
        String hostAddress = null;
        try {
            hostAddress = InetAddress.getLocalHost ().getHostAddress ();

            URL connection = new URL ("http://checkip.amazonaws.com/");
            URLConnection con = connection.openConnection ();
            String str;
            BufferedReader reader = new BufferedReader (new InputStreamReader (con.getInputStream (), "UTF-8"));
            str = reader.readLine ();
            reader.close ();

            hostAddress = str;
        } catch (IOException ex) {
            ex.printStackTrace ();
        }

        return hostAddress;
    }

    private class ImmediateHandler extends SystemHandler
    {

        private final long responseMessageId;
        private boolean isWaiting;
        private int waitTime;
        private boolean waitForever;
        private BeamMessage message;

        public ImmediateHandler (long responseMessageId) {
            super (false, SystemMessageType.RAW_DATA_CONNECTION);

            this.responseMessageId = responseMessageId;
            isWaiting = true;
            waitTime = WAIT_FOREVER;
            waitForever = true;
        }

        public ImmediateHandler (long responseMessageId, boolean systemHandler, int waitTime, int... responseTypes) {
            super (systemHandler, responseTypes);

            isWaiting = true;
            this.responseMessageId = responseMessageId;
            this.waitTime = waitTime;
            if (waitTime == WAIT_FOREVER) {
                waitForever = true;
            }
        }

        public BeamMessage waitForMessage () {
            while (isWaiting && running) {
                try {
                    Thread.sleep (250);
                    if (!waitForever) {
                        waitTime -= 250;
                        if (waitTime < 1) {
                            break;
                        }
                    }

                    //check unhandled messages just in case its in there
                    for (BeamMessage msg : getUnhandledMessages ()) {
                        if ((responseMessageId == -1 && acceptsType (msg.getType ()))
                                || (responseMessageId != -1 && acceptsMessageId (msg.getMessageId ()))) {
                            Communicator.this.unhandledMessages.remove (msg);
                            return msg;
                        }
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace ();
                }
            }

            return message;
        }

        @Override
        public BeamMessage messageReceived (SystemCommunicator comm, BeamMessage msg) {
            if (msg == null) {
                //alert message. need to finish
                isWaiting = false;
            } else if (responseMessageId == -1) {
                //accept any message of matching message type
                message = msg;
                isWaiting = false;
            } else if (acceptsMessageId (msg.getMessageId ())) {
                //accept any message of matching response id
                message = msg;
                isWaiting = false;
            } else {
                //we may be looking for this type of message type but response id isn't for us;
                //add unhandled and continue
                Communicator.this.unhandledMessages.add (msg);
            }

            return null; //no response
        }

        private boolean acceptsMessageId (long messageId) {
            if (responseMessageId == -1) {
                return true;
            }

            return responseMessageId == messageId;
        }

    }

    private byte[] toBytes (int value) {
        return ByteBuffer.allocate (4).putInt (value).array ();
    }

    private int intFromBytes (byte[] bytes) {
        return ByteBuffer.wrap (bytes).getInt ();
    }

    private int intFromBytes (byte[] bytes, int offset, int length) {
        return ByteBuffer.wrap (bytes, offset, length).getInt ();
    }

    private byte[] toBytes (long value) {
        return ByteBuffer.allocate (8).putLong (value).array ();
    }

    private long longFromBytes (byte[] bytes) {
        return ByteBuffer.wrap (bytes).getLong ();
    }

    @Override
    public boolean equals (Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass () != obj.getClass ()) {
            return false;
        }

        final Communicator other = (Communicator) obj;

        return this.uid == other.uid;
    }

    @Override
    public int hashCode () {
        int hash = 7;
        hash = 97 * hash + (int) (this.uid ^ (this.uid >>> 32));
        return hash;
    }

}
