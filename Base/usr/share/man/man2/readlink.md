## Name

readlink - get symlink target

## Synopsis

```**c++
#include <unistd.h>

ssize_t readlink(const char* path, char* buffer, size_t size)
```

## Description

`readlink()` writes up to `size` bytes of the target path of a symbolic link at
the specified `path` to the given `buffer`. `readlink()` does not
null-terminate the buffer. If the target of the link is longer than `size`
bytes, it will get truncated.

## Return value

On success, `readlink()` returns the number of bytes written to the buffer,
which is always less or equal to the specified `size`. Otherwise, it returns -1
and sets `errno` to describe the error.

## Notes

The underlying system call always returns the full size of the target path on
success, not the number of bytes written. `Core::File::read_link()` makes use
of this to provide an alternative way to read links that doesn't require the
caller to pick a buffer size and allocate a buffer straight up.

Since it's essentially impossible to guess the right buffer size for reading
links, it's strongly recommended that everything uses `Core::File::read_link()`
instead.

## Examples

The following example demonstrates how one could implement an alternative
version of `getpid(2)` which reads the calling process ID from ProcFS:

```c++
#include <LibCore/File.h>
#include <unistd.h>

pid_t read_pid_using_readlink()
{
    char buffer[64];
    int rc = readlink("/proc/self", buffer, sizeof(buffer) - 1);
    if (rc < 0)
        return rc;
    buffer[rc] = 0;
    return atoi(buffer);
}

pid_t read_pid_using_core_file()
{
    auto target = Core::File::read_link("/proc/self");
    if (target.is_null())
        return -1;
    auto pid = target.to_uint();
    ASSERT(pid.has_value());
    return pid.value();
}
```

## See also

* [`readlink`(1)](../man1/readlink.md)
