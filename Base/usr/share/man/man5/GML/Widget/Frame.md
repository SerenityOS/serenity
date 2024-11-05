## Name

GML Frame Widget

## Description

Defines a GUI frame widget. Frames can contain other GUI widgets.

## Synopsis

`@GUI::Frame`

## Examples

```gml
@GUI::Frame {
    name: "tip_frame"
    min_width: 340
    min_height: 160
    layout: @GUI::HorizontalBoxLayout {
        margins: [0, 16, 0, 0]
    }
}
```

## Registered Properties

| Property  | Type    | Possible values                                              | Description        |
| --------- | ------- | ------------------------------------------------------------ | ------------------ |
| thickness | integer | 64-bit integer                                               | Sets the thickness |
| shadow    | enum    | Plain, Raised, Sunken                                        | Sets the shadow    |
| shape     | enum    | NoFrame, Box, Container, Panel, VerticalLine, HorizontalLine | Sets the shape     |
