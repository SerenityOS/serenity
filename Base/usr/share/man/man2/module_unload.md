## Name

module\_unload - unload a kernel module

## Synopsis

```**c++
#include <serenity.h>

int module_unload(const char* name, size_t name_length);
```

## Description

`module_unload()` will unload a kernel module by name.

## Return value

If the module is successfully unloaded, `module_unload()` returns 0.
Otherwise, it returns -1 and sets `errno` to describe the error.

## Errors

* `EFAULT`: `path` pointed to memory that was not accessible for the caller.
* `ENOENT`: There was no module loaded with the specified name.

## See also

* [`module_load`(2)](module_load.md)
* [`modunload`(1)](../man1/modunload.md)
* [`kernel_modules`(7)](../man7/kernel_modules.md)
