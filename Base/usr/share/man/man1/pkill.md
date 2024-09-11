## Name

pkill - Signal processes based on name

## Synopsis

```sh
$ pkill [--count] [--ignore-case] [--echo] [--newest] [--oldest] [--older seconds] [--signal signame] [--uid uid-list] [--exact] <process-name>
```

## Options

-   `-c`, `--count`: Display the number of matching processes
-   `-i`, `--ignore-case`: Make matches case-insensitive
-   `-e`, `--echo`: Display what is killed
-   `-n`, `--newest`: Kill the most recently created process only
-   `-o`, `--oldest`: Select the least recently created process only
-   `-O`, `--older`: Select only processes older than the specified number of seconds
-   `-s signame`, `--signal signame`: Signal to send. The signal name or number may be used
-   `-U uid-list`, `--uid uid-list`: Select only processes whose UID is in the given comma-separated list. Login name or numerical user ID may be used
-   `-x`, `--exact`: Select only processes whose names match the given pattern exactly

## Arguments

-   `process-name`: Process name to search for

## See Also

-   [`ps`(1)](help://man/1/ps)
