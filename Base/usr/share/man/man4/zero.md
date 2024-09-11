## Name

zero - data sink

## Description

`/dev/zero` is a character device file which discards input.

Reading from `/dev/zero` returns '\0' bytes and exits with status 0.

## Files

-   /dev/zero

## Examples

```sh
$ head -c 8 /dev/zero | hexdump
00 00 00 00 00 00 00 00
```

## See also

-   [`null`(4)](help://man/4/null)
-   [`full`(4)](help://man/4/full)
