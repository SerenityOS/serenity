## Name

lspci - list connected PCI devices

## Synopsis

```**sh
$ lspci
```

## Description

lspci is a utility for displaying information about PCI buses in the system
and devices connected to them. It shows a brief list of devices.

## Options

-   `-n`, `--numerical`: Don't try to resolve numerical PCI IDs. This is useful when
    there is a need to see the actual PCI IDs, or if `/res/pci.ids` file is not available.

## Files

-   `/sys/bus/pci` - source of the PCI devices list.
-   `/res/pci.ids` - a database of PCI identifiers used to match available devices to their vendor, device and class names.

## Examples

```sh
$ lspci
```
