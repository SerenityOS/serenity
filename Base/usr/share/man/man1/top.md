## Name

top - Display information about processes

## Synopsis

```
$ top [--complete] [--delay-time] [--sort-by]
```

## Description

`top` displays information about processes.

## Options

* `--help`: Display help message and exit
* `--version`: Print version
* `--complete`: Perform autocompletion
* `-d`, `--delay-time`: Delay time interval in seconds
* `-s`, `--sort-by`: Sort by field [pid, tid, pri, user, state, virt, phys, cpu, name]. By default, processes are sorted by cpu utilization

## Examples

5 seconds delay time interval :

```
$ top -d 5
```

Display information about processes sorted by PID with delay time interval of 3 seconds:

```
$ top -d 3 -s pid
```
