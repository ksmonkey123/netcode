# Changelog
_Last updated: 2018/09/19_

## 2.1.0
 - Added `UserRef` support.

## 2.0.1
 - Fixed a Bug where client questions and answers would leak into the normal message processing.
 - Delayed C/Q processing during setup until after the initial handshake has been completed but before any queued messages get processed.
 - Added `NetcodeClientFactory` cloning.
 - Fixed a Bug where the server would attempt to deserialize the message and fail unless only JRE types are involved.

## 2.0.0
 - Introduced Public Channels and Channel Discovery.
 - Introduced Client Questions. (private messages sent between clients that are not handled by the normal message queue and that require a response to be sent back)
 - Introduced Simple Queries. (simple requests sent to the server to be responded to)
   - Providing a list of all public channels with open slots as a S/Q.
 - Introduced Server Commands. (simple requests sent to the server inside an open channel connection to be responded to.
   - Providing a list of all public channels with open slots as a S/C.
   - Providing up-to-date detailed information about the currently joined channel as a S/C.
 - Introduced Timeouts for C/Q and S/C (default: 60s).
 - Improved server-side performance by removing channel-level synchronisation.
 - Improved sequential consistency of messages during mode transitions by halting processing during the transition.
 - Added method chaining support to `NetcodeClientFactory`.

## 1.1.0
 - First truly stable release