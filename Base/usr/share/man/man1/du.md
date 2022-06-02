## Name

du - print disk usage

## Synopsis

```**sh
$ du [options...] [files...]
```

## Description

Without any options, `du` recursively prints the disk usage of each directory argument and its subdirectories, and of each file argument.

## Options

* `-a`, `--all`: Print the sizes of all files in the file hierarchy, not just directories
* `--apparent-size`: Print apparent sizes, rather than disk usage
* `-d N`, `--max-depth N`: Print the size of each entry only if it is `N` or fewer levels below the root of the file hierarchy
* `--exclude pattern`: Exclude entries that match the glob pattern `pattern`
* `-h`, `--human-readable`: Print the size of each entry in human-readable units (B, KiB, MiB...)
* `-s`, `--summarize`: Print only the size of each argument, not entries in the file hierarchy. Equivalent to `--max-depth 0`
* `-t size`, `--threshold size`: Do not print entries smaller than `size` if positive, or entries greater than `size` if negative
* `--time time-type`: Show timestamp of type `time-type` for each entry. Available choices are: mtime, modification (modification timestamp), ctime, status, use (change timestamp) and atime, access (access timestamp)
* `-X file`, `--exclude-from file`: Exclude entries that match any of the newline-delimited glob patterns in `file`

## Arguments

* `files`: Directories or files to print disk usage of

## Examples

```sh
~ $ du -s *
4       Desktop
4       Documents
4       Downloads
6       README.md
4       Source
4       js-tests
4       tests
4       web-tests
~ $ du -a Documents
2       Documents/emoji.txt
2       Documents/zip/archive.zip
4       Documents/zip
2       Documents/tips.txt
4       Documents
~ $ du --apparent-size -a Documents
4       Documents/emoji.txt
4       Documents/zip/archive.zip
4       Documents/zip
4       Documents/tips.txt
4       Documents
```
