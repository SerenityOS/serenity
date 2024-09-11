## Name

GML Combo Box Widget

## Description

Defines a GUI combo box widget. Another name for this would be a dropdown or select.

## Synopsis

`@GUI::ComboBox`

## Examples

```gml
@GUI::ComboBox {
    name: "msgbox_icon_combobox"
    model_only: true
}

@GUI::ComboBox {
    name: "msgbox_buttons_combobox"
    model_only: true
}
```

## Registered Properties

| Property    | Type   | Possible values | Description                  |
| ----------- | ------ | --------------- | ---------------------------- |
| placeholder | string | Any string      | Editor placeholder           |
| model_only  | bool   | true or false   | Only allow values from model |
