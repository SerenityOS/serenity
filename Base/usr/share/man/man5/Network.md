## Name

NetworkServer - network configuration

## Synopsis

```**
/etc/Network.ini
```

## Description

The Network configuration is loaded by NetworkServer service on startup. It consists of a list of interfaces, with adapter names as groups.

The interface that is not listed in this config file has DHCP enabled by default.

## Options

-   `Enabled` (default: `true`) - Whether the interface is enabled.
-   `DHCP` (default: `false`) - Whether the DHCP client should be run on this interface. This overrides static IP settings.
-   `IPv4Address` (default: `0.0.0.0`) - The static IPv4 address for the interface. Used only when `DHCP` is `false`.
-   `IPv4Netmask` (default: `0.0.0.0`) - The static IPv4 netmask for the interface. Used only when `DHCP` is `false`.
-   `IPv4Gateway` (default: `0.0.0.0`) - The static IPv4 default gateway for the interface. Used only when `DHCP` is `false`.

## Example

```ini
# Set static IP address to 10.0.0.5/8 and default gateway to 10.0.0.1
[ep1s0]
IPv4Address=10.0.0.5
IPv4Netmask=255.0.0.0
IPv4Gateway=10.0.0.1

# Try to run DHCP discovery on ep0s8. It is equivalent to not adding this entry at all.
[ep0s8]
DHCP=true

# Disable interface ep1s1 entirely.
[ep1s1]
Enabled=false
```

## See Also

-   [`ifconfig`(1)](help://man/1/ifconfig)
