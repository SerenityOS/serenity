# Widget

Defines a GUI widget.

```gml
@GUI::Widget {
  fixed_width: 250
  fixed_height: 215
  fill_with_background_color: true
  layout: @GUI::VerticalBoxLayout {

  }
}
```

## Registered Properties

| Property                    | Type          | Possible values                                       | Description                                                                                       |
| --------------------------- | ------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| x                           | int           |                                                       | x offset relative to parent                                                                       |
| y                           | int           |                                                       | y offset relative to parent                                                                       |
| visible                     | bool          |                                                       | Whether widget and children are drawn                                                             |
| focused                     | bool          |                                                       | Whether widget should be tab-focused on start                                                     |
| focus_policy                | enum          | ClickFocus, NoFocus, TabFocus, StrongFocus            | How the widget can receive focus                                                                  |
| enabled                     | bool          |                                                       | Whether this widget is enabled for interactive purposes, e.g. can be clicked                      |
| tooltip                     | string        |                                                       | Mouse tooltip to show when hovering over this widget                                              |
| min_size                    | size          |                                                       | Minimum size this widget wants to occupy                                                          |
| max_size                    | size          |                                                       | Maximum size this widget wants to occupy                                                          |
| width                       | int           |                                                       | Width of the widget, independent of its layout size calculation                                   |
| height                      | int           |                                                       | Height of the widget, independent of its layout size calculation                                  |
| min_width                   | int           | smaller than max_width                                | Minimum width this widget wants to occupy                                                         |
| min_height                  | int           | smaller than max_height                               | Minimum height this widget wants to occupy                                                        |
| max_width                   | int           | greater than min_width                                | Maximum width this widget wants to occupy                                                         |
| max_height                  | int           | greater than min_height                               | Maximum height this widget wants to occupy                                                        |
| fixed_width                 | int           |                                                       | Both maximum and minimum width; widget is fixed-width                                             |
| fixed_height                | int           |                                                       | Both maximum and minimum height; widget is fixed-height                                           |
| fixed_size                  | size          |                                                       | Both maximum and minimum size; widget is fixed-size                                               |
| shrink_to_fit               | bool          |                                                       | Whether the widget shrinks as much as possible while still respecting its childrens minimum sizes |
| font                        | string        | Any system-known font                                 | Font family                                                                                       |
| font_size                   | int           | Font size that is available on this family            | Font size                                                                                         |
| font_weight                 | font_weight   | Font weight that is available on this family and size | Font weight                                                                                       |
| font_type                   | enum          | FixedWidth, Normal                                    | Font type                                                                                         |
| foreground_color            | color         |                                                       | Color of foreground elements such as text                                                         |
| foreground_role             | string        | Any theme palette color role name                     | Palette color role (depends on system theme) for the foreground elements                          |
| background_color            | color         |                                                       | Color of the widget background                                                                    |
| background_role             | string        | Any theme palette color role name                     | Palette color role (depends on system theme) for the widget background                            |
| fill_width_background_color | bool          |                                                       | Whether to fill the widget's background with the background color                                 |
| layout                      | layout object |                                                       | Layout object for layouting this widget's children                                                |
| relative_rect               | rect          |                                                       | Rectangle for relatively positioning the widget to the parent                                     |
