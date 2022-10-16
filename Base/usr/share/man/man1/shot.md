## Name

shot - Take a screenshot

## Synopsis

```**sh
$ shot [-c|-e] [-s index] [-r] [-d seconds] [filename]
```

## Description

`shot` asks WindowServer to take a screenshot of the entire screen, or of a
specific region if `--region` is specified. If `filename` isn't specified, it defaults to
"screenshot-%Y-%m-%d-%H-%M-%S.png" where `%Y` is replaced by the current year, `%m` by the
current month, `%d` by the current day, `%H` by the current hour, `%M` by the current minute,
and `%S` by the current second.

## Options

* `-c`, `--clipboard`: Output to clipboard
* `-d seconds`, `--delay seconds`: Seconds to wait before taking a screenshot
* `-s index`, `--screen index`: The index of the screen (default: -1 for all screens)
* `-r`, `--region`: Select a region to capture
* `-e`, `--edit`: Open in PixelPaint

## Arguments

* `filename`: Output filename

## Examples

Take a screenshot and edit it in PixelPaint:
```sh
shot -e
```

Select a region, wait three seconds, take a screenshot and output it to the clipboard:
```sh
shot -red3
```

