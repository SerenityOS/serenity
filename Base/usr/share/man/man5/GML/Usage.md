## Name

GML Usage

## Description

How to use GML in SerenityOS C++ applications

## Note

This manpage describes the new method of loading GML files via generated C++ code. There also is the runtime `load_from_gml` function which has the same behavior. The runtime method should not be used except for specific use cases such as [GMLPlayground](help://man/1/Applications/GMLPlayground).

## CMake

Include `compile_gml()` your application's CMake file. The generated source file name is not fixed, but should follow this convention.

```cmake
compile_gml(MyApp.gml MyAppGML.cpp)
```

Include the name of the source file that will be compiled from your GML file in your `SOURCES`.

```cmake
set(SOURCES
    MyAppGML.cpp
)
```

## C++

The C++ source file that is generated will provide an implementation for the `ErrorOr<NonnullRefPtr<MyApp::Widget>> MyApp::Widget::try_create()` function, given that the root widget of the GML file is `MyApp::Widget`. For this to work, you have to add a declaration for this function in the header of the `MyApp::Widget` class. (The function will not collide with functions provided by `Core::Object`, do not worry.) Additionally, there must be a no-argument constructor in `MyApp::Widget`, which is used by the `try_create` implementation.

Calling the `try_create` function directly or indirectly (e.g. `Window::try_set_main_widget`) will now automatically load the structure defined in GML.

From there, you can use `find_descendant_of_type_named` to select widgets from your GML by their `name` property.

```gml
@MyApp::Widget {
    @GUI::Button {
        name: "mem_add_button"
        text: "M+"
        fixed_width: 35
        fixed_height: 28
        foreground_color: "red"
    }
}
```

```cpp
// MyApp::Widget::foo_bar
m_mem_add_button = *find_descendant_of_type_named<GUI::Button>("mem_add_button");
```

### `initialize` Pattern

Initialization, like adding models, attaching callbacks, etc., should be done in a member function with the signature:

```cpp
// MyApp::Widget
ErrorOr<void> initialize();
```

This initializer function, if it exists, will automatically be called after the structure of your widget was set up by the auto-generated GML code.

The only case where this function cannot be used is when your initialization requires additional parameters, like a GUI window. You may still consider moving as much initialization to the canonical function as possible.
