Reflect about your solution!

Summary:
server-side:
The chatserver-class accepts new connections. For each connection a TCPListener
will be created, which are handled within the ServerInfo implementation.
For handling connections a FixedThreadPool is used. UDP requests are handled
through the UDPListener.
For I/O-operations the pre-supplied Shell-class is used.

client-side:
The server-side sends UDP information through the ServerCommunicationUDP
class. The TCP connection is handled through another way:

ServerListener      <- listens to information on the TCP-socket
ServerCommunication <- separates public-messages and replies

ClientCommunication <- accepts requests from other clients (started through
						register)
ClientListener      <- listens for messages from a client (one for each 
						client - started by ClientCommunication)
ClientSender        <- sends a message towards a target client

For I/O-operations the pre-supplied Shell-class is used. The client handles
the target socket information through a map and the incoming listening 
threads through a FixedThreadPool.


The FixedThreadPool is used because we know the maximum number of connections
(we have 2 users, so we need a maximum of 2 threads) we can expect, furthermore
we can easily expand for more users. Should our knowledge about users change
we can change to a BufferedThreadPool or similar ones.
The ThreadPool is generally used because the handling of multiple threads
is way easier this way.
