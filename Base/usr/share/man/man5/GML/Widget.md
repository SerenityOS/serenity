# Widget

Defines a GUI widget.

```gml
@GUI::Widget {
  min_width: 200
  preferred_width: "opportunistic_grow"
  fixed_height: 215
  fill_with_background_color: true
  layout: @GUI::VerticalBoxLayout {

  }
}
```

## Registered Properties

| Property                    | Type          | Possible values                                       | Description                                                                                        |
| --------------------------- | ------------- | ----------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| x                           | int           |                                                       | x offset relative to parent                                                                        |
| y                           | int           |                                                       | y offset relative to parent                                                                        |
| visible                     | bool          |                                                       | Whether widget and children are drawn                                                              |
| focused                     | bool          |                                                       | Whether widget should be tab-focused on start                                                      |
| focus_policy                | enum          | ClickFocus, NoFocus, TabFocus, StrongFocus            | How the widget can receive focus                                                                   |
| enabled                     | bool          |                                                       | Whether this widget is enabled for interactive purposes, e.g. can be clicked                       |
| tooltip                     | string        |                                                       | Mouse tooltip to show when hovering over this widget                                               |
| min_size                    | ui_size       | {Regular, Shrink}²                                    | Minimum size this widget wants to occupy (Shrink is equivalent to 0)                               |
| max_size                    | ui_size       | {Regular, Grow}²                                      | Maximum size this widget wants to occupy                                                           |
| preferred_size              | ui_size       | {Regular, Shrink, Fit, OpportunisticGrow, Grow}²      | Preferred size this widget wants to occupy, if not otherwise constrained (Shrink means min_size)   |
| width                       | int           |                                                       | Width of the widget, independent of its layout size calculation                                    |
| height                      | int           |                                                       | Height of the widget, independent of its layout size calculation                                   |
| min_width                   | ui_dimension  | Regular, Shrink                                       | Minimum width this widget wants to occupy (Shrink is equivalent to 0)                              |
| min_height                  | ui_dimension  | Regular, Shrink                                       | Minimum height this widget wants to occupy (Shrink is equivalent to 0                              |
| max_width                   | ui_dimension  | Regular, Grow                                         | Maximum width this widget wants to occupy                                                          |
| max_height                  | ui_dimension  | Regular, Grow                                         | Maximum height this widget wants to occupy                                                         |
| preferred_width             | ui_dimension  | Regular, Shrink, Fit, OpportunisticGrow, Grow         | Preferred width this widget wants to occupy, if not otherwise constrained (Shrink means min_size)  |
| preferred_height            | ui_dimension  | Regular, Shrink, Fit, OpportunisticGrow, Grow         | Preferred height this widget wants to occupy, if not otherwise constrained (Shrink means min_size) |
| fixed_width                 | ui_dimension  | Regular (currently only integer values ≥0 allowed)    | Both maximum and minimum width; widget is fixed-width                                              |
| fixed_height                | ui_dimension  | Regular (currently only integer values ≥0 allowed)    | Both maximum and minimum height; widget is fixed-height                                            |
| fixed_size                  | ui_size       | {Regular}²                                            | Both maximum and minimum size; widget is fixed-size                                                |
| shrink_to_fit               | bool          |                                                       | Whether the widget shrinks as much as possible while still respecting its children's minimum sizes |
| font                        | string        | Any system-known font                                 | Font family                                                                                        |
| font_size                   | int           | Font size that is available on this family            | Font size                                                                                          |
| font_weight                 | font_weight   | Font weight that is available on this family and size | Font weight                                                                                        |
| font_type                   | enum          | FixedWidth, Normal                                    | Font type                                                                                          |
| foreground_color            | color         |                                                       | Color of foreground elements such as text                                                          |
| foreground_role             | string        | Any theme palette color role name                     | Palette color role (depends on system theme) for the foreground elements                           |
| background_color            | color         |                                                       | Color of the widget background                                                                     |
| background_role             | string        | Any theme palette color role name                     | Palette color role (depends on system theme) for the widget background                             |
| fill_width_background_color | bool          |                                                       | Whether to fill the widget's background with the background color                                  |
| layout                      | layout object |                                                       | Layout object for layouting this widget's children                                                 |
| relative_rect               | rect          |                                                       | Rectangle for relatively positioning the widget to the parent                                      |
