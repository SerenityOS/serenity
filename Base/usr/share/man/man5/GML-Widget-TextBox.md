## Name

GML Text Box Widget

## Description

Defines a GUI text box widget. A text box is a single-line text input field, and it inherits from [TextEditor](help://man/5/GML-Widget-TextEditor).

## Synopsis

`@GUI::TextBox`

## Examples

```gml
@GUI::TextBox {
    placeholder: "Text box"
    mode: "Editable"
}

@GUI::TextBox {
    text: "Disabled"
    enabled: false
}
```

## See also

-   [GML-Widget-TextEditor(5)](help://man/5/GML-Widget-TextEditor)
