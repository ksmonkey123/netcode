# netcode
The _Netcode_ library attempts to simplify communication between multiple instances of an application.
_Netcode_ essentially provides a chat service for machine-to-machine communication.

## Operation
_Netcode_ requires a single (publicly reachable) server instance. This server is the
central node that all application instances connect to. All instances (or clients) can create new
_channels_ or join existing _channels_. Since version 2.0.0 the _Netcode_ server also supports
_channel discovery_.

_Netcode_ allows a lot of customisation both for servers and for clients. This includes choosing between
_plaintext_, _SSL_ or _TLS_ sockets, defining acceptable _ciper suites_, defining _timeouts_, filtering
incoming connections based on a provided _appId_, disabling _public channels_ and _channel discovery_.

_Netcode_ is implemented in such a way that clients of any version can connect to servers of any version
and that clients of different versions can join the same channel. When a new client connects to a server
capability details are exchanged so that a client always knows what features a server supports.

## Protocol Description
In all the following examples it is assumed, that Bob is already connected to the server and all clients are on the same channel.
### New Client Joins
```plantuml
Alice->Server: (connect)
Server->Alice: serverCapabilities
Alice->Server: HandshakeRequest
Server->Alice: GreetingMessage
Server->Bob: UserChange: "Alice Joined"
```
### A Client Disconnects
```plantuml
Alice->Server: (disconnect)
Server->Bob: UserChange: "Alice Left"
```
### Sending Messages
The following example assumes _bouncing_ to be enabled.
```plantuml
Alice->Server: Message: "Hey Bob!"
Server->Alice: Message from Alice: "Hey Bob!"
Server->Bob: Message from Alice: "Hey Bob!"
Bob->Server: Message: "Hey Alice!"
Server->Alice: Message from Bob: "Hey Alice!"
Server->Bob: Message from Bob: "Hey Alice!"
Alice->Server: PrivateMessage to Bob: "secret"
Server->Bob: PrivateMessage from Alice: "secret"
```