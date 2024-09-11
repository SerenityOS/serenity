## Name

cmp - compare files

## Synopsis

```**sh
$ cmp [options...] file1 file2
```

## Description

Compare two files and report the location of any differences. By default, execution stops after the first difference, but this can be overridden (see `--verbose` option). Byte and line numbers start at 1.

## Options

-   `--help`: Display help message and exit
-   `-l`, `--verbose`: Output the byte number, and the differing bytes, for every difference.
-   `-s`, `--silent`: Silence output.

## Arguments

-   `file1` and `file2`: Files to compare. Use `-` as the file name to read from standard input.

## Exit status

-   0 - Files are identical.
-   1 - Files are different.
-   2 - An error occurred.

## See also

-   [`comm`(1)](help://man/1/comm)
