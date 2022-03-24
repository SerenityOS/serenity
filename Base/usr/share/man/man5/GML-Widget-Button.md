## Name

## Description

Defines a GUI Button widget.

## Synopsis

`@GUI::Button`

## Examples

```gml
@GUI::Button {
    name: "normal_button"
    text: "Button"
}

@GUI::Button {
    name: "disabled_normal_button"
    text: "Disabled"
    enabled: false
}
```

## Registered Properties

| Property     | Type   | Possible values | Description                                                                                           |
| ------------ | ------ | --------------- | ----------------------------------------------------------------------------------------------------- |
| button_style | enum   | Normal, Coolbar | Sets the style of the button                                                                          |
| text         | string | Any string      | Sets the label text                                                                                   |
| checked      | bool   | true or false   | Whether the button is checked; this only applies to checkable subclasses                              |
| checkable    | bool   | true or false   | Whether the button can be checked; this only applies to checkable subclasses                          |
| exclusive    | bool   | true or false   | Whether the button's check state is exclusive to its group; this only applies to checkable subclasses |
