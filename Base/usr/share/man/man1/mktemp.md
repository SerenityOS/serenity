## Name

mktemp - create a temporary file or directory

## Synopsis

```**sh
$ mktemp [--directory] [--dry-run] [--quiet] [--tmpdir DIR] [template]
```

## Description

`mktemp` creates temporary a file or directory safely, and then prints its name.

A template may be specified and will be used instead of the default tmp.XXXXXXXXXX
as long as it contains at least 3 consecutive 'X's.

## Options

-   `-d`, `--directory`: Create a temporary directory instead of a file
-   `-u`, `--dry-run`: Do not create anything, just print a unique name
-   `-q`, `--quiet`: Do not print diagnostics about file/directory creation failure
-   `-p`, `--tmpdir`: Create temporary files relative to this directory

## Examples

```sh
# Create a temporary file
$ mktemp
# Find an available temporary file name
$ mktemp -u
# Create a temporary directory with a custom template
$ mktemp -d serenity.XXXXX
```

## See also

-   [`mkdir`(1)](help://man/1/mkdir) to create a regular directory
-   [`touch`(1)](help://man/1/touch) to create a regular file
