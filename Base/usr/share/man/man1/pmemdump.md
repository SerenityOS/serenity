## Name

pmemdump - dump physical memory

## Synopsis

```**sh
$ pmemdump [-r] <offset> <length>
```

## Description

Dump a portion of the physical memory space.

## Options

-   `-r`: Dump from /dev/mem with `read(2)` instead of doing `mmap(2)` on it.

## Examples

```sh
$ pmemdump -r 983040 65536
$ pmemdump 983040 65536
```

## Notes

The pmemdump utility opens the `/dev/mem` file, and gets a mapping by doing `mmap(2)`
on it.

Using the `-r` flag might be useful sometimes, especially when reading from an unaligned
reserved physical memory region when trying to `mmap(2)` `/dev/mem` on the specified
offset fails.

## See also

-   [`mem`(4)](help://man/4/mem)
