## Name

GML Syntax

# Description

Overview of the GML syntax.

## Basic Syntax

For a start, GML can be thought of as a QML (Qt Markup Language) derivative. JSON is used as a sub-language in some places.

Each widget (or `Core::Object`, more generally) begins with `@`, with the name of the widget following. As this name refers to an actual C++ class, we need to include namespaces and separate them with `::` as usual. To define the properties of this widget, we follow the object name with curly brackets and a list of properties.

```gml
// The base widget class, straight from LibGUI.
@GUI::Widget {

}
// Some other common classes:
@GUI::HorizontalBoxLayout {

}
// (for compactness)
@GUI::Button {}
@GUI::Label {}
@GUI::TabWidget {}

// If your application-specific widget is registered, you can do this:
@MyApplication::CoolWidget {

}
```

As seen above, C-style single line comments with `//` are possible.

Inside an object, we declare the properties of the object as well as all of its children. The children are other widgets, specified plainly, while the properties take the form `key: value`. For almost all properties, the value is a JSON value, and each property expects a different kind of value. The documentation for each widget object contains information about the supported properties, their possible values, and what the properties do. Quite some properties are common to all widgets, see [GML/Widget(5)](help://man/5/GML/Widget).

```gml
// A 20x200 coolbar button with text.
@GUI::Button {
    width: 200
    height: 20
    text: "Operation failed successfully."
    button_style: "Coolbar"
}

// Two tabs, named "Tab 1" and "Tab 2", each containing a label.
@GUI::TabWidget {
    min_width: 150
    min_height: 200

    @GUI::Label {
        title: "Tab 1"
        text: "This is the first tab"
    }

    @GUI::Label {
        title: "Tab 2"
        text: "This is the second tab. What did you expect?"
    }
}
```

For layouting purposes, we use the special property `layout` which takes a layout object instead of a JSON value. The exact positioning of the children not only depends on the layout, but also on the container widget type. A widget's documentation contains information about such special cases if they exist.

```gml
// Make a frame that has two buttons horizontally laid out.
@GUI::Frame {
    min_height: 16
    min_width: 128

    layout: @GUI::HorizontalBoxLayout {
        // margin and spacing are frequent layouting properties.
        spacing: 5
    }
    @GUI::Button {
        text: "I'm the left button..."
    }
    @GUI::Button {
        text: "...and I'm the right button!"
    }
}
```

## Properties

A property's `value` is required to be either a JSON value or another object. Objects are only used for a few special properties which are documented with the widgets that need them.

Among the supported JSON values, these types can be further distinguished:

-   `int`: Regular JSON integer, note that JSON floats are not currently used.
-   `ui_dimension`: either positive integers, that work just like `int`, or special meaning values as JSON strings; see [UI Dimensions](help://man/5/GML/UI-Dimensions)
-   `bool`: Regular JSON boolean, may be enclosed in quotes but this is discouraged.
-   `string`: JSON string, also used as a basis for other types.
-   `readonly_string`: String-valued property that cannot be changed from C++ later.
-   `enum`: A JSON string with special semantics. This is always the value of some C++ enum, but it's enclosed in quotes.
    -   `font_weight`: Special case of `enum` for `Gfx::FontWeight`. One of `Thin`, `ExtraLight`, `Light`, `Regular`, `Medium`, `SemiBold`, `Bold`, `ExtraBold`, `Black`, `ExtraBlack`.
    -   `text_alignment`: Special case of `enum` for `Gfx::TextAlignment`. Supports values of the form `VerticalHorizontal`, where `Vertical` is the vertical alignment, one of `Center`, `Top`, `Bottom`, and `Horizontal` is the horizontal alignment, one of `Left`, `Right`, `Center`. The exception is the value `Center` (because `CenterCenter` is silly).
    -   `text_wrapping`: Special case of `enum` for `Gfx::TextWrapping`. One of `Wrap` or `DontWrap`.
-   `rect`: A JSON object of four `int`s specifying a rectangle. The keys are `x`, `y`, `width`, `height`.
-   `size`: A JSON array of two `int`s specifying two sizes in the format `[width, height]`.
-   `ui_size`: A JSON array of two `ui_dimension`s specifying two sizes in the format `[width, height]`
-   `margins`: A JSON array or object specifying four-directional margins as `int`s. If this is a JSON object, the four keys `top`, `right`, `bottom`, `left` are used. If this is a JSON array, there can be one to four integers. These have the following meaning (see also `GUI::Margins`):
    -   `[ all_four_margins ]`
    -   `[ top_and_bottom, right_and_left ]`
    -   `[ top, right_and_left, bottom ]`
    -   `[ top, right, bottom, left ]`

Properties are never ended with `;` or `,`, and the property key is never enclosed in quotes or double quotes.

## See also

-   The SerenityOS source code is the best source of further information on GML. The GML parser and AST builder can be found in the `GML` subdirectory in the `LibGUI` library. The `AK` JSON parsers and data structures are used for all JSON values.

## Examples

GML files can be found in the SerenityOS source tree with the `*.gml` extension.

```gml
@GUI::Widget {
    name: "my_first_widget"
}
```

```gml
// A Browser window GML
@GUI::Widget {
    name: "browser"
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @GUI::HorizontalSeparator {
        name: "top_line"
        fixed_height: 2
        visible: false
    }

    @GUI::TabWidget {
        name: "tab_widget"
        container_margins: [0]
        uniform_tabs: true
        text_alignment: "CenterLeft"
    }
}
```

```gml
// A SystemMonitor GML (abbreviated)
// This makes use of quite some custom objects and properties.
@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {
            margins: [0, 4, 4]
        }

        @GUI::TabWidget {
            name: "main_tabs"

            @GUI::Widget {
                title: "Processes"
                name: "processes"

                @GUI::TableView {
                    name: "process_table"
                    column_headers_visible: true
                }
            }

            @GUI::Widget {
                title: "Performance"
                name: "performance"
                background_role: "Button"
                fill_with_background_color: true
                layout: @GUI::VerticalBoxLayout {
                    margins: [4]
                }

                @GUI::GroupBox {
                    title: "CPU usage"
                    name: "cpu_graph"
                    layout: @GUI::VerticalBoxLayout {}
                }

                @GUI::GroupBox {
                    title: "Memory usage"
                    fixed_height: 120
                    layout: @GUI::VerticalBoxLayout {
                        margins: [6]
                    }

                    @SystemMonitor::GraphWidget {
                        stack_values: true
                        name: "memory_graph"
                    }
                }

                @SystemMonitor::MemoryStatsWidget {
                    name: "memory_stats"
                    // A custom property that refers back up to the GraphWidget for the memory graph.
                    memory_graph: "memory_graph"
                }
            }

            @SystemMonitor::StorageTabWidget {
                title: "Storage"
                name: "storage"
                layout: @GUI::VerticalBoxLayout {
                    margins: [4]
                }

                @GUI::TableView {
                    name: "storage_table"
                }
            }

            @SystemMonitor::NetworkStatisticsWidget {
                title: "Network"
                name: "network"
            }
        }
    }

    @GUI::Statusbar {
        segment_count: 3
        name: "statusbar"
    }
}
```
