## Name

GML Scrollable Container Widget

## Description

Defines a GUI scrollable container widget.

Unlike other container widgets, this one does not allow multiple child widgets to be added, and thus also does not support setting a layout.

## Synopsis

`@GUI::ScrollableContainerWidget`

Content declared as `content_widget` property.

## Examples

```gml
@GUI::ScrollableContainerWidget {
	content_widget: @GUI::Widget {
		[...]
	}
}
```

## Registered Properties

| Property                           | Type          | Possible values        | Description                                 |
| ---------------------------------- | ------------- | ---------------------- | ------------------------------------------- |
| ~~layout~~                         |               | none                   | Does not take layout                        |
| scrollbars_enabled                 | bool          | true or false          | Status of scrollbar                         |
| should_hide_unnecessary_scrollbars | bool          | true or false          | Whether to hide scrollbars when unnecessary |
| content_widget                     | widget object | Any Subclass of Widget | The Widget to be displayed in the Container |
