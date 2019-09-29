## Name

TelnetServer - Serenity telnet server

## Synopsis

```**sh
# TelnetServer [options]
```

## Description

TelnetServer is a basic telnet server for Serenity. By default, it
runs on port 23 and provides a shell upon connection.

## Options

* `-p`: Choose different port for TelnetServer to attach to.
* `-c`: Choose different program for TelnetServer to run on connection. Arguments can be passed to the program.

## Examples

```sh
# TelnetServer -p 24 -c "/usr/bin/nyancat -f 60"
```
