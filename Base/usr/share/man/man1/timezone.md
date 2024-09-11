## Name

timezone - View or set the system time zone

## Synopsis

```**sh
$ timezone [--list-time-zones] [time-zone]
```

## Description

The `timezone` utility can be used to view or change the current system time zone, or view all time zones available on the system.

## Options

-   `-l`, `--list-time-zones`: View all available time zones and exit.

## Arguments

-   `time-zone`: Time zone to change the system to use.

## Examples

Get the current system time zone:

```sh
$ timezone
UTC
```

Set the system time zone:

```sh
$ timezone America/New_York
```

View all available time zones:

```sh
$ timezone --list-time-zones
Africa/Abidjan
Africa/Algiers
Africa/Bissau
...
Pacific/Wake
Pacific/Wallis
PST8PDT
WET
```
