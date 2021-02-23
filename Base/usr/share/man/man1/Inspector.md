## Name

Inspector - Serenity process inspector

## Synopsis

```**sh
$ Inspector [pid]
```

## Description

Inspector facilitates process inspection via RPC.

The inspected process must have previously allowed the
[`accept`(2)](../man2/accept.md) system call with
[`pledge`(2)](../man2/pledge.md) to allow inspection
via UNIX socket.

## Examples

```sh
$ Inspector $(pidof Shell)
```

