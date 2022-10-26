## Name

ntpquery - Query the time from a NTP server

## Synopsis

```**sh
$ ntpquery [-v] [-a|-s] [host]
```

## Description

`ntpquery` contacts a NTP server (by default time.google.com), and optionally sets or
adjusts (i.e. slow down or speed up) the system time, as long as the command is run as root.

## Options

* `-a`, `--adjust`: Gradually adjust system time (requires root)
* `-s`, `--set`: Immediately set system time (requires root)
* `-v`, `--verbose`: Verbose output

## Arguments

* `host`: NTP server. Defaults to time.google.com

## See also

* [`adjtime`(1)](help://man/1/adjtime)
