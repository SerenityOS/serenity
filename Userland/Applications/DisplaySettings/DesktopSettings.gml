@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [8, 8, 8, 8]
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 24, 16, 6]
        }

        title: "Workspaces"
        shrink_to_fit: true

        @GUI::Widget {
            fixed_height: 32

            layout: @GUI::HorizontalBoxLayout {
                margins: [6, 6, 6, 6]
            }

            @GUI::Label {
                text: "Rows:"
                text_alignment: "CenterRight"
            }

            @GUI::SpinBox {
                name: "virtual_desktop_rows_spinbox"
                min: 1
                max: 16
                orientation: "Horizontal"
            }

            @GUI::Label {
                text: "Columns:"
                text_alignment: "CenterRight"
            }

            @GUI::SpinBox {
                name: "virtual_desktop_columns_spinbox"
                min: 1
                max: 16
                orientation: "Horizontal"
            }
        }

        @GUI::Widget {
            fixed_height: 76

            layout: @GUI::HorizontalBoxLayout {
            }
            @GUI::Label {
                name: "light_bulb_label"
                fixed_height: 32
                fixed_width: 32
            }
            @GUI::Widget {
                layout: @GUI::VerticalBoxLayout {
                    margins: [6, 6, 6, 6]
                }
                @GUI::Label {
                    text: "Use the Ctrl+Alt+Arrow hotkeys to move between workspaces."
                    text_alignment: "TopLeft"
                    word_wrap: true
                }
                @GUI::Label {
                    text: "Use the Ctrl+Shift+Alt+Arrow hotkeys to move between\nworkspaces and move the active window."
                    text_alignment: "TopLeft"
                    word_wrap: true
                }
            }
        }
    }
}
