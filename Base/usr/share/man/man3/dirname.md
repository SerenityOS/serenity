## Name

dirname - get a file's containing directory path

## Synopsis

```**c++
#include <libgen.h>

char* dirname(char* path);
```

## Description

Given a file path, `dirname()` returns a path to the directory that contains the
file. `dirname()` works purely lexically, meaning it only manipulates the path
as a string, and does not check if such a file or its containing directory
actually exist.

A call to `dirname()` may reuse and modify the passed in `path` buffer. Do not
expect it to have the same value after calling `dirname()`.

## Return value

`dirname()` returns the directory path as a string. This string may be allocated
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
    dbgln("{}", dirname(path1)); // should be "/home/anon"

    char path2[] = "foo/bar/";
    dbgln("{}", dirname(path2)); // should be "foo"

    char path3[] = "foo";
    dbgln("{}", dirname(path3)); // should be "."

    char path4[] = "/";
    dbgln("{}", dirname(path4)); // should be "/"
}
```

## See also

-   [`basename`(3)](help://man/3/basename)
