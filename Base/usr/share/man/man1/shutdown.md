## Name

shutdown - Power off the machine

## Synopsis

```**sh
$ shutdown
```

## Description

`shutdown` instructs the kernel to power off the machine immediately.

## Notes

The `shutdown` utility opens the `/sys/kernel/power_state` node and writes the magic value "2"
to instruct the kernel to power off the machine.
