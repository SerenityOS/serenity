## Name

reboot - Reboot the machine

## Synopsis

```**sh
$ reboot
```

## Description

`reboot` instructs the kernel to reboot the machine immediately.

## Notes

The `reboot` utility opens the `/sys/kernel/power_state` node and writes the magic value "1"
to instruct the kernel to reboot the machine.
