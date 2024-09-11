## Name

basename - extract file name from a path

## Synopsis

```**c++
#include <libgen.h>

char* basename(char* path);
```

## Description

Given a file path, `basename()` returns that file's name. `basename()` works
purely lexically, meaning it only manipulates the path as a string, and does
not check if such a file actually exists.

A call to `basename()` may reuse and modify the passed in `path` buffer. Do not
expect it to have the same value after calling `basename()`.

## Return value

`basename()` returns the file name as a string. This string may be allocated
in static memory, or it may point to some part of the original `path` buffer.
Do not `free()` the returned string, and do not `free()` the original `path`
buffer while using the returned string.

## Examples

```c++
#include <AK/LogStream.h>
#include <libgen.h>

int main()
{
    char path1[] = "/home/anon/README.md";
    dbgln("{}", basename(path1)); // should be "README.md"

    char path2[] = "foo/bar/";
    dbgln("{}", basename(path2)); // should be "bar"

    char path3[] = "foo";
    dbgln("{}", basename(path3)); // should be "foo"

    char path4[] = "/";
    dbgln("{}", basename(path4)); // should be "/"
}
```

## See also

-   [`dirname`(3)](help://man/3/dirname)
