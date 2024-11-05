## Name

head - output the first part of files

## Synopsis

```**sh
$ head [option...] [file...]
```

## Description

Print the first 10 lines of each `file` to standard output. With more than one `file`,
precede each with a header giving the file name.

With no `file`, or when `file` is `-`, read standard input.

## Arguments

-   `file`: File to process

## Options

-   `-n` `--number=NUM`: Number of lines to print (default 10)
-   `-c` `--bytes=NUM`: Number of bytes to print
-   `-q` `--quiet`: Never print filenames
-   `-v` `--verbose`: Always print filenames

## Examples

```sh
# Print the first four lines from README.md and precede it with a filename header
$ head -v -n 4 README.md
==> README.md <==
# SerenityOS

Graphical Unix-like operating system for x86 computers.

```

## See also

-   [`tail`(1)](help://man/1/tail)
-   [`cat`(1)](help://man/1/cat)
