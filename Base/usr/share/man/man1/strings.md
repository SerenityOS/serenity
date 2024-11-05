## Name

strings - find printable strings in files

## Synopsis

```**sh
$ strings [--bytes NUMBER] [--print-file-name] [-o] [--radix FORMAT] [PATHS...]
```

## Description

`strings` looks for printable strings in each file specified in `PATHS` and writes them to standard output. If `PATHS` is not specified, input is read from standard input.

## Options

-   `-n NUMBER`, `--bytes NUMBER`: Specify the minimum string length (4 is default).
-   `-f`, `--print-file-name`: Print the name of the file before each string.
-   `-o`: Equivalent to specifying `-t o`.
-   `-t FORMAT`, `--radix FORMAT`: Write each string preceded by its byte offset from the start of the file in the specified `FORMAT`, where `FORMAT` matches one of the following: `d` (decimal), `o` (octal), or `x` (hexidecimal).

## Examples

Display the printable strings in /bin/strings with a minimum length of 8 characters:

```sh
$ strings -n 8 /bin/strings
```

Display the printable strings in a binary file, preceded by their byte offset in hexadecimal format:

```sh
$ strings -t x ~/Videos/test.webm
```

Display the printable strings in all .txt files in the current directory, preceded by their pathname:

```sh
$ strings -f *.txt
```
