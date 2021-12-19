## Name

mkdir - create directories

## Synopsis

```**sh
$ mkdir [options...] directories...
```

## Description

Create a new empty directory for each of the given *directories*.

## Options

* `-p`, `--parents`: Create parent directories if they don't exist
* `-m`, `--mode`: Sets the permissions for the final directory (possibly altered by the process umask). The mode argument must be given in octal format.

## Examples

```sh
$ mkdir -p /tmp/foo/bar
$ mkdir -m 0700 /tmp/owner-only
```

## See also

* [`mkdir`(2)](../man2/mkdir.md)
