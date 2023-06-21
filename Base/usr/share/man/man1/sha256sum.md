## Name

sha256sum - calculate and check sha256sum hash of a file

## Synopsis

```**sh
$ sha256sum [options...] [file...]
```

## Description
`sha256sum` displays or checks SHA256 checksums of files. If no file is specified, it reads from standard input.

## Options
* `-b`, `--binary`: Reads in binary mode.
* `-c`, `--check`: Calculates SHA256 sum from a file, and checks it.
* `-t`, `--text`: Reads in text mode (default option).
* `--quiet`: Does not print `OK` for a verified file.
* `--status`: Does not print anything to terminal. Instead, the status code is used.
* `-w`, `--warn`: Warns if there is a badly formatted checksum line.
