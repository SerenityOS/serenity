## Name

ldd - list dynamic dependencies

## Synopsis

```**sh
$ ldd [-r] [-f] <path>
```

## Description

`ldd` prints all dependency libraries of an ELF object.

## Options

* `-f`, `--force-without-valid-interpreter`: Force library resolving on ELF 
    object without a valid interpreter
* `-r`, `--max-recursion`: Max library resolving recursion

## Arguments

* `path`: Path to ELF object

## Security

In contrast to other OS implementations, the `ldd` binary is completely safe for
usage on untrusted binaries - we only use the `LibELF` code for doing library
resolving, and the actual binary interpreter (when specified) is never called to
decode the dependency information.

## Examples

```sh
# List all dependency libraries for libc.so
$ ldd -f /usr/lib/libc.so
# List all dependency libraries for /bin/id
$ ldd /bin/id
# List all dependency libraries for /bin/WindowServer
$ ldd /bin/WindowServer
```
