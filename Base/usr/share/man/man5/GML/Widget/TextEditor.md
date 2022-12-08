## Name

GML Text Editor Widget

## Description

Defines a GUI text editor widget. TextEditor is the base class of various kinds of text input boxes, both multiline and single-line.

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

| Property    | Type   | Possible values                                                                                       | Description                                                                   |
| ----------- | ------ | ----------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| text        | string |                                                                                                       | Set text                                                                      |
| placeholder | string |                                                                                                       | Set placeholder; the text that is shown when the user has not input any text. |
| mode        | enum   | Editable, ReadOnly, DisplayOnly                                                                       | Set editor mode                                                               |
| gutter      | bool   | Enable the gutter, a column left to the text content that is used for e.g. breakpoints in HackStudio. |
| ruler       | bool   | Enable the ruler, a column left to the text and gutter that shows the line numbers.                   |
