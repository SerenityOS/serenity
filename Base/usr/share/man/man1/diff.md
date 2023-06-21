## Name

diff - compare files line by line

## Synopsis

```**sh
$ diff [files...]
```

## Description

Compare `files` line by line.

## Arguments

* `files`: files to compare ex: `file1 file2`

## Examples

```sh
# View differences in two files
$ echo 123 > file1
$ echo 456 > file2
$ diff file1 file2
1c1
< 123
---
> 456
```

