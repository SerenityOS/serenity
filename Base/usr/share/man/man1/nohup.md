## Name

nohup - Invoke a utility that will ignore SIGHUPs

## Synopsis

```sh
$ nohup <utility> [args...]
```

## Description

`nohup` will invoke `utility` given an optional list of `args`, where at the time of the invocation it will ignore the SIGHUP signal.

If the standard output is a tty, `utility` will append its standard output to the end of the file **nohup.out** in the current directory. If **nohup.out** fails to be created or opened for
appending, `utility`'s standard output will instead be appended to the end of the file **$HOME/nohup.out**. If this also fails, `utility` won't be invoked. **nohup.out**'s
permission bits are set to S_IRUSR | S_IWUSR when it is created.

If standard error is a tty, one out of the following will occur:

1. If the standard output is open but not a tty, `utility` will redirect its standard error to its standard output.
2. If the standard output is a tty or is closed, standard error shall be appended to the end of **nohup.out** as described above.

## Arguments

-   `utility`: Utility to be invoked
-   `args`: Arguments to pass to `utility`

## Exit Status

-   126 - `utility` was found but could not be invoked.
-   127 - Either `utility` could not be found or an error occurred in `nohup`.
