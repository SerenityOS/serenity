## Name

GML Color Input Widget

## Description

Defines a GUI color input widget.

## Synopsis

`@GUI::ColorInput`

## Examples

```gml
@GUI::ColorInput {
    name: "font_colorinput"
    placeholder: "Color dialog"
}

@GUI::ColorInput {
    placeholder: "Disabled"
    enabled: false
}
```

## Registered Properties

| Property           | Type   | Possible values | Description                                             |
| ------------------ | ------ | --------------- | ------------------------------------------------------- |
| color_picker_title | string | Any string      | Sets the title of the input                             |
| has_alpha_channel  | bool   | true or false   | Whether to include the alpha channel in color selection |
