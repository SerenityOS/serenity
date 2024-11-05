## Name

dd - copy blocks of data

## Synopsis

```**sh
$ dd if=[input_file] of=[output_file] [args...]
```

## Description

`dd` is an application used to write blocks of data from one file to another. A common use case for `dd` is to make a bootable hard drive from a ISO file.

## Options

-   `--help`: Display help message and exit

## Arguments

-   `if`: input file (or device) to read from (default: stdin)
-   `of`: output file (or device) to write to (default: stdout)
-   `bs`: block size (of bytes) to use (default: 512)
-   `count`: number of blocks to write
-   `seek`: number of output blocks to skip (default: 0)
-   `skip`: number of input blocks to skip (default: 0)
-   `status`: level of output (default: default)
    -   `default`: error messages + final statistics
    -   `none`: just error messages
    -   `noxfer`: no final statistics

## Examples

```**sh
$ dd if=/dev/zero of=./zeros bs=1M count=1
```
