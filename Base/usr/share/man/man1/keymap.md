## Name

keymap - load a keyboard layout

## Synopsis

```**sh
# keymap <name|file>
```

## Description

The `keymap` utility can be used to set a keyboard layout using the given name or file.

Loading by name will search for keyboard layout files in `/res/keymaps/*.json`.

## Examples

Load a keyboard layout by name:
```sh
# keymap en
```

Load a keyboard layout using a file:
```sh
# keymap /res/keymaps/en.json
# keymap ./map.json
```
