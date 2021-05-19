@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [4, 4, 4, 4]
    }

    @DisplaySettings::MonitorWidget {
        name: "monitor_widget"
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::HorizontalBoxLayout

        @GUI::Label {
            text: "Wallpaper:"
            text_alignment: "CenterLeft"
            fixed_width: 70
        }

        @GUI::ComboBox {
            name: "wallpaper_combo"
        }

        @GUI::Button {
            name: "wallpaper_open_button"
            tooltip: "Select wallpaper from file system."
            button_style: "Coolbar"
            fixed_width: 22
            fixed_height: 22
        }
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::HorizontalBoxLayout

        @GUI::Label {
            text: "Modes:"
            text_alignment: "CenterLeft"
            fixed_width: 70
        }

        @GUI::ComboBox {
            name: "mode_combo"
        }
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::HorizontalBoxLayout

        @GUI::Label {
            text: "Color:"
            text_alignment: "CenterLeft"
            fixed_width: 70
        }

        @GUI::ColorInput {
            name: "color_input"
            fixed_width: 90
        }
    }
}
