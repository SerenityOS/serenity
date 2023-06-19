## Name

rmdir - remove empty directories

## Synopsis

```**sh
$ rmdir `[directory...]` 
```

## Description

Remove given `directory(ies)`, if they are empty

## Options

* `-p`, `--parents`:  Remove all directories in each given path

## Arguments

* `directory`: directory(ies) to remove

## Examples

```sh
# Try to remove non-empty directory
$ mkdir serenity ; echo cool > serenity/cool.txt
$ rmdir serenity
rmdir: Directory not empty

# Remove empty directory
$ mkdir example
$ ls -a example
.	..
$ rmdir example
$ ls -a example
example: No such file or directory

# Removes foo/bar/baz/, foo/bar/ and foo/
$ rmdir -p foo/bar/baz/
```
