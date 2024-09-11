## Name

uname - print kernel information

## Synopsis

```**sh
$ uname [-s] [-n] [-r] [-m] [-a]`
```

## Description

Print information about the operating system, as reported by the `uname()`
system call.

## Options

-   `-s`: Print the system name
-   `-n`: Print the node name (hostname)
-   `-r`: Print the system release version
-   `-v`: Print the version of the release
-   `-m`: Print the machine type
-   `-a`: Print all of the above

## Examples

```sh
$ uname -sm
Serenity x86_64
```

## See also

-   [`uname`(2)](help://man/2/uname)
