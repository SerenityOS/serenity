## Name

lsdir - list directory entries

## Synopsis

```**sh
# lsdir [options...] [path...]
```

## Description

This utility will list all directory entries of a given path (or list of paths)
and print their inode number and file type (in either POSIX DT\_\* format or human readable).

The utility uses `LibCore` `DirIterator` object and restrict its functionality
to the `get_dir_entries` syscall only, to get the raw values of each directory
entry.

## Options

-   `-P`, `--posix-names`: Show POSIX names for file types
-   `-t`, `--total-entries-count`: Print count of listed entries when traversing a directory

## Arguments

-   `path`: Directory to list

## Examples

```sh
# List directory entries of working directory
$ lsdir
# List directory entries of /proc directory
$ lsdir /proc
# List directory entries of /proc directory with POSIX names for file types
$ lsdir -P /proc
# List directory entries of /proc directory and print in the end the count of traversed entries
$ lsdir -t /proc
```

## See also

-   [`ls`(1)](help://man/1/ls)
