## Name

mem - physical system memory

## Description

`/dev/mem` is a character device file that is used by other programs to examine
the physical memory.

Trying to [`mmap`(2)](help://man/2/mmap) a physical range results either with success,
or with an error. When invoking [`mmap`(2)](help://man/2/mmap) on bad memory range,
the kernel will write a message about it to the kernel log.

By default, the kernel limits the areas which can be accessed. The allowed areas
are the reserved ranges in physical memory, essentially limiting the access to
ROMs and memory-mapped PCI regions on x86.

To create it manually:

```sh
mknod /dev/mem c 1 1
chmod 660 /dev/mem
```

## Returned error values after [`mmap`(2)](help://man/2/mmap)

-   `EINVAL`: An access violation was detected.
-   `ENOMEM`: The requested range would wrap around, creating an access violation.

## See also

-   [`mmap`(2)](help://man/2/mmap)
