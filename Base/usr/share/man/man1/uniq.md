## Name

uniq - filter out repeated adjacent lines

## Synopsis

```**sh
$ uniq [-c] [-d|-u] [-f skip-fields] [-s skip-chars] [--version] [INPUT] [OUTPUT]
```

## Description

Filter out repeated adjacent lines from INPUT (or standard input) and write to OUTPUT (or standard output). It is recommended to sort out the input using [`sort(1)`](help://man/1/sort) beforehand.

## Options

-   `-c`, `--count`: Precede each line with its number of occurrences.
-   `-d`, `--repeated`: Only print repeated lines.
-   `-u`, `--unique`: Only print unique lines (default).
-   `-i`, `--ignore-case`: Ignore case when comparing lines.
-   `-f N`, `--skip-fields N`: Skip first N fields of each line before comparing.
-   `-s N`, `--skip-chars N`: Skip first N chars of each line before comparing.
-   `--help`: Display help message and exit.
-   `--version`: Print version.

## Examples

Filter out repeated lines from README.md and write to standard output:

```sh
$ uniq README.md
```

Filter out repeated lines from README.md and write to UNIQUE.md:

```sh
$ uniq README.md UNIQUE.md
```

Filter out repeated lines from standard input with their occurrence count:

```sh
$ echo "Well\nWell\nWell\nHello Friends!" | uniq -c
3 Well
1 Hello Friends!
```

Filter out repeated lines, ignoring the first field ("XXXX" and "ZZZZ") and the four chars after it (" ABC" and " BCA", thus comparing only the "D"s — which are equal indeed):

```sh
$ echo "XXXX ABCD\nZZZZ BCAD" | uniq -f1 -s4
ZZZZ BCAD
```
