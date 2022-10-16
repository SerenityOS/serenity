## Name

utmpupdate - Update the utmp file

## Synopsis

```sh
$ utmpupdate [--create] [--delete] [--PID pid] [--from application] <tty>
```

## Description

`utmpupdate` manages the [`/var/run/utmp`](help://man/4/utmp) file.
It is executed by [`Terminal`(1)](help://man/1/Terminal).

## Options

* `-c`, `--create`: Create an entry
* `-d`, `--delete`: Delete an entry
* `-p pid`, `--PID pid`: The process ID on which the user is logged-in.
* `-f application`, `--from application`: The parent application on which the user is logged-in.

## Arguments

* `tty`: TTY file (e.g. /dev/pts/2, /dev/tty3)

## Files

* /var/run/utmp

## See also

* [`utmp`(4)](help://man/4/utmp)
