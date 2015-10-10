Beam is client/server and peer-to-peer compatible networking library. It provides the ability to create large network systems with ease using language-independent messaging protocols. With Beam you can create a server to handle many concurrent connections as well as connect clients to each other for P2P connections. Beam provides the ability for P2P connections through UDP hole punching and UPnP. Message serialization is currently performed by Google's Protocol Buffers (MessagePack and others will be available soon). 

## Overview ##
* Creating a Server
* Message Types
* Message Handlers
* Connecting a Client
* Creating Messages
* Sending Messages
* Exchanging Messages
* Queuing Messages
* Broadcasting Messages
* Secure Communication
	* AES
	* RSA
* P2P Connection
	* UPNP Connection
	* UDP Hole Punching


## Creating a Server ##
This code starts a server on TCP port 45800
```java
BeamServer server = new BeamServer (45800);
server.start ();
```

## Message Handlers ##

A Beam server works with message handlers. Each handler has the ability to accept one or more different types of messages

```java
public class ExampleHandler extends BasicHandler
{

	public ExampleHandler () {
		super (ExampleMessage.EXAMPLE_MESSAGE_ID);
	}

	@Override
	public BeamMessage messageRecieved (Communicator comm, BasicMessage msg) {
		System.out.println ("Client sent message: " + msg.getString ("client_message"));

		//response message
		return msg.emptyResponse ().setString ("server_response", "example_reply_message");
	}
}
```

Note: Most handlers extend BeamHandler. BasicHandler is used in this example to allow for easier message manipulation.

Now that you have created a handler you will need to add that handler to the server.
```java
server.addHandler (ExampleHandler.class);
```

Note: Add handlers to server before starting to ensure it receives all messages.


## Connecting a Client ##

Starting a server creates a thread to handle incoming connections, reading/writing to the socket, and notifying handlers. Now that we have a server created and listening we will need to connect a client to it.

This code connects a client to a server at 127.0.0.1 on TCP port 45800

```java
BeamClient client = new BeamClient (127.0.0.1, 45800);
client.connect ();
```

## Creating Messages ##

```java
public class ExampleMessage extends BasicMessage
{
	public final static int EXAMPLE_MESSAGE_ID = 1000;
	
	public ExampleMessage () {
		super (EXAMPLE_MESSAGE_ID);
	}

	public ExampleMessage (BeamMessage message) {
		super (message);
	}

}

```

Note: Most messages extends BeamMessage. BasicMessage is being extending in this example to allow for easier message manipulation.

## Sending Messages ##

Sending a message will send a message and block while waiting for a response.

To create and send a BasicMessage to the server from client
```java
ExampleMessage exampleMessage = new ExampleMessage ();
exampleMessage.setString ("client_message", "example_client_message");
BeamMessage responseMessage = client.sendMessage (exampleMessage);

//convert and output server response
exampleMessage = new ExampleMessage (responseMessage);
System.out.println ("Server response: " + exampleMessage.getString ("server_response"));
```

## Exchanging Messages ##

Exchanging a message will send a message and update the original message with the response message given. Useful when the request and response message are the same class as it avoids unnessecary casting like when sending messages.

To create and exchange a BasicMessage to the server from client
```java
ExampleMessage exampleMessage = new ExampleMessage ();
exampleMessage.setString ("client_message", "example_client_message");
if (client.exchangeMessage (exampleMessage)) {
	//output server response
	System.out.println (exampleMessage.getString ("server_response"));
} else {
	System.out.println ("Request timed out!");
}
```

## Queuing Messages ##

Queuing a message sends a message without waiting for a response.

To create and queue a BasicMessage to the server from client
```java
ExampleMessage exampleMessage = new ExampleMessage ();
exampleMessage.setString ("client_message", "example_client_message");

//queue message. no need to wait for response
client.queueMessage (exampleMessage);
```

## Broadcasting Messages ##

As a server you can broadcast messages to all connected clients
```java
ExampleMessage exampleMessage = new ExampleMessage ();
exampleMessage.setString ("example_broadcast", "test_value");

server.broadcast (exampleMessage);
```

## Secure Communication ##

### AES ####

Client
```java
AES aes = new AES ("password");
BeamMessage aesMessage = new AESBeamMessage (aes, ENCRYPTED_MESSAGE_ID);
aesMessage.set ("secret_variable", "secret_value");

//send and receive response (response is returned decrypted)
BeamMessage responseMessage = client.sendMessage (aesMessage);
System.out.println ("Server response: " + responseMessage.get ("server_response");
```

Server
```java
public class ExampleAESHandler extends AESBeamHandler
{

	public ExampleAESHandler (AES aes) {
		super (aes, ENCRYPTED_MESSAGE_ID);
	}

	@Override
	public BeamMessage messageRecieved (Communicator comm, BeamMessage message) {
		System.out.println ("Client sent secret: " + message.get ("secret_variable"));

		BasicMessage responseMessage = new BasicMessage (ENCRYPTED_MESSAGE_ID);
		responseMessage.setString ("server_response", "server_secret_value");
		return responseMessage;
	}

}
```

### RSA ####


Client
```java
RSA rsa = new RSA (1024);
RSAConnection rsaConn = client.establishRSAConnection (rsa);
BeamMessage rsaMessage = new RSABeamMessage (rsaConn, ENCRYPTED_MESSAGE_ID);
rsaMessage.set ("secret_variable", "secret_value");

//send and receive response (response is returned decrypted)
BeamMessage responseMessage = client.sendMessage (rsaMessage);
System.out.println ("Server response: " + responseMessage.get ("server_response");
```

Server
```java
public class ExampleRSAHandler extends RSABeamHandler
{

	public ExampleRSAHandler (RSA rsa) {
		super (ENCRYPTED_MESSAGE_ID);
	}

	@Override
	public BeamMessage messageRecieved (Communicator comm, BeamMessage message) {
		System.out.println ("Client sent secret: " + message.get ("secret_variable"));

		BasicMessage responseMessage = new BasicMessage (ENCRYPTED_MESSAGE_ID);
		responseMessage.setString ("server_response", "server_secret_value");
		return responseMessage;
	}

}
```

Note: Server must add a RSAHandshakeHandler with the private RSA
```java
server.addRSAHandshakeHandler (new RSAHandshakeHandler (rsa));
```

## P2P Connection ##

### UPNP Connection ###

Client A
```java
//opens TCP port 45800 on Client A
UPNPControl upnpControl = new UPNPControl (ConnectionProtocol.TCP);
upnpControl.addPortMapping ("Example UPNP Connection", 45800, 45800);

//receive message from Client B
BeamMessage message = peerComm.fetchWithWait (Communicator.WAIT_FOREVER, EXAMPLE_UPNP_MESSAGE);
BasicMessage basicMessage = new BasicMessage (message);
System.out.println ("Client B sent: " + basicMessage.getString ("client_message"));

//queue response to Client B
peerComm.queue (basicMessage.emptyResponse ().setString ("client_message", "Hello from Client A!"));
```

Client B
```java
//connect to client A directly on port 45800 (which is open via UPNP)
BeamClient client = new BeamClient (CLIENT_IP_ADDRESS, 45800);
client.connect ();

//send message to Client A
BasicMessage message = new BasicMessage (EXAMPLE_UPNP_MESSAGE);
message.setString ("client_message", "Hello from Client B!");
BeamMessage rtnMessage = peerComm.send (message);

//convert and output Client A response
message = new BasicMessage (rtnMessage);
System.out.println ("Client A sent: " + basicMessage.getString ("client_message"));
```

### UDP Hole Punching ###

Client A
```java
UDPHoleClient holeClient = new UDPHoleClient (PUNCH_SERVER_HOST, PUNCH_SERVER_PORT);
Communicator peerComm = holeClient.createHoleCommunicator (PEER_IDENTIFIER, PEER_ACCESS_CODE);

//receive message from Client B
BeamMessage message = peerComm.fetchWithWait (Communicator.WAIT_FOREVER, EXAMPLE_PUNCH_MESSAGE);
BasicMessage basicMessage = new BasicMessage (message);
System.out.println ("Client B sent: " + basicMessage.getString ("client_message"));

//queue response to Client B
peerComm.queue (basicMessage.emptyResponse ().setString ("client_message", "Hello from Client A!"));
```

Client B
```java
UDPPunchClient punchClient = new UDPPunchClient (PUNCH_SERVER_HOST, PUNCH_SERVER_PORT);
Communicator peerComm = punchClient.punchPeerCommunicator (PEER_IDENTIFIER, PEER_ACCESS_CODE);

if (peerComm == null) {
	System.out.println ("Could not find peer to connect to! Make sure peer is waiting with hole request...");
	return;
}

//send message to Client A
BasicMessage message = new BasicMessage (TEST_PUNCH_MESSAGE_TYPE);
message.setString ("client_message", "Hello from Client B!");
BeamMessage rtnMessage = peerComm.send (message);

//convert and output Client A response
message = new BasicMessage (rtnMessage);
System.out.println ("Client A sent: " + basicMessage.getString ("client_message"));
```
