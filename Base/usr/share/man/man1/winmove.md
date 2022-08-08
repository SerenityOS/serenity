## Name

winmove - Move and resize windows

## Synopsis

```**sh
$ winmove [-x x] [-y y] [-w width] [-h height] <id>
```

## Description

winmove moves and resizes windows identified by an `id`,
most likely found with [`winlist`(1)](help://man/1/winlist).

## Options

* `-x x`, `--absolute-x x`: X coordinate to move the window to
* `-y y`, `--absolute-y y`: Y coordinate to move the window to
* `-w width`, `--width width`: New width for the window
* `-h height`, `--height height`: New heigth for the window
* `--help`: Display help message and exit
* `--version`: Print version

## Examples

Move a specific window to left top corner:
```sh
$ winmove -x 0 -y 0 41290841
```

Find Terminal windows and make their width 300:
```sh
$ winlist | grep anon@courage | cut -d' ' -f1 | xargs -L1 winmove -w 300
```

