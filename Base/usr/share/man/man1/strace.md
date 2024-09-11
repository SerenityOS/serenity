## Name

strace

## Synopsis

```sh
$ strace [--pid pid] [--output output] [--exclude exclude] [--include include] [argument...]
```

## Description

Trace all syscalls and their result.

## Options

-   `-p pid`, `--pid pid`: Trace the given PID
-   `-o output`, `--output output`: Filename to write output to
-   `-e exclude`, `--exclude exclude`: Comma-delimited syscalls to exclude
-   `-i include`, `--include include`: Comma-delimited syscalls to include

## Arguments

-   `argument`: Arguments to exec

## See Also

-   [`profile`(1)](help://man/1/profile)
-   [`Profiler`(1)](help://man/1/Applications/Profiler)
