## Name

sort - sort lines

## Synopsis

```**sh
$ sort [INPUT]
```

## Description

Sort each lines of INPUT (or standard input). A quick sort algorithm is used.

## Options

-   `-k keydef`, `--key-field keydef`: The field to sort by
-   `-u`, `--unique`: Don't emit duplicate lines
-   `-n`, `--numeric`: Treat the key field as a number
-   `-t char`, `--sep char`: The separator to split fields by
-   `-r`, `--reverse`: Sort in reverse order
-   `-z`, `--zero-terminated`: Use `\0` as the line delimiter instead of a newline

## Examples

```sh
$ echo "Well\nHello\nFriends!" | sort
Friends!
Hello
Well
```
