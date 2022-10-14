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

## Options

* `-A`: Test that all of the following crash types crash as expected.
* `-s`: Perform a segmentation violation by dereferencing an invalid pointer.
* `-d`: Perform a division by zero.
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
* `-X`: Attempt to execute non-executable memory (Not mapped with PROT\_EXEC).
* `-U`: Attempt to trigger an x86 User Mode Instruction Prevention fault.
* `-I`: Use an x86 I/O instruction in userspace.
* `-c`: Read the x86 TSC (Time Stamp Counter) directly.
* `-p`: Violate `pledge()`'d promises.
* `-n`: Perform a failing assertion.

## Examples

```sh
$ crash -F
Testing: "Write to freed memory"
Shell: Job 1 (crash -F) Segmentation violation
```
