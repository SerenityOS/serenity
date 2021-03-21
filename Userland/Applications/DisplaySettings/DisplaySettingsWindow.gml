@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [4, 4, 4, 4]
    }

    @DisplaySettings::MonitorWidget {
        name: "monitor_widget"
        fixed_width: 338
        fixed_height: 248
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
            button_style: "CoolBar"
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
            text: "Resolution:"
            text_alignment: "CenterLeft"
            fixed_width: 70
        }

        @GUI::ComboBox {
            name: "resolution_combo"
            fixed_width: 90
        }

        @GUI::Widget

        @GUI::Label {
            text: "Display scale:"
            text_alignment: "CenterLeft"
            fixed_width: 95
        }

        @GUI::RadioButton {
            name: "scale_1x"
            text: "1x"
        }

        @GUI::RadioButton {
            name: "scale_2x"
            text: "2x"
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

    @GUI::Widget

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::HorizontalBoxLayout

        @GUI::Widget

        @GUI::Button {
            name: "ok_button"
            text: "OK"
            fixed_width: 75
        }

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
            fixed_width: 75
        }

        @GUI::Button {
            name: "apply_button"
            text: "Apply"
            fixed_width: 75
        }
    }
}
