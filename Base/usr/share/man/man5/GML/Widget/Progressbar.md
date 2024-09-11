## Name

GML Progress Bar Widget

## Description

Defines a GUI progress bar widget.

## Synopsis

```gml
@GUI::Progressbar
```

## Examples

```gml
@GUI::Progressbar {
    fixed_height: 22
    name: "progressbar"
    min: 0
}
```

## Registered Properties

| Property | Type   | Possible values                   | Description                                |
| -------- | ------ | --------------------------------- | ------------------------------------------ |
| text     | string | Any string                        | Sets progress text                         |
| format   | enum   | NoText, Percentage, ValueSlashMax | Sets the format of the progress indication |
| min      | int    | Any 64 bit integer                | Sets the minimum progress value            |
| max      | int    | Any 64 bit integer                | Set the maximum progress value             |
