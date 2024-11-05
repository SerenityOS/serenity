## Name

![Icon](/res/icons/16x16/app-profiler.png) Profiler - Serenity process profiler

[Open](file:///bin/Profiler)

## Synopsis

```**sh
$ Profiler [--pid PID] [perfcore-file]
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

-   `-p PID`, `--pid PID`: PID to profile

## Arguments

-   `perfcore-file`: Path of perfcore file to load

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

-   [`perfcore`(5)](help://man/5/perfcore)
-   [`profile`(1)](help://man/1/profile)
