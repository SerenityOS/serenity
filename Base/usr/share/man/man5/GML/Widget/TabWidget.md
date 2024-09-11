## Name

GML Tab Widget

## Description

Defines a GUI tab widget.

## Synopsis

`@GUI::TabWidget`

## Examples

```gml
@GUI::TabWidget {
  uniform_tabs: true

  @GUI::Widget {
    title: "First tab"
  }
  @GUI::Widget {
    title: "Second tab"
  }
}
```

## Registered Properties

| Property           | Type           | Possible values                                                             | Description                          |
| ------------------ | -------------- | --------------------------------------------------------------------------- | ------------------------------------ |
| container_margins  | margins        |                                                                             | Margins for the tab content          |
| reorder_allowed    | bool           | true or false                                                               | Allow changing the order of the tabs |
| show_close_buttons | bool           | true or false                                                               | Show a close button on each tab      |
| show_tab_bar       | bool           | true or false                                                               | Whether to display the tabs          |
| text_alignment     | text_alignment | Center, CenterLeft, CenterRight, TopLeft, TopRight, BottomLeft, BottomRight | Set the alignment of tab text        |
| tab_position       | tab_position   | Top, Bottom, Left, Right                                                    | Set the tab position                 |
| uniform_tabs       | bool           | true or false                                                               | Give all tabs the same width         |
