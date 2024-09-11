## Name

GML Toolbar Widget

## Description

Defines a GUI toolbar widget.

When `collapsible` is set to `true`, the toolbar can be resized below the size of its items.
Any items that do not fit the current size, will be placed in an overflow menu.
To keep groups (i.e. Buttons/items separated by Separators) together, and move them to the overflow menu as one, set the `grouped` property.

## Synopsis

`@GUI::Toolbar`

## Examples

```gml
@GUI::Toolbar {
    name: "toolbar"
    collapsible: true
    grouped: true
}
```

## Registered Properties

| Property    | Type | Possible values | Description                                                                           |
| ----------- | ---- | --------------- | ------------------------------------------------------------------------------------- |
| collapsible | bool | true or false   | If items that do not fit should be placed in an overflow menu                         |
| grouped     | bool | true or false   | If items should be moved to the overflow menu in groups, separated by Separator items |
