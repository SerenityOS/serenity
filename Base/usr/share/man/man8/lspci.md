## Name

lspci - list connected PCI devices

## Synopsis

```**sh
$ lspci
```

## Description

lspci is a utility for displaying information about PCI buses in the system
and devices connected to them. It shows a brief list of devices.

## Files

* `/proc/pci` - source of the PCI devices list.
* `/res/pci.ids` - a database of PCI identifiers used to match available devices to their vendor, device and class names.

## Examples

```sh
$ lspci
```
