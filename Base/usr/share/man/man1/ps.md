## Name

ps - list currently running processes

## Synopsis

```**sh
$ ps [--version] [-a] [-A] [-e] [-f] [-o column-format] [-p pid-list] [--ppid pid-list] [-q pid-list] [-t tty-list] [-u user-list]
```

## Description

Print a list of currently running processes in the current TTY.
For each process, print its PID (process ID), to which TTY it belongs, and invoking commandline (CMD).

## Options

-   `-a`: Consider all processes that are associated with a TTY.
-   `-A` or `-e`: Consider all processes, not just those in the current TTY.
-   `-f`: Also print for each process: UID (as resolved username), PPID (parent PID), and STATE (Runnable, Sleeping, Selecting, Reading, etc.)
-   `-o column-format`: Specify a user-defined format, as a list of column format specifiers separated by commas or spaces.

    A column format specifier is of the form: `COLUMN_NAME[=COLUMN_TITLE]`.
    Where `COLUMN_NAME` is any of the following: `uid`, `pid`, `ppid`, `pgid`, `sid`, `state`, `tty`, or `cmd`.

    Specifying a `COLUMN_TITLE` will change the name shown in the column header. `COLUMN_TITLE` may be blank.
    If all given column titles are blank, the column header is omitted.

-   `-p pid-list`: Select processes matching any of the given PIDs. `pid-list` is a list of PIDs, separated by commas or spaces.
-   `--ppid pid-list`: Select processes whose PPID matches any of the given PIDs. `pid-list` is a list of PIDs, separated by commas or spaces.
-   `-q pid-list`: Only consider the given PIDs, if they exist. Output the processes in the order provided by `pid-list`. `pid-list` is a list of PIDs, separated by commas or spaces.
-   `-t tty-list`: Select processes associated with any of the given terminals. `tty-list` is a list of short TTY names (e.g: `pts:0`) or the full TTY device paths, separated by commas or spaces.
-   `-u user-list`: Select processes matching any of the given UIDs. `user-list` is a list of UIDs or login names, separated by commas or spaces.

## Examples

Show all processes (full format):

```sh
$ ps -ef
```

Show the PID, state and name of all processes

```sh
$ ps -eo pid,state,cmd
```

Show the name and state of PID 42 and rename the first column from CMD to Command:

```sh
$ ps -q 42 -o cmd=Command,state
```

Show name of PID 42 and omit the header entirely

```sh
$ ps -q 42 -o cmd=
```

## See Also

-   [`pmap`(1)](help://man/1/pmap)
-   [`lsof`(1)](help://man/1/lsof)
