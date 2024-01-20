## Name

crash - intentionally perform an illegal operation

## Synopsis

```**sh
$ /usr/Tests/Kernel/crash [options]
```

## Description

This program is used to test how the Serenity kernel or UserspaceEmulator
handle userspace crashes, and can be used to simulate many different kinds
of crashes.

Some crash tests are only available on certain architectures.

## Options

All architectures:

* `-A`: Test that all of the crash types implemented on this architecture crash as expected.
* `-s`: Perform a segmentation violation by dereferencing an invalid pointer.
* `-i`: Execute an illegal CPU instruction.
* `-a`: Call `abort()`.
* `-m`: Read a pointer from uninitialized malloc memory, then read from it.
* `-f`: Read a pointer from memory freed using `free()`, then read from it.
* `-M`: Read a pointer from uninitialized malloc memory, then write to it.
* `-F`: Read a pointer from memory freed using `free()`, then write to it.
* `-r`: Write to read-only memory.
* `-T`: Make a syscall while using an invalid stack pointer.
* `-t`: Trigger a page fault while using an invalid stack pointer.
* `-S`: Make a syscall from writeable memory.
* `-y`: Make a syscall from legitimate memory (but outside syscall-code mapped region).
* `-X`: Attempt to execute non-executable memory (Not mapped with PROT\_EXEC).
* `-U`: Attempt to use a priviledged (kernel mode or higher) instruction in user mode.
* `-p`: Violate `pledge()`'d promises.
* `-n`: Perform a failing assertion.
* `-R`: Dereference a null RefPtr.

x86_64 only:

* `-I`: Use an x86 I/O instruction in userspace.

AArch64 and x86_64 only:

* `-d`: Perform a division by zero.

## Examples

```sh
$ crash -F
Testing: "Write to freed memory"
Shell: Job 1 (crash -F) Segmentation violation
```
