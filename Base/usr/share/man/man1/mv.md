## Name

mv - move (rename) files

## Synopsis

```**sh
$ mv [options...] <old name> <new name>
$ mv [options...] <source...> <destination>
```

## Description

`mv` renames file `old name` to `new name` or moves all `source` files into `destination`.

## Options

* `-f`, `--force`: Do not prompt before overwriting (not implemented for now)
* `-v`, `--verbose`: Display all moved files

## Examples

```sh
$ mv bin /usr
$ mv *.cpp /usr/src
$ mv old.txt new.txt
```
