## Name

GML Property Definition

## Description

How to register property to a widget.

**LIbGUI** widget definitions contain macros that define the properties that can be used for a given widget.

However, widgets also understand properties defined by their parents. Such as  `x`, `y`, `name`, etc.

## Examples

```cpp
REGISTER_ENUM_PROPERTY(
        "button_style", button_style, set_button_style, Gfx::ButtonStyle,
        { Gfx::ButtonStyle::Normal, "Normal" },
        { Gfx::ButtonStyle::Coolbar, "Coolbar" });
```
