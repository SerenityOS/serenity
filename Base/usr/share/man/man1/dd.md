## Name

dd - copy blocks of data

## Synopsis

```**sh
$ dd if=[input_file] of=[output_file] [args...]
```

## Description

`dd` is an application used to write blocks of data from one file to another. A common use case for `dd` is to make a bootable hard drive from a ISO file.

## Options

* `--help`: Display help message and exit

## Arguments

* `if`: input file (or device) to read from
* `of`: output file (or device) to write to
* `bs`: block size (of bytes) to use
* `count`: number of blocks to write

## Examples

```**sh
$ dd if=/dev/zero of=./zeros bs=1M count=1
```
