## Name

SystemServer - services configuration

## Synopsis

`/etc/SystemServer.ini`

## Description

SystemServer configuration consists of a list of *services* for it to spawn.

Each service is configured as a section in the configuration file, where the
section name is the service name and the keys inside the section are the options
describing how to launch and manage this service.

## Options

* `Executable` - an executable to spawn. If no explicit executable is specified, SystemServer assumes `/bin/{service name}` (for example, `/bin/WindowServer` for a service named `WindowServer`).
* `Arguments` - a space-separated list of arguments to pass to the service as `argv` (excluding `argv[0]`). By default, SystemServer does not pass any arguments other than `argv[0]`.
* `StdIO` - a path to a file to be passed as standard I/O streams to the service. By default, services inherit SystemServer's own standard I/O streams, which are normally set to `/dev/tty0`.
* `Priority` - the scheduling priority to set for the service, either "low", "normal", or "high". The default is "normal".
* `KeepAlive` - whether the service should be restarted if it exits or crashes. For lazy services, this means the service will get respawned once a new connection is attempted on their socket after they exit or crash.
* `Lazy` - whether the service should only get spawned once a client attempts to connect to their socket.
* `Socket` - a path to a socket to create on behalf of the service. For lazy services, SystemServer will actually watch the socket for new connection attempts. An open file descriptor to this socket will be passed as fd 3 to the service.
* `User` - a name of the user to run the service as. This impacts what UID, GID (and extra GIDs) the service processes have. By default, services are run as root.

## Environment

* `SOCKET_TAKEOVER` - set by the SystemServer for a service if the service is being passed a socket.

## Examples

```ini
# Spawn the terminal as user anon once on startup.
[Terminal]
User=anon

# Set up a socket at /tmp/portal/lookup; once a connection attempt
# is made spawn the LookupServer as user anon with a low priority.
# If it exits or crashes, repeat.
[LookupServer]
Socket=/tmp/portal/lookup
Lazy=1
Priority=low
KeepAlive=1
User=anon

# Spawn the TTYServer on /dev/tty1 once on startup with a high priority,
# additionally passing it "tty1" as an argument.
[TTYServer]
Arguments=tty1
StdIO=/dev/tty1
Priority=high
```

## See also

* [`SystemServer`(7)](../man7/SystemServer.md)
