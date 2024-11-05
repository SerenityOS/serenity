## Name

sysctl - configure kernel parameters at runtime

## Synopsis

```**sh
# sysctl [-a] [-w] [variable[=value]...]
```

## Description

sysctl is a utility for managing kernel configuration parameters at runtime.
This requires root privileges, and can crash your system.
Available parameters are listed under /sys/kernel/conf/.

## Options

-   `-a`: Display all kernel parameters and associated values.
-   `-w`: Set kernel parameters to the specified values.

## Arguments

-   `variable`: Retrieve the specified parameter.
-   `variable=value`: Set the specified parameter to the specified value. The option `-w` has to be specified.

## Files

-   `/sys/kernel/conf` - source of kernel parameters

## Examples

View all parameters:

```sh
# sysctl -a
```

View `ubsan_is_deadly` parameter:

```sh
# sysctl ubsan_is_deadly
ubsan_is_deadly = 1
```

Set `ubsan_is_deadly` parameter to zero (disabled):
(Note: This requires root privileges)

```sh
# su
# sysctl -w ubsan_is_deadly=0
ubsan_is_deadly: 1 -> 0
```
