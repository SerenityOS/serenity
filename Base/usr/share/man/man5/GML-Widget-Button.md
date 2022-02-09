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
    enabled: "false"
}
```

## Registered Properties

| Property     | Type   | Possible values | Description                  |
|--------------|--------|-----------------|------------------------------|
| button_style | enum   | Normal, Coolbar | Sets the style of the button |
| text         | string | Any string      | Sets the label text          |
| checked      | bool   | true or false   |                              |
| checkable    | bool   | true or false   |                              |
