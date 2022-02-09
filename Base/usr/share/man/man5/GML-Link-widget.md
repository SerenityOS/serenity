## Name

Linking to GML widgets

## Description

How to link to your GML widgets in C++

## CMake

Include `compile_gml()` your applications CMake file.

```cmake
compile_gml(YourGMLFile.gml MyGML.h my_gml)
```

Include the name of the header file that will be compiled from your GML file in your `SOURCES`.

```cmake
set(SOURCES
    MyGML.h
)
```

## C++

You can then reference the variable you set (e.g. `calculator_gml`) in your main C++ file using `load_from_gml()`.

```cpp
load_from_gml(my_gml);
```

From there, you can use `find_descendant_of_type_named` to select widgets from your GML from their `name` property.

```gml
@GUI::Button {
    name: "mem_add_button"
    text: "M+"
    fixed_width: 35
    fixed_height: 28
    foreground_color: "red"
}
```
Is referenced using...
```cpp
load_from_gml(calculator_gml);
m_mem_add_button = *find_descendant_of_type_named<GUI::Button>("mem_add_button");
```
