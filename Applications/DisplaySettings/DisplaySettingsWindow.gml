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
        fixed_height: 22
        layout: @GUI::HorizontalBoxLayout {
            margins: [0, 4, 0, 0]
        }

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
            tooltip: "Select Wallpaper from file system."
            fixed_width: 22
            fixed_height: 22
        }
    }

    @GUI::Widget {
        fixed_height: 22
        layout: @GUI::HorizontalBoxLayout {
            margins: [0, 4, 0, 0]
        }

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
        fixed_height: 22
        layout: @GUI::HorizontalBoxLayout {
            margins: [0, 4, 0, 0]
        }

        @GUI::Label {
            text: "Resolution:"
            text_alignment: "CenterLeft"
            fixed_width: 70
        }

        @GUI::ComboBox {
            name: "resolution_combo"
        }
    }


    @GUI::Widget {
        fixed_height: 22
        layout: @GUI::HorizontalBoxLayout {
            margins: [0, 4, 0, 0]
        }

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

    @GUI::Widget {
        fixed_height: 22
        layout: @GUI::HorizontalBoxLayout {
        }

        @GUI::Widget {
        }

        @GUI::Button {
            name: "ok_button"
            text: "OK"
            fixed_width: 60
        }

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
            fixed_width: 60
        }

        @GUI::Button {
            name: "apply_button"
            text: "Apply"
            fixed_width: 60
        }
    }
}
