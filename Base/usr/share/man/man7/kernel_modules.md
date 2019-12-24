## Name

Kernel Modules - runtime code loading for the kernel

## Description

Serenity's kernel supports loading modules at runtime. This functionality can
be used to implement optional features (e.g. drivers), and speed up your
development cycle.

## Module format

A kernel module is a regular ELF object file which must export several
symbols. Any symbols it refers to will be resolved when it is loaded.

### `module_name`

This should be a string like `const char module_name[]` containing the name of
the module. This is used to give the module a name in any informational
contexts, but also to ensure that the module is not loaded twice by accident,
and also used as a reference to unload the module later.

### `module_init`

This should be a function with the following signature: `void module_init()`.
It will be called when the module is loaded.

### `module_fini`

This is optional, but if defined it should be a function with the following
signature: `void module_fini()`. It will be called when the module is
unloaded.

## Example:

```c++
#include <Kernel/kstdio.h>
#include <Kernel/Process.h>

extern "C" const char module_name[] = "ExampleModule";

extern "C" void module_init()
{
    kprintf("ExampleModule has booted!\n");

    for (int i = 0; i < 3; ++i) {
        kprintf("i is now %d\n", i);
    }

    kprintf("current pid: %d\n", current->process().sys$getpid());
    kprintf("current process name: %s\n", current->process().name().characters());
}

extern "C" void module_fini()
{
    kprintf("ExampleModule is being removed!\n");
}
```

## See also

* [`modload`(1)](../man1/modload.md)
* [`modunload`(1)](../man1/modunload.md)
* [`module_load`(2)](../man2/module_load.md)
* [`module_unload`(2)](../man2/module_unload.md)
