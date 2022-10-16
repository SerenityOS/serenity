## Name

config - Show or modify configuration values

## Synopsis

```sh
$ config [--remove] <domain> <group> [key] [value]
```

## Description

`config` shows and modifies values in the configuration files through ConfigServer.

## Options

* `-r`, `--remove`: Remove group or key

## Arguments

* `domain`: Config domain
* `group`: Group name
* `key`: Key name
* `value`: Value to write

## Examples

Add the Piano application to the Taskbar:
```sh
$ config Taskbar QuickLaunch Piano Piano.af
```

Set the wallpaper:
```sh
$ config WindowManager Background Wallpaper /res/wallpapers/grid.png
```
