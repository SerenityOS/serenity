## Name

![Icon](/res/icons/16x16/app-inspector.png) Inspector - Serenity process inspector

[Open](file:///bin/Inspector)

## Synopsis

```**sh
$ Inspector [pid]
```

## Arguments

* `pid`: Process ID to inspect

## Description

Inspector facilitates process inspection via RPC.

To inspect a process, it must have `MAKE_INSPECTABLE=1` in its environment,
and it must have previously allowed the
[`accept`(2)](help://man/2/accept) system call with
[`pledge`(2)](help://man/2/pledge) to allow inspection
via UNIX socket.

## Examples

```sh
$ Inspector $(pidof Shell)
```
