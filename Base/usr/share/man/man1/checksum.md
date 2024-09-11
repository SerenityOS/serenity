## Name

checksum - helper program for calculating checksums

## Synopsis

```**sh
$ b2sum [options...] <file...>
$ md5sum [options...] <file...>
$ sha1sum [options...] <file...>
$ sha256sum [options...] <file...>
$ sha512sum [options...] <file...>
```

## Description

This program calculates and print specified checksum of files. It cannot be run directly, only
as `b2sum`, `md5sum`, `sha1sum`, `sha256sum` or `sha512sum`. A non-zero exit code is returned if the
input cannot be read. If the '--check' option is used, a non-zero exit code is also returned
if the checksums cannot be verified.

## Options

-   `-c`, `--check`: Verify checksums against `file` or stdin.
