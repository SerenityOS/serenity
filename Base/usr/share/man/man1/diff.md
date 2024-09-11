## Name

diff - compare files line by line

## Synopsis

```**sh
$ diff [options...] [files...]
```

## Description

Compare `files` line by line.

## Arguments

-   `files`: files to compare ex: `file1 file2`

## Options

-   `-u`, `-U <context>`, `--unified <context>`: Write diff in unified format with `<unified>` number of surrounding context lines (default 3).
-   `-c`, `-C <context>`, `--context <context>`: Write diff in context format with `<context>` number of surrounding context lines (default 3).

## Examples

First we create two files to compare:

```sh
$ printf '1\n2\n3\n' > file1
$ printf '1\nb\n3\n' > file2
```

Here's how to view differences between the two files in normal format:

```sh
$ diff file1 file2
2c2
< 2
---
> b
```

Here's how to view differences between the two files in unified format:

```sh
$ diff -u file1 file2
--- file1
+++ file2
@@ -1,3 +1,3 @@
 1
-2
+b
 3
```
