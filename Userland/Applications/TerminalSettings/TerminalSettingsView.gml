@TerminalSettings::ViewWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Terminal font"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
            spacing: 8
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
                frame_style: "SunkenContainer"
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
        title: "Background opacity"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
            spacing: 8
        }

        @GUI::HorizontalOpacitySlider {
            name: "background_opacity_slider"
            min: 0
            max: 255
            orientation: "Horizontal"
        }
    }

    @GUI::Widget {
        preferred_height: "shrink"
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::GroupBox {
            title: "Cursor shape"
            layout: @GUI::VerticalBoxLayout {
                margins: [8]
            }

            @GUI::RadioButton {
                name: "terminal_cursor_block"
                text: "Block"
            }

            @GUI::RadioButton {
                name: "terminal_cursor_underline"
                text: "Underscore"
            }

            @GUI::RadioButton {
                name: "terminal_cursor_bar"
                text: "Vertical bar"
            }
        }

        @GUI::GroupBox {
            title: "Cursor behavior"
            layout: @GUI::VerticalBoxLayout {
                margins: [8]
            }

            @GUI::CheckBox {
                name: "terminal_cursor_blinking"
                text: "Blink cursor"
            }
        }
    }

    @GUI::GroupBox {
        title: "Scrollback"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
            spacing: 8
        }

        @GUI::Widget {
            preferred_height: "shrink"
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::SpinBox {
                name: "history_size_spinbox"
                min: 0
                max: 40960
                preferred_width: 100
            }

            @GUI::Label {
                text: "lines"
                autosize: true
            }
        }

        @GUI::CheckBox {
            name: "terminal_show_scrollbar"
            text: "Show terminal scrollbar"
        }
    }
}
