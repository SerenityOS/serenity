## Name

host - DNS lookup utility

## Synopsis

```**sh
$ host <name>
```

## Description

`host` is a simple utility for performing DNS lookups. It is used to
convert names to IP addresses and vice versa.

`name` is the domain name that is to be looked up. It can also be a
dotted-decimal IPv4 address, in which case `host` will perform a reverse
lookup for that address.

## Examples

```sh
$ host github.com
$ host 8.8.8.8
```
