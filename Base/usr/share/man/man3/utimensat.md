## Name

futimens, utimensat - update file access and modification times

## Synopsis

```**c++
#include <sys/stat.h>

int futimens(int fd, struct timespec const times[2]);

#include <fcntl.h>

int utimensat(int dirfd, char const* path, struct timespec const times[2],
    int flag);
```

## Description

`futimens()` and `utimensat()` set the access and modification times of a file
to the values specified in `times`.

`futimens()` updates the times of the file associated with the file descriptor.

`utimensat()` functions in two ways.

1. Given a valid file descriptor for a directory and a non-empty path,
   `utimensat()` updates the value of the file specified by the path relative to
   the directory specified by the file descriptor. This is standard POSIX
   behavior.
2. Given a valid file descriptor to a regular file and an empty path,
   `utimensat()` updates the value of the file associated with the file
   descriptor. This is not standard POSIX behavior, but it allows `futimens()` to
   be implemented in terms of `utimensat()`.

If the `tv_nsec` field of `times` is set to UTIME_NOW, then the corresponding
timestamp of the file is set to the current time. If the `tv_nsec` field of
`times` is set to UTIME_OMIT, then the corresponding timestamp is not modified.
In both of these special cases, `tv_sec`, the other field in `times`, is
ignored.

If `times` is a null pointer, then both the last access time and the last
modification time are set to the current time. This configuration is equivalent
to setting the `tv_nsec` field to UTIME_NOW for both timespec structures in the
array.

Parameter `flag` of `utimensat()` may be set to 0 or `AT_SYMLINK_NOFOLLOW`. If
set to `AT_SYMLINK_NOFOLLOW`, instead of following a symbolic link,
`utimensat()` updates the timestamps of the link itself. `futimens()` always
follows symbolic links.

Parameter `dirfd` of `utimensat()` may be set to `AT_FDCWD` to use the current
working directory as the relative directory.

## Return Value

`futimens()` and `utimensat()` return 0 upon success and -1 otherwise. Upon
failure, these functions also set `errno` to indicate the error and leave the
access and modification times of the specified file unmodified.

## Errors

`futimens()` and `utimensat()` may return the following error codes.

-   `EFAULT`: `path` of `utimensat()` is a null pointer.
-   `EINVAL`: Length of `path` is too long.
-   `EINVAL`: `flag` is not 0 or `AT_SYMLINK_NOFOLLOW`
-   `EINVAL`: The timestamp is not supported by the file system.
-   `EINVAL`: Fields of `times` are less than 0 or greater than or equal to 1000
    million and not `UTIME_NOW` or `UTIME_OMIT`.
-   `EACCES`: The current user does not have write access to the file.
-   `EROFS`: The file system that contains the file is read-only.
-   `ENOTDIR`: `path` is not absolute and `dirfd` is not a file descriptor
    associated with a directory.

## Examples

```c++
#include <fcntl.h>
#include <sys/stat.h>

int main()
{
    timespec times[2];
    auto& atime = times[0];
    auto& mtime = times[1];

    atime.tv_sec = 0;
    atime.tv_nsec = UTIME_NOW;
    mtime.tv_sec = 0;
    mtime.tv_nsec = UTIME_OMIT;

    // Update only last access time of file "README.md" in current working
    // directory to current time. Leave last modification time unchanged.
    if (utimensat(AT_FDCWD, "README.md", times, 0) == -1) {
        return 1;
    }

    return 0;
}
```

## See also

-   [`touch`(1)](help://man/1/touch)
