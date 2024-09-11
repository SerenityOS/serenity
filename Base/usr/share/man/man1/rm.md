## Name

rm - Remove files

## Synopsis

```**sh
$ rm [options...] <path...>
```

## Description

`rm` removes files specified in `path`.

If a directory is specified in `path`, the `-r` (recursive) flag is required. Otherwise, an error occurs.

## Options

-   `-r`, `--recursive`: Remove files and directories recursively
-   `-f`, `--force`: Do not prompt before removing
-   `-v`, `--verbose`: Display what files are removed
-   `--no-preserve-root`: Do not treat '/' specially

## Examples

```sh
# Remove README.md file
$ rm README.md

# Remove Tests directory
$ rm -r Tests
```

## See also

-   [`rmdir`(1)](help://man/1/rmdir) to just delete empty directories
