## Name

nc

## Synopsis

```sh
$ nc [--listen] [--verbose] [--udp] [-N] [--length ] <target> <port>
```

## Description

Network cat: Connect to network sockets as if it were a file.

## Options:

* `--help`: Display help message and exit
* `--version`: Print version
* `-l`, `--listen`: Listen instead of connecting
* `-v`, `--verbose`: Log everything that's happening
* `-u`, `--udp`: UDP mode
* `-N`: Close connection after reading stdin to the end
* `-I`, `--length`: Set maximum tcp receive buffer size

## Arguments:

* `target`: Address to listen on, or the address or hostname to connect to
* `port`: Port to connect to or listen on

<!-- Auto-generated through ArgsParser -->
