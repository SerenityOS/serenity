## Name

Library or Application Defined Widgets

## Description

Some applications and libraries find it useful to define their own widgets.

This is done using `REGISTER_WIDGET()`, just as in **LibGUI**. The syntax of the macro is as follows:

```cpp
REGISTER_WIDGET(namespace, class_name)
```

This means that every registered widget has to be placed in a namespace. For applications that usually do not need their own namespace, a common approach is to use the application name as the namespace for these registered widgets.

Note that registered widgets need to be constructible without any arguments.

## Examples

```gml
@Web::OutOfProcessWebView {
    name: "web_view"
    min_width: 340
    min_height: 160
    visible: false
}
```

```cpp
// OutOfProcessWebView.cpp

REGISTER_WIDGET(Web, OutOfProcessWebView)

...

OutOfProcessWebView::OutOfProcessWebView()
{
    set_should_hide_unnecessary_scrollbars(true);
    set_focus_policy(GUI::FocusPolicy::StrongFocus);

    create_client();
}

...
```

## See also

-   [GML Define property(5)](help://man/5/GML/Define-property)
