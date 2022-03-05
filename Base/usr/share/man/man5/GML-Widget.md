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

| Property                    | Type        | Possible values                                    | Description                                    |
|-----------------------------|-------------|----------------------------------------------------|------------------------------------------------|
| x                           | int         |                                                    |                                                |
| y                           | int         |                                                    |                                                |
| visible                     | bool        |                                                    |                                                |
| focused                     | bool        |                                                    |                                                |
| enabled                     | bool        |                                                    |                                                |
| tooltip                     | string      |                                                    |                                                |
| min_size                    | size        |                                                    |                                                |
| max_size                    | size        |                                                    |                                                |
| width                       | int         |                                                    |                                                |
| height                      | int         |                                                    |                                                |
| min_width                   | int         |                                                    |                                                |
| min_height                  | int         |                                                    |                                                |
| max_width                   | int         |                                                    |                                                |
| max_height                  | int         |                                                    |                                                |
| fixed_width                 | int         |                                                    |                                                |
| fixed_height                | int         |                                                    |                                                |
| fixed_size                  | size        |                                                    |                                                |
| shrink_to_fit               | bool        |                                                    |                                                |
| font                        | string      |                                                    |                                                |
| font_size                   | int         |                                                    |                                                |
| font_weight                 | font_weight |                                                    |                                                |
| font_type                   | enum        | FixedWidth, Normal                                 |                                                |
| foreground_color            | color       |                                                    |                                                |
| foreground_role             | string      |                                                    |                                                |
| background_color            | color       |                                                    |                                                |
| background_role             | string      |                                                    |                                                |
| fill_width_background_color | bool        |                                                    |                                                |
| layout                      | widget      | @GUI::VerticalBoxLayout, @GUI::HorizontalBoxLayout |                                                |
| relative_rect               | rect        |                                                    |                                                |
| focus_policy                | enum        | ClickFocus, NoFocus, TabFocus, StrongFocus         |                                                |
| margins                     |             |                                                    |                                                |
