## Name

pgrep - look up processes based on name

## Synopsis

```sh
$ pgrep [--count] [-d delimiter] [--ignore-case] [--list-name] [--newest] [--uid uid-list] [--invert-match] [--exact] <process-name>
```

## Options

* `-c`, `--count`: Suppress normal output and print the number of matching processes
* `-d`, `--delimiter`: Set the string used to delimit multiple pids
* `-i`, `--ignore-case`: Make matches case-insensitive
* `-l`, `--list-name`: List the process name in addition to its pid
* `-n`, `--newest`: Select the most recently created process only
* `-U uid-list`, `--uid uid-list`: Select only processes whose UID is in the given comma-separated list. Login name or numerical user ID may be used
* `-x`, `--exact`: Select only processes whose names match the given pattern exactly
* `-v`, `--invert-match`: Select non-matching lines

## Arguments

* `process-name`: Process name to search for
