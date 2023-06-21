## Name

GML Property Definition

## Description

How to register property to a widget.

**LibGUI** widget definitions contain macros that define the properties that can be used for a given widget. This is a useful feature to expose widget-type-specific configuration to GML.

Widgets understand all properties defined by their parents. Such as `x`, `y`, `name`, etc.

## `REGISTER_*` macros

There is one REGISTER macro for every generic property type, a list of which can be found in [GML Syntax(5)](help://man/5/GML/Syntax#Properties). If you need special behavior for one single property, it is usually enough to re-use one of the property types and do extra handling in the setter and getter.

The general syntax of the macros is as follows:

```cpp
REGISTER_TYPENAME_PROPERTY(property_name, getter, setter [, additional parameters...]);
```

The macros are to be called in the constructor. `property_name` is a string that GML can use to set this property. `getter` is the name of a member function on this class that acts as a getter for the property. The getter receives value of the property's type, which is a C++ value for `int`, `string`, `readonly_string`, `enum`, and `bool`, or a JSON value for anything else. `setter` is the name of a member function on this class that acts as a setter for the property, taking no arguments and returning the exact type that the getter expects. It is not possible to omit either of the functions, but they may be no-ops if needed. For non-settable strings `REGISTER_READONLY_STRING_PROPERTY` is used.

For some macros, additional parameters are required. Most of the macros and their parameters can be found in the `Core::Object` header; however some layout properties are situated in the `GUI::Layout` header.

## Examples

```cpp
REGISTER_STRING_PROPERTY("alt_text", alt_text, set_alt_text);
// The enum property requires that you specify all available enum fields plus their GML string representation.
REGISTER_ENUM_PROPERTY(
        "button_style", button_style, set_button_style, Gfx::ButtonStyle,
        { Gfx::ButtonStyle::Normal, "Normal" },
        { Gfx::ButtonStyle::Coolbar, "Coolbar" });
```

## See also

-   [GML Define widget(5)](help://man/5/GML/Define-widget)
