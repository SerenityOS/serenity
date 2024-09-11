## Name

mkdir - create directories

## Synopsis

```**sh
$ mkdir [options...] directories...
```

## Description

Create a new empty directory for each of the given _directories_.

## Options

-   `-p`, `--parents`: Create parent directories if they don't exist
-   `-m`, `--mode`: Sets the permissions for the final directory (possibly altered by the process umask). The mode argument can be given in any of the formats
    accepted by the chmod(1) command. Addition and removal of permissions is relative to a default permission of 0777.
-   `-v`, `--verbose`: Print a message for each created directory

## Examples

```sh
$ mkdir -p /tmp/foo/bar
$ mkdir -m 0700 /tmp/owner-only
$ mkdir -m a=rx /tmp/foo/bar
```

## See also

-   [`mkdir`(2)](help://man/2/mkdir)
