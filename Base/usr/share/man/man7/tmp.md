## Name

tmp - SerenityOS TmpFS

## Description

The kernel can mount a purely RAM-backed filesystem.
This filesystem is normally mounted in /tmp and /dev.

## Filesystem-Specific Mount flags

- `fs_max_size` - Given an unsigned integer with this flag, the new TmpFS instance will be
    limited to a certain bytes size globally.
- `inode_max_size` - Given an unsigned integer with this flag, the new TmpFS instance's inodes will be
    limited to a certain bytes size individually.

## See also

* [`mount`(2))](help://man/2/mount).
