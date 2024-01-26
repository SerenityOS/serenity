## Name

semver - utility to compare 

## Synopsis

```**sh
$ semver [--satisfies SPEC] [--separator SEPARATOR] [--bump BUMP_TYPE] <versions...>
```

## Description

Validate, compare, bump, or check if the versions satisfies the spec or not.

## Options

* `--help`: Display help message and exit
* `--version`: Display version
* `--satisfies`: Spec string to filter all the versions that satisfies it
* `-s`, `--separator`: Normal version part separator (default: `.`)
* `-b`, `--bump`: Part of the version to bump. You must choose from `major`, `minor`, `patch`, or `prerelease`

## Arguments

* `versions`: raw version strings to process

## Examples

```sh
# Try 
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

## See also
* [`mkdir`(1)](help://man/1/mkdir)
* [`rm`(1)](help://man/1/rm)
