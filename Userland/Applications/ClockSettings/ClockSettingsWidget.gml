@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Time Format"
        shrink_to_fit: false
        fixed_height: 160
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
        }

        @GUI::Label {
            text: "Set the date/time format used by the taskbar clock."
            text_alignment: "TopLeft"
        }

        @GUI::Widget {
            shrink_to_fit: true
            layout: @GUI::VerticalBoxLayout {
                spacing: 4
            }

            @GUI::RadioButton {
                name: "24hour_radio"
                text: "24-hour (12:34:56)"
            }

            @GUI::RadioButton {
                name: "12hour_radio"
                text: "12-hour (12:34 a.m)"
            }

            @GUI::RadioButton {
                name: "custom_radio"
                text: "Custom:"
            }

            @GUI::TextBox {
                name: "custom_format_input"
            }
        }
    }
}
