## Name

blockdev - query block devices

## Synopsis

```**sh
$ blockdev [options] <device>
```

## Description

The `blockdev` command call ioctls on the given block device.

## Options

-   `-s`, `--size`: Get disk size in bytes
-   `-b`, `--block-size`: Get block size in bytes

## Examples

```sh
# Get disk size
# blockdev -s /dev/hda
863718912

# Get block size
# blockdev -b /dev/hda
512
```
