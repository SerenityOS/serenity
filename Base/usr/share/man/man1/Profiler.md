## Name

Profiler - Serenity process profiler

## Synopsis

```**sh
$ Profiler [--pid PID] [perfcore file]
```

## Description

Profiler facilitates process performance profiling and provides a GUI offering
visual graph and tree representations to easily navigate generated profiling
information.

If no arguments are provided, a window containing a list of running processes
is presented, allowing a process to be selected for profiling.

Profiling information is written to `perfcore.<pid>` in the working directory
and opened immediately for browsing following termination of profiling.

Profiler can also load performance information from previously created
`perfcore` files.

## Options

* `-p PID`, `--pid PID`: PID to profile

## Examples

Profile running Shell process:

```sh
$ Profiler -p $(pidof Shell)
```

Open a previously created perfcore file for browsing:

```sh
$ Profiler perfcore.123
```

## See also

* [`perfcore`(5)](../man5/perfcore.md)

