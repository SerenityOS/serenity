## Name

GML Text Editor Widget

## Description

Defines a GUI text editor widget.

## Synopsis

`@GUI::TextEditor`

## Examples

```gml
@GUI::TextEditor {
    name: "text_editor"
    placeholder: "Text editor"
}
```

## Registered Properties

| Property    | Type   | Possible values                 | Description     |
| ----------- | ------ | ------------------------------- | --------------- |
| text        | string | Any string                      | Set text        |
| placeholder | string | Any string                      | Set placeholder |
| mode        | enum   | Editable, ReadOnly, DisplayOnly | Set editor mode |
