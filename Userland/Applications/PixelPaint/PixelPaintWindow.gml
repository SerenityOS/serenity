@GUI::Widget {
    name: "main"
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolbarContainer {
        name: "toolbar_container"

        @GUI::Toolbar {
            name: "toolbar"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 2
        }

        @GUI::ToolbarContainer {
            name: "toolbar_container"

            @PixelPaint::ToolboxWidget {
                name: "toolbox"
            }
        }

        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                spacing: 0
            }

            @GUI::TabWidget {
                name: "tab_widget"
                container_margins: [4, 5, 5, 4]
                reorder_allowed: true
                show_close_buttons: true
            }

            @PixelPaint::PaletteWidget {
                name: "palette_widget"
            }
        }

        @GUI::Widget {
            fill_with_background_color: true
            fixed_width: 200
            layout: @GUI::VerticalBoxLayout {}

            @GUI::GroupBox {
                title: "Layers"
                layout: @GUI::VerticalBoxLayout {
                    margins: [6]
                }

                @PixelPaint::LayerListWidget {
                    name: "layer_list_widget"
                }
            }

            @PixelPaint::LayerPropertiesWidget {
                name: "layer_properties_widget"
                max_height: 94
            }

            @GUI::GroupBox {
                title: "Histogram"
                preferred_height: "shrink"
                layout: @GUI::VerticalBoxLayout {
                    margins: [6]
                }

                @PixelPaint::HistogramWidget {
                    name: "histogram_widget"
                    min_height: 65
                }
            }

            @GUI::GroupBox {
                title: "Vectorscope"
                min_height: 80
                layout: @GUI::VerticalBoxLayout {
                    margins: [6]
                }

                @PixelPaint::VectorscopeWidget {
                    name: "vectorscope_widget"
                    preferred_height: "fit"
                }
            }

            @PixelPaint::ToolPropertiesWidget {
                name: "tool_properties_widget"
                max_height: 144
            }
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
