## Name

module\_load - load a kernel module

## Synopsis

```**c++
#include <serenity.h>

int module_load(const char* path, size_t path_length);
```

## Description

`module_load()` will load a kernel module from an ELF object file given its
path in the filesystem.

## Return value

If the module is successfully loaded, `module_load()` returns 0. Otherwise, it
returns -1 and sets `errno` to describe the error.

## Errors

* `EPERM`: The calling process does not have superuser permissions.
* `EFAULT`: `path` pointed to memory that was not accessible for the caller.
* `ENOEXEC`: The specified file could not be parsed as an ELF object.
* `ENOENT`: One or more symbols referred to by the module could not be resolved.
* `EINVAL`: The module had no `.text` section, or didn't export a `module_init` function.
* `EEXIST`: A module with the same name was already loaded.

## See also

* [`module_unload`(2)](module_unload.md)
* [`modload`(1)](../man1/modload.md)
* [`kernel_modules`(7)](../man7/kernel_modules.md)
