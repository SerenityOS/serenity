## Name

ping - send ICMP ECHO_REQUEST packets to network hosts

## Synopsis

```sh
$ ping [--count count] [-i interval] [-W interval] [--size size] [--quiet] [-t ttl] [--adaptive] [--flood] <host>
```

## Options

-   `--help`: Display help message and exit.
-   `--version`: Print version.
-   `-c count`, `--count count`: Stop after sending specified number of ECHO_REQUEST packets.
-   `-i interval`: Wait `interval` seconds between sending each ECHO_REQUEST packet. Fractional seconds are allowed. Only super-user can set the interval value less than 0.2 seconds.
-   `-W interval`: Time in seconds to wait for a reply for each packet sent. Fractional seconds are allowed. 0 means infinite timeout.
-   `-s size`, `--size size`: Amount of bytes to send as payload in the ECHO_REQUEST packets.
-   `-q`, `--quiet`: Quiet mode. Only display the summary when finished.
-   `-t`, `--ttl`: Set the TTL(time-to-live) value of the ICMP packets.
-   `-A`, `--adaptive`: Adaptive ping. The interval between each ECHO_REQUEST adapts to the RTT(round-trip-time).
-   `-f`, `--flood`: Flood ping. For every ECHO_REQUEST sent a period '.' is printed, while for every ECHO_REPLY received a backspace is printed. This provides a rapid display of how many packets are being dropped. If interval is not given, it sets interval to 0 and outputs packets as fast as they come back. Only super-user may use this option with 0 interval.

## Arguments

-   `host`: Host to ping
