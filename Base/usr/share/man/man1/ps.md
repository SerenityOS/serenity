## Name

ps - list currently running processes

## Synopsis

```**sh
$ ps [--version] [-a] [-A] [-e] [-f] [-p pid-list] [-q pid-list]
```

## Description

Print a list of currently running processes in the current TTY.
For each process, print its PID (process ID), to which TTY it belongs, and invoking commandline (CMD).

## Options

* `-a`: Consider all processes that are associated with a TTY.
* `-A` or `-e`: Consider all processes, not just those in the current TTY.
* `-f`: Also print for each process: UID (as resolved username), PPID (parent PID), and STATE (Runnable, Sleeping, Selecting, Reading, etc.)
* `-p pid-list`: Only consider the given PIDs, if they exist. `pid-list` is a list of PIDs, separated by commas or spaces.
* `-q pid-list`: Only consider the given PIDs, if they exist. Output the processes in the order provided by `pid-list`. `pid-list` is a list of PIDs, separated by commas or spaces.

## Examples

```sh
$ ps -ef
```
