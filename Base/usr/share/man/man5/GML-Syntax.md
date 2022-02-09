## Name

GML Basic Syntax

# Description

How to write GML using proper syntax.

## Basic Syntax

Each widget begins with `@GUI::`, with the name of the widget following. To define the properties of this widget, we follow with curly brackets and a list of properties.

## Properties

A property's `value` is required to be in the property's set `type`:

- `int`
- `bool`
- `string`
- `readonly_string`
- `enum`
- `font_weight`
- `text_alignment`
- `text_wrapping`
- `rect`
- `size`
- `margins`

Properties are never ended with `;` or `,`, and the property name is never enclosed in quotes or double quotes.

Properties are always surrounded by curly brackets (e.g. `{}`). If no properties are set however, no brackets are required.

## Examples

```gml
@GUI::Widget {
    name: "my_first_widget"
}
```
