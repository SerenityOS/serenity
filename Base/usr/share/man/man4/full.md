## Name

full - always full device

## Description

`/dev/full` is a character device which is always full.

Reading from `/dev/full` returns '\0' bytes and exits with status 0.

Writing to `/dev/full` fails with ENOSPC error.

To create it manually:

```sh
mknod /dev/full c 1 7
chmod 666 /dev/full
```

## Files

-   /dev/full

## Examples

```sh
$ head -c 8 /dev/full | hexdump
00 00 00 00 00 00 00 00
```

## See also

-   [`null`(4)](help://man/4/null)
-   [`zero`(4)](help://man/4/zero)
