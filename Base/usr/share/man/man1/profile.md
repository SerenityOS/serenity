## Name

profile - Process or system profiler

## Synopsis

```**sh
$ profile [-p PID] [-a] [-e] [-d] [-f] [-w] [-t event_type] [COMMAND_TO_PROFILE]
```

## Description

`profile` records profiling information that can then be read with `ProfileViewer`.

## Options

-   `-p PID`: Target PID
-   `-a`: Profile all processes (super-user only), result at /sys/kernel/profile
-   `-e`: Enable
-   `-d`: Disable
-   `-f`: Free the profiling buffer for the associated process(es).
-   `-w`: Enable profiling and wait for user input to disable.
-   `-t event_type`: Enable tracking specific event type

Event type can be one of: sample, context_switch, page_fault, syscall, read, kmalloc and kfree.

## Examples

```sh
# Enable whole-system profiling
$ profile -ae
# ...then, to stop
$ profile -ad

# Profile a running process, with PID 42
$ profile -p 42

# Profile syscalls made by echo
$ profile -t syscall -- echo "Hello friends!"
```

## See also

-   [`Profiler`(1)](help://man/1/Applications/Profiler) GUI for viewing profiling data produced by `profile`.
-   [`strace`(1)](help://man/1/strace)
