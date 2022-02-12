@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @DisplaySettings::MonitorWidget {
        name: "monitor_widget"
        fixed_width: 304
        fixed_height: 201
    }

    @GUI::Widget {
        fixed_height: 20
    }

    @GUI::Label {
        shrink_to_fit: true
        fixed_height: 16
        text_alignment: "CenterLeft"
        text: "Select a background picture:"
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 8
        }

        @GUI::IconView {
            name: "wallpaper_view"
        }

        @GUI::Widget {
            shrink_to_fit: true
            layout: @GUI::VerticalBoxLayout {}

            @GUI::Button {
                name: "wallpaper_open_button"
                tooltip: "Select wallpaper from file system"
                text: "Browse..."
                shrink_to_fit: true
            }

            @GUI::Widget {
                fixed_height: 12
            }

            @GUI::Label {
                text: "Mode:"
                text_alignment: "CenterLeft"
                fixed_height: 16
            }

            @GUI::ComboBox {
                name: "mode_combo"
            }

            @GUI::Widget {
                fixed_height: 12
            }

            @GUI::Label {
                text: "Color:"
                text_alignment: "CenterLeft"
                fixed_height: 16
            }

            @GUI::ColorInput {
                name: "color_input"
                fixed_width: 90
            }
        }
    }
}
