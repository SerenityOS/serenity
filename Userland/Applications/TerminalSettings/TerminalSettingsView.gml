@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Background Opacity"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 16
        }

        @GUI::OpacitySlider {
            name: "background_opacity_slider"
            min: 0
            max: 255
            orientation: "Horizontal"
        }
    }

    @GUI::GroupBox {
        title: "Terminal Font"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 16
        }

        @GUI::CheckBox {
            name: "terminal_font_defaulted"
            text: "Use system default"
        }

        @GUI::Widget {
            preferred_height: "fit"
            name: "terminal_font_selection"
            layout: @GUI::HorizontalBoxLayout {
                spacing: 6
            }

            @GUI::Label {
                background_role: "Base"
                shape: "Container"
                shadow: "Sunken"
                thickness: 2
                fill_with_background_color: true
                name: "terminal_font_label"
            }

            @GUI::Button {
                text: "..."
                name: "terminal_font_button"
                fixed_width: 30
            }
        }
    }

    @GUI::GroupBox {
        title: "Cursor Settings"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
        }

        @GUI::RadioButton {
            name: "terminal_cursor_block"
            text: "Block cursor"
        }

        @GUI::RadioButton {
            name: "terminal_cursor_underline"
            text: "Underline cursor"
        }

        @GUI::RadioButton {
            name: "terminal_cursor_bar"
            text: "Bar cursor"
        }

        @GUI::CheckBox {
            name: "terminal_cursor_blinking"
            text: "Blinking cursor"
        }
    }

    @GUI::GroupBox {
        title: "Color Scheme"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 16
        }

        @GUI::ComboBox {
            name: "color_scheme_combo"
        }
    }
}
