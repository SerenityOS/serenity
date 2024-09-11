## Name

pidof - list pids for a given process name

## Synopsis

```sh
$ pidof [-s] [-o pid] [-S separator] <process-name>
```

## Options

-   `-o pid`: Omit the given pid, or the parent process if the special value %PPID is passed
-   `-s`: Only return one pid
-   `-S separator`: Use `separator` to separate multiple pids

## Arguments

-   `process-name`: Process name to search for

## See also

-   [`pgrep`(1)](help://man/1/pgrep)
-   [`ps`(1)](help://man/1/ps)
