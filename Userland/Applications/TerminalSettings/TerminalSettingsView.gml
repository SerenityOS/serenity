@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Background Opacity"
        fixed_height: 70

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
        fixed_height: 100

        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 16
        }

        @GUI::CheckBox {
            name: "terminal_font_defaulted"
            text: "Use system default"
        }

        @GUI::Widget {
            shrink_to_fit: true
            name: "terminal_font_selection"

            layout: @GUI::HorizontalBoxLayout {
                spacing: 6
            }

            @GUI::Frame {
                background_role: "Base"
                fill_with_background_color: true

                layout: @GUI::VerticalBoxLayout {
                }

                @GUI::Label {
                    name: "terminal_font_label"
                }
            }

            @GUI::Button {
                text: "..."
                name: "terminal_font_button"
                fixed_width: 30
            }
        }
    }

    @GUI::GroupBox {
        title: "Color Scheme"
        fixed_height: 70

        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 16
        }

        @GUI::ComboBox {
            name: "color_scheme_combo"
        }
    }
}
