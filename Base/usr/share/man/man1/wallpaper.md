## Name

wallpaper - manage the system wallpaper.

## Synopsis

```**sh
$ wallpaper [--show-all] [--show-current] [name]
```

## Description

The `wallpaper` utility can be used to set the system wallpaper and
list available wallpapers in the `/res/wallpapers/` directory.

## Options

-   `-a`, `--show-all`: Show all wallpapers
-   `-c`, `--show-current`: Show current wallpaper
-   `-r`, `--set-random`: Set random wallpaper

## Examples

Set wallpaper to `/res/wallpapers/grid.png`:

```**sh
$ wallpaper grid.png
```

List available wallpapers:

```**sh
$ wallpaper -a
```
