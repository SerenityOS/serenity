## Name

date - print or set the system date and time

## Synopsis

```**sh
$ date [--set date] [--unix] [--iso-8601] [--rfc-3339] [--rfc-5322]
```

## Description

date is a utility to set the system date and time
or print the system date and time in various formats.

## Options

* `-s`, `--set`: Set system date and time
* `-u`, `--unix`: Print date as Unix timestamp
* `-i`, `--iso-8601`: Print date in ISO 8601 format
* `-r`, `--rfc-3339`: Print date in RFC 3339 format
* `-R`, `--rfc-5322`: Print date in RFC 5322 format

## Examples

```sh
# Print the current date and time in ISO 8601 format
$ date --iso-8601

# Set date to 1610017485 (UNIX time)
$ date -s 1610017485
```
