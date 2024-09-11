## Name

SystemServer - one server to rule them all

## Description

SystemServer is the first userspace process to be started by the kernel on boot.
Its main responsibility is spawning all the other servers and other programs
that need to be autostarted (referred to as **services**).

A service can be configured to be _kept alive_, in which case SystemServer will
respawn it if it exits or crashes. A service may also be configured as _lazy_,
in which case SystemServer won't spawn it immediately, but only once a client
connects to its socket (see **Socket takeover** below).

## Socket takeover

SystemServer can be configured to set up a socket on behalf of a service
(typically, an _IPC portal_ socket inside `/tmp/portal/`). SystemServer sets up
the configured sockets before spawning any services, preventing any races
between socket creation and the client trying to connect to those sockets.

When a service is spawned, SystemServer passes it an open file descriptor to the
configured socket as fd 3, and sets `SOCKET_TAKEOVER=1` in the environment to
inform the service that socket takeover is happening. SystemServer calls
[`listen`(2)](help://man/2/listen) on the file descriptor, so the service doesn't
need to do it again. The file descriptor does not have the `FD_CLOEXEC` flag set
on it.

The service is advised to set this flag using [`fcntl`(2)](help://man/2/fcntl) and
unset `SOCKET_TAKEOVER` from the environment in order not to confuse its
children.

LibCore provides `Core::LocalServer::take_over_from_system_server()` method that
performs the service side of the socket takeover automatically.

If a service is configured as _lazy_, SystemServer will actually listen on the
socket it sets up for the service, and only spawn the service once a client
tries to connect to the socket. The service should then start up and accept the
connection. This all happens transparently to the client. If a lazy service is
configured to be _kept alive_, it can even exit after some period of inactivity;
in this case SystemServer will respawn it again once there is a new connection
to its socket.

SystemServer can also be configured to accept connections on the socket and
spawn separate instances of the service for each accepted connection, passing
the accepted socket to the service process.

## See also

-   [`SystemServer`(5)](help://man/5/SystemServer)
