## Name

jp - pretty-print a JSON file with syntax-coloring and indentation

## Synopsis

```**sh
$ jp [options...] [path...]
```

## Description

`jp` pretty-prints a JSON file with syntax-coloring and indentation.

If no *path* argument is provided stdin is used.

## Options

* `--help`: Display this message
* `-i`, `--indent-size`: Size of indentations in spaces

## Arguments

* `path`: file to pretty-print

## Examples

```sh
# Pretty-print stdin
$ cat /proc/all | jp
# Pretty-print a file
$ jp json-data.json
# Pretty-print a file with two spaces per indent
$ jp -i 2 json-data.json
```
