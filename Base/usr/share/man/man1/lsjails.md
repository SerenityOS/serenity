## Name

lsjails - list existing jails

## Synopsis

```**sh
# lsjails
```

## Description

This utility will list all existing jails at the moment of invoking this program.
Please note that if the current process is in jail, it will not see any jail.

## Examples

```sh
# lsjails
Index   Name
2       test-jail
```

## See also
* [`jail-create`(1)](help://man/1/jail-create)
* [`jail-attach`(1)](help://man/1/jail-attach)
