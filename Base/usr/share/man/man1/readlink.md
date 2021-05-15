## Name

readlink - get symlink target

## Synopsis

```**sh
$ readlink [--no-newline] <path...>
```

## Description

Print targets of the specified symbolic links.

## Options

* `-n`, `--no-newline`: Do not output a newline after each target

## Examples

```sh
$ readlink /proc/self/cwd
```

## See also

* [`readlink`(2)](../man2/readlink.md)
