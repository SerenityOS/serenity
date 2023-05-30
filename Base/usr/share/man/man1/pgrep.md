## Name

pgrep - look up processes based on name

## Synopsis

```sh
$ pgrep [--count] [-d delimiter] [--ignore-case] [--invert-match] <process-name>
```

## Options

* `-c`, `--count`: Suppress normal output and print the number of matching processes
* `-d`, `--delimiter`: Set the string used to delimit multiple pids
* `-i`, `--ignore-case`: Make matches case-insensitive
* `-v`, `--invert-match`: Select non-matching lines

## Arguments

* `process-name`: Process name to search for
