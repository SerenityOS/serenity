## Name

pkill - Signal processes based on name

## Synopsis

```sh
$ pkill [--count] [--ignore-case] [--echo] [--signal number] [--uid uid-list] [--exact] <process-name>
```

## Options

* `-c`, `--count`: Display the number of matching processes
* `-i`, `--ignore-case`: Make matches case-insensitive
* `-e`, `--echo`: Display what is killed
* `-s number`, `--signal number`: Signal number to send
* `-U uid-list`, `--uid uid-list`: Select only processes whose UID is in the given comma-separated list. Login name or numerical user ID may be used
* `-x`, `--exact`: Select only processes whose names match the given pattern exactly

## Arguments

* `process-name`: Process name to search for
