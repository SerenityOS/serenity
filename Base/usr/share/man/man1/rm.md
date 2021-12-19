## Name

rm - Remove files

## Synopsis

```**sh
$ rm [options...] <path...>
```

## Description

`rm` removes files specified in `path`.

If a directory is specified in `path`, the `-r` (recursive) flag is required. Otherwise, an error occurs.

## Options

* `-r`, `--recursive`: Remove files and directories recursively
* `-f`, `--force`: Do not prompt before removing
* `-v`, `--verbose`: Display what files are removed

## Examples

```sh
# Remove README.md file
$ rm README.md

# Remove cpp-tests directory
$ rm -r cpp-tests
```
