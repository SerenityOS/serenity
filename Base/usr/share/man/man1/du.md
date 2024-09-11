## Name

du - print disk usage

## Synopsis

```**sh
$ du [files...]
```

## Description

`du` prints disk usage data for every argument, in KiB (kibibytes).

## Options

-   `-a`, `--all`: Write counts for all files, not just directories
-   `--apparent-size`: Print apparent sizes, rather than disk usage
-   `-c`, `--total`: Print total size in the end
-   `-h` , `--human-readable`: Print human-readable sizes
-   `--si`: Print human-readable sizes in SI units
-   `-d N`, `--max-depth N`: Print the total for a directory or file only if it is N or fewer levels below the command line argument
-   `-s`, `--summarize`: Display only a total for each argument
-   `-t size`, `--threshold size`: Exclude entries smaller than size if positive, or entries greater than size if negative
-   `--time time-type`: Show time of time time-type of any file in the directory, or any of its subdirectories. Available choices: mtime, modification, ctime, status, use, atime, access
-   `--exclude pattern`: Exclude files that match pattern
-   `-x`, `--one-file-system`: Don't traverse directories on different file systems
-   `-X file, --exclude-from`: Exclude files that match any pattern in file
-   `--max-size`: Exclude files with size above a specified size
-   `--min-size`: Exclude files with size below a specified size

## Arguments

-   `files`: Files to print disk usage of

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
