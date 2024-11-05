## Name

GML Checkbox Widget

## Description

Defines a GUI checkbox widget.

## Synopsis

`@GUI::CheckBox`

## Examples

```gml
@GUI::CheckBox {
    name: "top_checkbox"
    text: "Checkbox"
}

@GUI::CheckBox {
    name: "bottom_checkbox"
    text: "Disabled"
    enabled: false
}
```

## Registered Properties

| Property          | Type   | Possible values   | Description                                                        |
| ----------------- | ------ | ----------------- | ------------------------------------------------------------------ |
| autosize          | bool   | true or false     | Determines if auto-sized                                           |
| checkbox_position | String | "Left" or "Right" | Place the checkbox itself on either the left or right of its label |

## See also

-   [GML Button](help://man/5/GML/Widget/Button)
