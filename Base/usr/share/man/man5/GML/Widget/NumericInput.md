## Name

GML Numeric Input Widget

## Description

Defines a GUI text box that only allows integers within a specified range.

## Synopsis

`@GUI::NumericInput`

## Examples

```gml
@GUI::NumericInput {
    min: 0
    value: 42
    max: 9000
}
```

## Registered Properties

| Property | Type | Possible values    | Description                            |
| -------- | ---- | ------------------ | -------------------------------------- |
| min      | int  | Any 64 bit integer | Minimum number the input can be set to |
| max      | int  | Any 64 bit integer | Maximum number the input can be set to |
| value    | int  | Any 64 bit integer | Initial value                          |
