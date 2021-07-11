## Name

ls - list directory contents

## Synopsis

```**sh
$ ls [options...] [path...]
```

## Description

`ls` lists directory contents and attributes.

If no *path* argument is provided the current working directory is used.

## Options

* `--help`: Display this message
* `-a`, `--all`: Show dotfiles
* `-A`: Do not list implied . and .. directories
* `-B`, `--ignore-backups`: Do not list implied entries ending with ~
* `-F`, `--classify`: Append a file type indicator to entries
* `-d`, `--directory`: List directories themselves, not their contents
* `-l`, `--long`: Display long info
* `-t`: Sort files by timestamp
* `-r`, `--reverse`: Reverse sort order
* `-G`: Use pretty colors
* `-i`, `--inode`: Show inode ids
* `-n`, `--numeric-uid-gid`: In long format, display numeric UID/GID
* `-o`, In long format, do not show group information
* `-h`, `--human-readable`: Print human-readable sizes
* `-K`, `--no-hyperlinks`: Disable hyperlinks
* `-R`, `--recursive`: List subdirectories recursively

## Arguments

* `path`: Directory to list

## Examples

```sh
# List contents of working directory
$ ls
# List contents of working directory including hidden dot files
$ ls -la
# List contents of working directory and its subdirectories
$ ls -R
# List contents of /etc/ directory
$ ls /etc
# List contents of /etc/ directory including hidden dot files
$ ls -la /etc
```

