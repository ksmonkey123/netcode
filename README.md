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