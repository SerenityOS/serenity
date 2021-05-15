## Name

pape - manage the system wallpaper.

## Synopsis

```**sh
$ pape [--show-all] [--show-current] [name]
```

## Description

The `pape` utility can be used to set the system wallpaper and
list available wallpapers in the `/res/wallpapers/` directory.

## Options

* `-a`, `--show-all`: Show all wallpapers
* `-c`, `--show-current`: Show current wallpaper

## Examples

Set wallpaper to `/res/wallpapers/grid.png`:

```**sh
$ pape grid.png
```

List available wallpapers:

```**sh
$ pape -a
```
