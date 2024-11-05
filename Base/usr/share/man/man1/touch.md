## Name

touch - create a file or change its timestamps

## Synopsis

```**sh
$ touch [-acm] [-r ref_file|-t time|-d date_time] <path...>
```

## Description

`touch` updates the last access and last modification times of all files
specified in `path` to the current time.

Unless `-c` is specified, `touch` creates a regular empty file for a `path`
that does not exist.

## Options

-   `-a`: Change access time of file
-   `-c`: Do not create a file if it does not exist
-   `-m`: Change modification time of file
-   `-r`: Use time of file specified by reference path instead of current time
-   `-t`: Use specified time in format [[CC]YY]MMDDhhmm[.SS] instead of current
    time
-   `-d`: Use specified datetime in formats YYYY-MM-DDThh:mm:SS[.frac][tz] or
    YYYY-MM-DDThh:mm:SS[,frac][tz] instead of current time

## Examples

```sh
# Create or update a file named 'file' with its last access and last
# modification attributes set to the current time:
$ touch file

# Update a file called 'somefile' with last access and last modification
# timestamps set to 14:49:30 on May 13, 2009:
$ touch -c -d '2009-05-13 14:49:30' somefile

# Create or update a file called 'anotherfile', where the resulting file has
# both last modification and last access timestamps set to April 4, 1971 at
# 09:17:00 local time:
$ touch -t 197104180917 anotherfile

# Create or update a file called 'thatfile'. It's last access time is set to
# the last access time of the file named 'anotherfile' instead of the current
# time, and the last modification time remains unchanged as long as the file
# exists:
$ touch -r anotherfile thatfile
```

## See also

-   [`mktemp`(1)](help://man/1/mktemp) to create a temporary file
-   [`futimens`(2)](help://man/3/futimens)
-   [`utimensat`(2)](help://man/3/utimensat)
