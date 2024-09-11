## Name

TelnetServer - Serenity telnet server

## Synopsis

```sh
$ TelnetServer [-p port] [-c command]
```

## Description

TelnetServer is a basic telnet server for Serenity. By default, it
runs on port 23 and provides a shell upon connection. This program
must be run as root.

## Options

-   `--help`: Display help message and exit
-   `--version`: Print version
-   `-p port`: Port to listen on
-   `-c command`: Program to run on connection

## Examples

```sh
$ TelnetServer -p 24 -c "/usr/bin/nyancat -f 60"
```
