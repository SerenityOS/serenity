## Name

chown - change file owner / group

## Synopsis

```**sh
$ chown <user[:group]> <path...>
```

## Description

`chown` changes the owner of specified files to `user`, and owning group to `group`. If `group` is not specified, it is left unchanged.

**NOTE**: The caller must be a superuser to change user ownership. Other users can use `chown` to change the group to one of their other
group.

## Options

-   `-h`, `--no-dereference`: Don't follow symlinks
-   `-R`, `--recursive`: Change file ownership recursively
-   `-L`: Follow symlinks while recursing into directories

## Examples

```sh
# Change 'file' owner and group to 'anon':
$ chown anon:anon file

# Change 'file' owner to 'root', leave group unchanged:
# chown root file
```

## See also

-   [`chgrp`(1)](help://man/1/chgrp)
-   [`chmod`(1)](help://man/1/chmod)
