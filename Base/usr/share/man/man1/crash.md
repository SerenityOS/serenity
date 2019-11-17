## Name

crash - intentionally perform an illegal operation

## Synopsis

```**sh
$ crash [options]
```

## Description

This program is used to test how the Serenity kernel handles
userspace crashes, and can be used to simulate many different
kinds of crashes.

## Options

* `-s`: Perform a segmentation violation by dereferencing an invalid pointer.
* `-d`: Perform a division by zero.
* `-i`: Execute an illegal CPU instruction.
* `-a`: Call `abort()`.
* `-m`: Read a pointer from uninitialized memory, then read from it.
* `-f`: Read a pointer from memory freed using `free()`, then read from it.
* `-M`: Read a pointer from uninitialized memory, then write to it.
* `-F`: Read a pointer from memory freed using `free()`, then write to it.
* `-r`: Write to read-only memory.
* `-T`: Make a syscall while using an invalid stack pointer.
* `-t`: Trigger a page fault while using an invalid stack pointer.

## Examples

```sh
$ crash -F
Shell: crash(33) exitied due to signal "Segmentation violation"
```
