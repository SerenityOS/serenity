## Name

comm - compare two sorted files line by line

## Synopsis

```**sh
$ comm [options...] <file1> <file2>
```

## Description

`comm` compares two **sorted** files specified by `file1` and `file2` line by line alphabetically. One of file1 and file2, but not both, can be `-`, in which case `comm` will read from the standard input for that file.

With no options, `comm` produces a three column output, indented by tabs, of lines unique to `file1`, lines unique to `file2`, and lines common to both files. `comm` provides options to suppress the output of a specific column, use case insensitive comparison or print a summary.

## Options

-   `-1`: Suppress the output of column 1 (lines unique to `file1`)
-   `-2`: Suppress the output of column 2 (lines unique to `file2`)
-   `-3`: Suppress the output of column 3 (lines common to `file1` and `file2`)
-   `-i`: Use case insensitive comparison of lines
-   `-c`, `--color`: Always print colored output even if the standard output is not a tty
-   `--no-color`: Do not print colored output
-   `-t`, `--total`: Print a summary

## Arguments

-   `file1`: First file to compare. (`-` for the standard input)
-   `file2`: Second file to compare. (`-` for the standard input)

## Examples

```sh
# Files should be sorted first
$ sort < file1 > file1_sorted
$ sort < file2 > file2_sorted

# Display the default three-column output
$ comm file1_sorted file2_sorted

# Read one sorted file from the standard input
# and only display column 3
$ sort < file1 | comm -12c - file2_sorted | less

# Use case insensitive comparison,
# suppress output of all columns
# and print a summary
$ comm -123it file1_sorted file2_sorted
```

## See also

-   [`cmp`(1)](help://man/1/cmp)
