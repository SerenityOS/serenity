## Name

GML Usage

## Description

How to use GML in SerenityOS C++ applications

## CMake

Include `stringify_gml()` your applications CMake file. The header file name and GML string name are not fixed but must follow this convention.

```cmake
stringify_gml(MyApp.gml MyAppGML.h my_app_gml)
```

Include the name of the header file that will be compiled from your GML file in your `SOURCES`.

```cmake
set(SOURCES
    MyAppGML.h
)
```

## C++

You can then reference the variable you set (e.g. `my_app_gml`) in your main C++ file using `load_from_gml()`.

```cpp
load_from_gml(my_app_gml);
```

From there, you can use `find_descendant_of_type_named` to select widgets from your GML by their `name` property.

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
