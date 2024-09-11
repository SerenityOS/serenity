## Name

elfdeps - list ELF object dynamic dependencies

## Synopsis

```**sh
$ elfdeps [-r] [-f] <path>
```

## Description

`elfdeps` prints all dependency libraries of an ELF object.

## Options

-   `-f`, `--force-without-valid-interpreter`: Force library resolving on ELF
    object without a valid interpreter
-   `-r`, `--max-recursion`: Max library resolving recursion
-   `-s`, `--path-only-format`: Use Path-only format printing

## Arguments

-   `path`: Path to ELF object

## Security

The `elfdeps` binary is completely safe for usage on untrusted binaries -
we only use the `LibELF` code for doing library resolving, and the actual
binary interpreter (when specified in an ELF exectuable) is never called to
decode the dependency information.

## Examples

```sh
# List all dependency libraries for libc.so
$ elfdeps -f /usr/lib/libc.so
# List all dependency libraries for /bin/id
$ elfdeps /bin/id
# List all dependency libraries for /bin/WindowServer
$ elfdeps /bin/WindowServer
```
