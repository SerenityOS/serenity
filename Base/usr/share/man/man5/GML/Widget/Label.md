## Name

GML Label Widget

## Description

Defines a GUI label widget.

## Synopsis

`@GUI::Label`

## Examples

```
@GUI::Label {
    text: "Copying files..."
    text_alignment: "CenterLeft"
    font_weight: "Bold"
    fixed_height: 32
    name: "files_copied_label"
}
```

## Registered Properties

| Property       | Type           | Possible values                                                             | Description                |
| -------------- | -------------- | --------------------------------------------------------------------------- | -------------------------- | --- | --- |
| text_alignment | text_alignment | Center, CenterLeft, CenterRight, TopLeft, TopRight, BottomLeft, BottomRight | Sets alignment of the text |
| text_wrapping  | text_wrapping  |                                                                             |                            |     |     |
