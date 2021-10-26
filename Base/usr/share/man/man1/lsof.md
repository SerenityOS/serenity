## Name

lsof

## Synopsis

```sh
$ lsof [-p pid] [-d fd] [-u login/UID] [-g PGID] [filename]
```

## Description

List open files of a processes. This can mean actual files in the file system, sockets, pipes, etc.

## Options:

* `--help`: Display help message and exit
* `--version`: Print version
* `-p pid`: Select by PID
* `-d fd`: Select by file descriptor
* `-u login/UID`: Select by login/UID
* `-g PGID`: Select by process group ID

## Arguments:

* `filename`: Filename

<!-- Auto-generated through ArgsParser -->
