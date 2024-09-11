## Name

stat - Display file status

## Synopsis

```**sh**
$ stat [-L] file
```

## Description

Display file status and provide more information about that file.

## Options

-   `-L`: Follow links to files
-   `--help`: Display help message and exit
-   `--version`: Print version

## Examples

```sh
$ stat -L /bin/ls
    File: /bin/ls
   Inode: 8288
    Size: 184896
   Links: 1
  Blocks: 376
     UID: 0 (root)
     GID: 0 (root)
    Mode: (100755/-rwxr-xr-x)
Accessed: 2022-10-20 03:56:56
Modified: 2022-10-20 03:55:34
 Changed: 2022-10-20 03:56:57
```
