## Name

uname - print kernel information

## Synopsis

`uname [-s] [-n] [-r] [-m] [-a]`

## Description

Print information about the operating system, as reported by the `uname()`
system call.

## Options

* `-s`: Print the system name
* `-n`: Print the node name (hostname)
* `-r`: Print the system release version
* `-m`: Print the machine type
* `-a`: Print all of the above

## Examples

```sh
$ uname -sm
Serenity i686
```

## See also

* [`uname`(2)](../man2/uname.md)
