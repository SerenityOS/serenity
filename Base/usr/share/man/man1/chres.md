## Name

chres - change display resolution

## Synopsis

```**sh
$ chres <width> <height> [scale factor]
```

## Description

`chres` changes the display resolution to <width>x<height>@<scale factor>x.

## Options

-   `-s`, `--screen`: Screen

## Examples

```sh
# Change resolution to 1920x1080, scale 2x:
$ chres 1920 1080 2
```

## Files

-   `/tmp/portal/window` to communicate with WindowServer
