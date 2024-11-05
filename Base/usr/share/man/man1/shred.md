## Name

shred - destroy a file with its content on disk

## Synopsis

```**sh
$ shred [options...] [path...]
```

## Description

`shred` destroys files' content on disk.

## Options

-   `--help`: Display this message
-   `-v`, `--verbose`: Show progress during the shred operation
-   `-n`, `--iterations`: Iterations count of shred operation
-   `-u`: Deallocate and remove file after overwriting
-   `--random-source`: Get random bytes from a file other than /dev/random

## Arguments

-   `path`: Files to shred

## Examples

```sh
# shred a file and remove it aftewards
$ shred -u /tmp/FILE_TO_BE_SHREDDED_AND_REMOVED
# shred a file with 10 iterations
$ shred --iterations 10 /tmp/FILE_TO_BE_SHREDDED
# shred a file with verbose progress and 10 iterations
$ shred -v --iterations 10 /tmp/FILE_TO_BE_SHREDDED
```

## See also

-   [`rm`(1)](help://man/1/rm) to delete a file or directory without overwriting the content first
