## Name

traceroute - Trace a packet's route

## Synopsis

```**sh
$ traceroute [options...] destination
```

## Description

`traceroute` sends a packet to `destination`, and prints all hosts ("hops") it passes by.
It does so by sending ping packets with a custom Time To Live, starting at 1 until `tries`, or 30 by default.
When a hop receives a packet, it lowers its Time To Live by 1 before handing it to the next hop.
If it reaches 0, the hop sends back an ICMP error.
`traceroute` increments the TTL on each iteration, until the packet reaches `destination`.

## Options

* `-h hops`, `--max-hops hops`: use at most `hops` to the destination (defaults to 30)
* `-r tries`, `--max-retries tries`: retry TTL at most `tries` times (defaults to 3, at most 255)
* `-t seconds`, `--timeout seconds`: wait at most `seconds` for a response (defaults to 5)

## Arguments

* `destination`: Destination host

## Example

Trace packets to serenityos.org:
```sh
$ traceroute serenityos.org
```

Try at most 255 hops because your packets like to travel a lot:
```sh
$ traceroute -h 255 serenityos.net
```
