## Name

SystemServer - services configuration

## Synopsis

```**
/etc/SystemServer.ini
```

## Description

SystemServer configuration consists of a list of _services_ for it to spawn.

Each service is configured as a section in the configuration file, where the
section name is the service name and the keys inside the section are the options
describing how to launch and manage this service.

## Options

-   `Executable` - an executable to spawn. If no explicit executable is specified, SystemServer assumes `/bin/{service name}` (for example, `/bin/WindowServer` for a service named `WindowServer`).
-   `Arguments` - a space-separated list of arguments to pass to the service as `argv` (excluding `argv[0]`). By default, SystemServer does not pass any arguments other than `argv[0]`.
-   `StdIO` - a path to a file to be passed as standard I/O streams to the service. By default, services run with `/dev/null` for standard I/O.
-   `Priority` - the scheduling priority to set for the service, either "low", "normal", or "high". The default is "normal".
-   `KeepAlive` - whether the service should be restarted if it exits or crashes. For lazy services, this means the service will get respawned once a new connection is attempted on their socket after they exit or crash.
-   `Lazy` - whether the service should only get spawned once a client attempts to connect to their socket.
-   `Socket` - a comma-separated list of paths to sockets to create on behalf of the service. For lazy services, SystemServer will actually watch the socket for new connection attempts. See [socket takeover mechanism](#socket-takeover-mechanism) for details on how sockets are passed to services by SystemServer.
-   `SocketPermissions` - comma-separated list of (octal) file system permissions for the socket file. The default permissions are 0600. If the number of socket permissions defined is less than the number of sockets defined, then the last defined permission will be used for the remainder of the items in `Socket`.
-   `User` - a name of the user to run the service as. This impacts what UID, GID (and extra GIDs) the service processes have. By default, services are run as root.
-   `WorkingDirectory` - the working directory in which the service is spawned. By default, services are spawned in the root (`"/"`) directory.
-   `SystemModes` - a comma-separated list of system modes in which the service should be enabled. By default, services are only enabled in the "graphical" mode. The current system mode is read from the [kernel command line](help://man/7/boot_parameters#options), and is assumed to be "graphical" if not specified there.
-   `Environment` - a space-separated list of "variable=value" pairs to set in the environment for the service.
-   `MultiInstance` - whether multiple instances of the service can be running simultaneously.
-   `AcceptSocketConnections` - whether SystemServer should accept connections on the socket, and spawn an instance of the service for each client connection.

Note that:

-   `Lazy` requires `Socket`, but only one socket must be defined.
-   `SocketPermissions` require a `Socket`.
-   `MultiInstance` conflicts with `KeepAlive`.
-   `AcceptSocketConnections` requires `Socket` (only one), `Lazy`, and `MultiInstance`.

## Environment

-   `SOCKET_TAKEOVER` - set by SystemServer to describe the sockets being passed.

## Socket takeover mechanism

When SystemServer runs a service which has `Socket` defined, it will create the sockets and then pass an environment variable named `SOCKET_TAKEOVER` to the launched service. This environment variable is structured as follows:

```console
SOCKET_TAKEOVER=/tmp/socket1:3;/tmp/socket2:4
```

Items in the variable are separated by semicolons, and each item has two components separated by a colon. The first part is the path of the socket requested, and the second part is the file descriptor number that was passed to the newly created service. The service can then parse this information and obtain file descriptors for each socket.

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

# Launch the Shell on /dev/tty0 on startup when booting in text mode.
[Shell@tty0]
Executable=/bin/Shell
StdIO=/dev/tty0
Environment=TERM=xterm
KeepAlive=1
SystemModes=text

# Launch WindowManager with two sockets: one for main windowing operations, and
# one for window management operations. Both sockets get file permissions as 660.
[WindowServer]
Socket=/tmp/portal/window,/tmp/portal/wm
SocketPermissions=660
Priority=high
KeepAlive=1
User=window
```

## See also

-   [`SystemServer`(7)](help://man/7/SystemServer)
