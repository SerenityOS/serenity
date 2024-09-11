## Name

GML Spin Box Widget

## Description

Defines a GUI spin box widget. This is a number input with buttons to increment and decrement the value.

## Synopsis

`@GUI::SpinBox`

## Examples

```gml
@GUI::SpinBox {
    name: "thickness_spinbox"
    min: 0
    max: 2
}
```

## Registered Properties

| Property | Type | Possible values    | Description                                  |
| -------- | ---- | ------------------ | -------------------------------------------- |
| min      | int  | Any 64 bit integer | Minimum number the spin box can increment to |
| max      | int  | Any 64 bit integer | Maximum number the spin box can increment to |
