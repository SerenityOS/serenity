## Name

adjtime - print remaining system clock adjustment, and optionally set it

## Synopsis

```**sh
$ adjtime [options...]
```

## Description

`adjtime -s delta_seconds` will smoothly adjust the system time by slowing it
down (if `delta_seconds` is negative) or speeding it up (if `delta_seconds` is
positive) by a fraction of a second. The larger `delta_seconds` is, the longer
this takes. If `delta_seconds` is set and a previous time adjustment is in
progress, the remaining adjustment is canceled. That is, if `adjtime -s 1` is
called, and then `adjtime -s 1` is called again later when only 0.3s of the
first `adjtime` call have been applied yet, the clock is adjusted by 1.3
seconds total, not by 2 seconds.

`adjtime` also prints the remaining system clock adjustment.

## Options

-   `-s delta_seconds`, `--set delta_seconds`: Adjust system time by
    `delta_seconds`. Must be superuser.

## Examples

```sh
# adjtime -s 4.2
4.2
# sleep 1 && adjtime
4.1
```
