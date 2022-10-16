## Name

lsof - List open files

## Synopsis

```**sh
$ lsof [options]... [filename]
```

## Description

`lsof` lists and filters opened files such as pipes, sockets or regular files.
If `filename` is specified, `lsof` will filter out by processes having `filename` opened.

## Options

* `-p pid`: Filter by PID
* `-d fd`: Filter by file descriptor
* `-u username/uid`: Filter by username/UID
* `-g pgid`: Filter by process group ID

## Arguments

* `filename`: Target filename. If unspecified, show all open files.

## Examples

List opened files:
```sh
$ lsof
```

List files opened by FileManager:
```sh
$ lsof -p $(pidof FileManager)
```

## See also

* [`proc`(7)](help://man/7/proc)

