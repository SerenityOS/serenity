## Name

keymap - load a keyboard layout

## Synopsis

```**sh
$ keymap [--set-keymap keymap] [--set-keymaps keymaps]
```

## Description

The `keymap` utility can be used to configure the list of selected keyboard layout and switch between them.

Layouts loaded from `/res/keymaps/*.json`.

## Examples

Get name of the currently set keymap:
```sh
$ keymap
en-us
```

Select a new list of keymaps:
```sh
$ keymap --set-keymaps en-us,ru
```

Set a keymap:
```sh
$ keymap --set-keymap ru
```
