## Name

netstat

## Synopsis

```sh
$ netstat [--all] [--list] [--tcp] [--udp] [--numeric] [--program] [--wide]
```

## Description

Display network connections

## Options

* `-a`, `--all`: Display both listening and non-listening sockets
* `-l`, `--list`: Display only listening sockets
* `-t`, `--tcp`: Display only TCP network connections
* `-u`, `--udp`: Display only UDP network connections
* `-n`, `--numeric`: Display numerical addresses
* `-p`, `--program`: Show the PID and name of the program to which each socket belongs
* `-W`, `--wide`: Do not truncate IP addresses by printing out the whole symbolic host

<!-- Auto-generated through ArgsParser -->
