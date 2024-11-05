## Name

tail - Print the end of a file

## Synopsis

```**sh
$ tail [-f] [-n number] [file]
```

## Description

`tail` prints the specified `number` (10 by default) of lines at the end of `file`.

## Options

-   `-f`, `--follow`: Output data as it is written to the file
-   `-n [+]NUM`, `--lines [+]NUM`: output the last NUM lines, instead of the last 10; or use -n +NUM to output starting with line NUM
-   `-c [+]NUM`, `--bytes [+]NUM`: output the last NUM bytes; or use -n +NUM to output starting with byte NUM

## Arguments

-   `file`: Target file. If unspecified or `-`, defaults to the standard input.

## Examples

Print the last 10 lines of README.md:

```sh
$ tail README.md
```

Print the last 42 lines of todo.txt:

```sh
$ tail -n 42 todo.txt
```

Print the last lines as they are written to logs.log:

```sh
$ tail -f logs.log
```

Print everything but the first line of foobar.csv

```sh
$ tail -n +2 foobar.csv
```

Print the last 1337 bytes of leet.txt

```sh
$ tail -c 1337 leet.txt
```

## See also

-   [`head`(1)](help://man/1/head)
-   [`cat`(1)](help://man/1/cat)
