## Name

watch - execute a program periodically

## Synopsis

```**sh
$ watch [-n interval] [-t] [-b] command
```

## Description

Run a command full-screen periodically until interrupted, then return the
aggregated error code.

## Options

-   `-n seconds`: Interval between executions, in seconds. By default, the program is run every 2 seconds.
-   `-t`, `--no-title`: Don't print the title bar.
-   `-b`, `--beep`: Beep each time the command exits with a non-zero status.
-   `-f file`, `--file file`: Run command whenever this file changes. Can be used multiple times.

## Exit Values

-   0 - Success
-   1 - At least one invocation of the command failed or exited with a non-zero status

## Examples

```sh
$ watch -n1 ls
$ watch -t -- uname -a
```
