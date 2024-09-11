## Name

uptime - Tell how long the system has been running

## Synopsis

```sh
$ uptime
```

## Description

`uptime` outputs information about the system, in a single line, to STDOUT.
This information includes when the system came online and how long it has been up.

## Options

-   `-p`, `--pretty`: Output only the uptime, in human-readable format.
-   `-s`, `--since`: Output only the datetime when the system came online.

## Examples

```sh
$ uptime
2024-01-24 06:23:27 up 4:20:00
```

```sh
$ uptime -p
Up 2 minutes, 20 seconds
```

```sh
$ uptime -s
2024-01-24 06:23:27
```
