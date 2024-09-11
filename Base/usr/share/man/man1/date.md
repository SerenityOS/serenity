## Name

date - print or set the system date and time

## Synopsis

```**sh
$ date [--set date] [--unix] [--iso-8601] [--rfc-3339] [--rfc-5322] [format-string]
```

## Description

date is a utility to set the system date and time
or print the system date and time in various formats.

## Options

-   `-s`, `--set`: Set system date and time
-   `-u`, `--unix`: Print date as Unix timestamp
-   `-i`, `--iso-8601`: Print date in ISO 8601 format
-   `-r`, `--rfc-3339`: Print date in RFC 3339 format
-   `-R`, `--rfc-5322`: Print date in RFC 5322 format

## Arguments

-   `format-string`: Custom format to print the date in. Must start with a '+' character.

## Examples

```sh
# Print the current date and time in ISO 8601 format
$ date --iso-8601

# Print the current date in a custom format
$ date +%Y-%m-%d

# Set date to 1610017485 (UNIX time)
$ date -s 1610017485
```

## See Also

-   [`ntpquery`(1)](help://man/1/ntpquery)
