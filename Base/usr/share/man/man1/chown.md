## Name

chown - change file owner / group

## Synopsis

```**sh
$ chown [user][:group] files...
```

## Description

`chown` changes the owner of specified files to `user`, and owning group to `group`. If `group` is not specified, it is left unchanged.

**NOTE**: The caller must be a superuser to change user ownership. Other users can use `chown` to change the group to one of their other
group.

## Examples

```sh
# Change 'file' owner and group to 'anon':
$ chown anon:anon file

# Change 'file' owner to 'root', leave group unchanged:
# chown root file
```

## See also

* [`chgrp`(1)](chgrp.md)
* [`chmod`(1)](chmod.md)
