## Name

ps - list currently running processes

## Synopsis

```**sh
$ ps [--version] [-e] [-f] [-q pid-list]
```

## Description

Print a list of currently running processes in the current TTY.
For each process, print its PID (process ID), to which TTY it belongs, and invoking commandline (CMD).

## Options

* `-e`: Consider all processes, not just those in the current TTY.
* `-f`: Also print for each process: UID (as resolved username), PPID (parent PID), and STATE (Runnable, Sleeping, Selecting, Reading, etc.)
* `-q pid-list`: Only consider the given PIDs, if they exist.

## Examples

```sh
$ ps -ef
```
