## Name

nc

## Synopsis

```sh
$ nc [--length ] [--listen] [-N] [-n] [--udp] [-p port] [--verbose] [target] [port]
```

## Description

Network cat: Connect to network sockets as if it were a file.

## Options

-   `-I`, `--length`: Set maximum tcp receive buffer size
-   `-l`, `--listen`: Listen instead of connecting
-   `-N`: Close connection after reading stdin to the end
-   `-n`: Suppress name resolution
-   `-u`, `--udp`: UDP mode
-   `-p port`: Local port for remote connections
-   `-v`, `--verbose`: Log everything that's happening
-   `-z`, `--test-listening-service`: Test a TCP-listening service

## Arguments

-   `target`: Address to listen on, or the address or hostname to connect to
-   `port`: Port to connect to or listen on

<!-- Auto-generated through ArgsParser -->
