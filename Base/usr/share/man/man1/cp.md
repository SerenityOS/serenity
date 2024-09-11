## Name

cp - copy files

## Synopsis

```**sh
$ cp [options...] <source...> <destination>
```

## Description

`cp` copies files specified in `source` to `destination`.

If there are several `sources`, the directory `destination` is created and all files specified in `sources` are copied into that directory. Otherwise, the `source` file is copied as `destination` file. If the file exists, it is overridden. If `destination` directory exists and there is only one `source`, the `source` file is copied into the `destination` directory.

If a directory is specified in `source`, the `-R` (recursive) flag is required. Otherwise, an error occurs.

## Options

-   `-l`, `--link`: Create hard links instead of copying
-   `-R`, `-r`, `--recursive`: Copy directories recursively
-   `-v`, `--verbose`: Display what files are copied

## Examples

```sh
# Copy test file and name it test-backup
$ cp test test-backup

# Copy tests directory and name it tests-backup
$ cp -R tests tests-backup

# Copy test file into existing root
$ cp test root
```

## See also

-   [`mv`(1)](help://man/1/mv)
