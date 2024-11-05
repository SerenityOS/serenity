## Name

arp - Display or modify the system ARP cache

## Synopsis

```**sh
# arp
```

## Description

This program run as root displays IP and MAC addresses of devices in local network.

ARP stands for Address Resolution Protocol, which is used to find devices in local network.

## Options

-   `-s`, `--set`: Set an ARP table entry
-   `-d`, `--delete`: Delete an ARP table entry
-   `-n`, `--numeric`: Display numerical addresses. Don't resolve hostnames

## Arguments

-   `address`: IPv4 protocol address
-   `hwaddress`: Hardware address

## Examples

```sh
# arp
Address          HWaddress
192.168.1.1      52:54:00:12:34:56
```
