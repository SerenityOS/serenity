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

| Property | Type | Possible values | Description              |
|----------|------|-----------------|--------------------------|
| autosize | bool | true or false   | Determines if auto-sized |
