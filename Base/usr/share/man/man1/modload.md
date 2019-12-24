## Name

modload - load a kernel module

## Synopsis

```**sh
$ modload <path>
```

## Description

Load a kernel module specified by *path*.

## Examples

```sh
$ modload /mod/TestModule.o
```

## See also

* [`modunload`(1)](modunload.md)
* [`module_load`(2)](../man2/module_load.md)
* [`kernel_modules`(7)](../man7/kernel_modules.md)
