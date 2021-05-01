## Name

tac - concatenate files and reverse line order

## Synopsis

```*sh
$ tac [files...]
```

## Description

`tac` reads specified files line by line, and prints them to standard output with the line order reversed. If no files are specified, or if `-` is input as an argument, `tac` reads from standard input.

## Examples

To print two files 'foo' and 'bar' with reversed line order:
```sh
$ cat foo bar
1 foo
2 foo
1 bar
2 bar
$ tac foo bar
2 foo
1 foo
2 bar
1 bar
```

To list filenames in reversed order.
```sh
$ ls
foo
bar
$ ls | tac
bar
foo
```

To print standard input with reversed line order.
```sh
$ tac
first
second
third
third
second
first
```
