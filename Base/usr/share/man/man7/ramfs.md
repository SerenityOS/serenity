## Name

ramfs - SerenityOS RAMFS

## Description

The kernel can mount a purely RAM-backed filesystem.
This filesystem is normally mounted in /tmp and /dev.

## Filesystem-Specific Mount flags

- `max_size` - Given an unsigned integer with this flag, the new RAMFS instance will be
    limited to a certain bytes size globally. This value must be aligned to the PAGE_SIZE value
    of the platform machine in use.

## See also

* [`mount`(2))](help://man/2/mount).
