## Name

GML property definition format

## Description

GML properties and widgets are described abstractly in JSON files. These descriptions are then used by multiple tools:

-   GMLCompiler uses the descriptions, besides error-checking, to determine which C++ headers to include, how to convert JSON property values to C++ values, etc.
-   [GMLPlayground](help://man/1/Applications/GMLPlayground) uses the descriptions for auto-complete.
-   (Property registration?)
-   Widget manpages (for LibGUI) are auto-generated from the JSON files.

## Format

A property definition file consists of an array of widget descriptions. Multiple JSON files may be merged (as done by e.g. the GMLCompiler) by concatenating the elements of this top-level array.

Each widget definition object can contain the following keys:

-   `name`: required, string. Fully qualified C++ name of the widget, including namespaces. Must be unique; it is recommended to use per-application namespaces for widgets that appear in GML.
-   `header`: required, string. C++ header which defines this widget. May use any absolute include format that is accepted in Serenity, most importantly:
    -   `LibGUI/Widget.h` for library headers
    -   `Applications/Help/MainWidget.h` for application headers
-   `inherits`: optional, string. `name` of another widget that this widget inherits from. Should reflect the C++ inheritance. All properties defined on that widget as well as its parents are also going to be available on this widget.
-   `description`: optional, string. Description of the widget, displayed in manpages and auto-complete. Shouldn't contain a trailing period.
-   `properties`: required, array of objects. All properties this widget defines. Properties in this list may override properties defined in parents.

Each property definition is an object with the following keys:

-   `name`: required, string. Identifier of the property as used in GML.
-   `getter`: optional, string. Identifier of the C++ getter for this property. If not provided (recommended!), the `name` is used as the getter instead.
-   `setter`: optional, string. Identifier of the C++ setter for this property. If not provided (recommended!), `set_name` is used as the setter instead.
-   `description`: optional, string. Description of the property, displayed in manpages and auto-complete. Shouldn't contain a trailing period.
-   `type`: required, string. Type of the property, as a C++(-like) type name. Supported types are:
    -   `String`: GML value is a JSON string, property value is an `AK::String`. The setter may receive `String` or `String const&`.
    -   `ByteString`: GML value is a JSON string, property value is an `AK::ByteString`. This type should only be used for file system paths. The setter may receive `ByteString` or `ByteString const&`.
    -   `i64`: GML value is a JSON number that fits in a 64-bit integer, property value is an `i64`.
    -   `u64`: GML value is a JSON number that fits in an unsigned 64-bit integer, property value is a `u64`.
    -   `double`: GML value is a JSON number, property value is a `double`.
    -   `bool`: GML value is a boolean, property value is a `bool`.
    -   `Gfx::Bitmap`: GML value is a string, property value is a `NonnullRefPtr<Gfx::Bitmap>` or a compatible type like `RefPtr<Gfx::Bitmap>`.
    -   `GUI::UIDimension`: Property value is a `GUI::UIDimension`. The valid GML values are detailed in [GML/UI-Dimensions(5)](help://man/5/GML/UI-Dimensions).
    -   `Gfx::Color`: Property value is a color. It may be anything accepted by the `Gfx::Color` CSS-like string parser, which includes hex colors and `rgb()`, `rgba()`, and CSS named colors.
    -   `GUI::Margins`: GML value is an array of integers (1-4), property value is `GUI::Margins`.
    -   `Array<T>`: GML value is an array of homogenous inner types, property value is any number of `AK::Array`-types (between `Array<T, min_values>` and `Array<T, max_values>`). The inner type `T` must be one of the other legal property types.
    - `Variant<T, U, V, W, ...>`: GML value has one of a few types, property value is `AK::Variant` of these types. The inner types must be other legal property types.
    -   Any other type name is assumed to be the fully qualified name of a C++ enum. The GML value is a string that must be one of the enum variants, and the property value is the C++ enum.
-   `min_values`: optional, integer. For array-typed properties: Minimum number of values that must be provided. Set min_values = max_values to only allow one array size.
-   `max_values`: optional, integer. For array-typed properties: Maximum number of values that can be provided.
