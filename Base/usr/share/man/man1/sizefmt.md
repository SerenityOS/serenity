## Name

sizefmt - Print the size of a number with a suffix, in bytes

## Synopsis

```**sh
$ sizefmt [integer-with-suffix]
```

## Description

`sizefmt` prints the 'real' size of a number with a suffix (possibly).
It can parse just a single character suffix (k for kilo, for example), or base 2 or 10
suffixes (KB for Kilobytes, or KiB for Kibibytes, for example).

## Arguments

-   `integer-with-suffix`: a number with a suffix (possibly).

## Examples

```sh
# prints 10000000 for 10 million bytes
$ sizefmt 10MB

# truncate a file /tmp/test_file with size of 10 Kibibytes
$ truncate -s $(sizefmt 10KiB) /tmp/test_file

# truncate a file /tmp/test_file2 with size of 10 Megabytes
$ truncate -s $(sizefmt 10MB) /tmp/test_file

# truncate a file /tmp/test_file3 with size of 2 Kibibytes
$ truncate -s $(sizefmt 2KiB) /tmp/test_file3
```
