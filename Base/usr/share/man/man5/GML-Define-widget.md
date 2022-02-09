## Name

Library or Application Defined Widgets

## Description

Some applications and libraries find it useful to define their own **LibGUI** widgets.

## Examples

```gml
@Web::OutOfProcessWebView {
    name: "web_view"
    min_width: 340
    min_height: 160
    visible: false
}
```

They are defined using `REGISTER_WIDGET()`, just as they are in **LIbGUI**.

```cpp
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
