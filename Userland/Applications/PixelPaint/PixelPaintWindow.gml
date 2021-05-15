@GUI::Widget {
    name: "main"
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 0
        }

        @PixelPaint::ToolboxWidget {
            name: "toolbox"
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
            fixed_width: 230

            layout: @GUI::VerticalBoxLayout {
            }

            @PixelPaint::LayerListWidget {
                name: "layer_list_widget"
            }

            @PixelPaint::LayerPropertiesWidget {
                name: "layer_properties_widget"
            }

            @PixelPaint::ToolPropertiesWidget {
                name: "tool_properties_widget"
            }
        }
    }
}
