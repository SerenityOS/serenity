## Name

json - pretty-print a JSON file with syntax-coloring and indentation

## Synopsis

```**sh
$ json [options...] [path...]
```

## Description

`json` pretty-prints a JSON file with syntax-coloring and indentation.

If no *path* argument is provided stdin is used.

## Options

* `--help`: Display this message
* `-i`, `--indent-size`: Size of indentations in spaces
* `-q`, `--query`: Dotted query key

## Arguments

* `path`: file to pretty-print

## Examples

```sh
# Pretty-print stdin
$ cat /proc/all | json
# Pretty-print a file
$ json json-data.json
# Pretty-print a file with two spaces per indent
$ json -i 2 json-data.json
# Query data from JSON
$ json -q 1 .config/CommonLocations.json
$ cat /proc/all | json -q processes
```
