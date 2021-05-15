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

            @PixelPaint::ImageEditor {
                name: "image_editor"
            }

            @PixelPaint::PaletteWidget {
                name: "palette_widget"
            }
        }

        @GUI::Widget {
            fill_with_background_color: true
            fixed_width: 200

            layout: @GUI::VerticalBoxLayout {
            }

            @GUI::GroupBox {
                title: "Layers"
                layout: @GUI::VerticalBoxLayout {
                    margins: [4, 16, 4, 8]
                }

                @PixelPaint::LayerListWidget {
                    name: "layer_list_widget"
                }
            }

            @PixelPaint::LayerPropertiesWidget {
                name: "layer_properties_widget"
            }

            @PixelPaint::ToolPropertiesWidget {
                name: "tool_properties_widget"
            }
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
