## Name

ping - send ICMP ECHO_REQUEST packets to network hosts

## Synopsis

```sh
$ ping [--count count] [-i interval] [--size size] [--quiet] <host>
```

## Options

* `-c count`, `--count count`: Stop after sending specified number of ECHO_REQUEST packets.
* `-i interval`: Wait `interval` seconds between sending each packet. Fractional seconds are allowed.
* `-s size`, `--size size`: Amount of bytes to send as payload in the ECHO_REQUEST packets.
* `-q`, `--quiet`: Quiet mode. Only display summary when finished.

## Arguments

* `host`: Host to ping
