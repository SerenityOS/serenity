## Name

ls - list directory contents

## Synopsis

```**sh
$ ls [options...] [path...]
```

## Description

`ls` lists directory contents and attributes.

If no _path_ argument is provided the current working directory is used.

## Options

-   `--help`: Display this message
-   `-a`, `--all`: Show dotfiles
-   `-A`: Do not list implied . and .. directories
-   `-B`, `--ignore-backups`: Do not list implied entries ending with ~
-   `-F`, `--classify`: Append a file type indicator to entries
-   `-p`: Append a '/' indicator to directories
-   `-d`, `--directory`: List directories themselves, not their contents
-   `-l`, `--long`: Display long info
-   `-t`: Sort files by timestamp (newest first)
-   `-S`: Sort files by size (largest first)
-   `-r`, `--reverse`: Reverse sort order
-   `-G`: Use pretty colors
-   `-i`, `--inode`: Show inode ids
-   `-I`, `--raw-inode`: Show raw inode ids if possible (see Notes to understand when this will not work)
-   `-n`, `--numeric-uid-gid`: In long format, display numeric UID/GID. Implies `-l`
-   `-o`: In long format, do not show group information. Implies `-l`
-   `-g`: In long format, do not show owner information. Implies `-l`
-   `-h`, `--human-readable`: Print human-readable sizes
-   `--si`: Print human-readable sizes in SI units
-   `-K`, `--no-hyperlinks`: Disable hyperlinks
-   `-R`, `--recursive`: List subdirectories recursively
-   `-1`: List one file per line

## Arguments

-   `path`: Directory to list

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

## Notes

Printing raw inode numbers is only possible when listing an entire directory.
This happens because the program uses the LibC `readdir` function, which
will provide the raw inode numbers as they're appearing "on disk".
In other cases, when strictly using the LibC `lstat` function the kernel
will resolve the inode number with respect to the mount table, so if there
is a mounted filesystem on a directory entry, `lstat` will give the root
inode number for that filesystem.

## See also

-   [`tree`(1)](help://man/1/tree) to show the contents of the directory and subdirectories in a tree visualization
