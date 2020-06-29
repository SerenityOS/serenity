## Name

watch - execute a program periodically

## Synopsis

`watch [-n interval] [-t] [-b] command`

## Description

Run a command full-screen periodically until interrupted, then return the
aggregated error code.

## Options

* '-n': Interval between executions, in seconds. By default, the program is run every 2 seconds.
* '-t': Don't print the title bar.
* '-b': Beep each time the command exits with a non-zero status

## Exit Values

* 0 - Success
* 1 - At least one invocation of the command failed or exited with a non-zero status

## Examples

```sh
$ watch -n1 ls
$ watch -t -- uname -a
```
