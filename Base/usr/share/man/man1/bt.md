## Name

bt - view the backtrace of the specified process

## Synopsis

```**sh
$ bt <pid>
```

## Description

This program is used to inspect the current executable state of a process.
It will read the stack of each thread in the process, and symbolicate the
addresses for each frame in the stack producing a backtrace.

**NOTE**:

* Kernel addresses will not be available unless you are super user.

* If Kernel addresses are available, they will not be symbolicated unless
  the current user has access to the `/boot/Kernel` file.

## Examples

View all stacks of pid number 10:

```sh
$ bt 10
```

Use [`watch`(1)](watch.md) to emit a backtrace of pid 124, every second:

```sh
$ watch -n 1 -- bt 124
```

## See also

* [`Inspector`(1)](Inspector.md)

* [`Profiler`(1)](Profiler.md)

* [`watch`(1)](watch.md)
