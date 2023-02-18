## Name

image2bin - convert an image to a binary bitmap

## Synopsis

```**sh
$ image2bin <path-to-image>
```

## Description

`image2bin` uses LibGfx to decode a specified image to a raw bitmap, so it could be stored
in a raw binary format for further examination.

## Examples

```sh
# Convert a PNG image to raw bitmap
$ image2bin example.png > example.bin
# Convert a JPEG image to raw bitmap
$ image2bin another_example.jpg > another_example.bin
```
