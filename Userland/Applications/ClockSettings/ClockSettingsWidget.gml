@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Time format"
        shrink_to_fit: false
        fixed_height: 240
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
        }

        @GUI::Label {
            text: "Set the date/time format used by the taskbar clock."
            text_alignment: "TopLeft"
        }

        @GUI::Widget {
            preferred_height: "fit"
            layout: @GUI::VerticalBoxLayout {
                spacing: 4
            }

            @GUI::RadioButton {
                name: "24hour_radio"
                text: "24-hour"
            }

            @GUI::RadioButton {
                name: "12hour_radio"
                text: "12-hour"
            }

            @GUI::CheckBox {
                name: "seconds_checkbox"
                text: "Show seconds"
            }

            @GUI::HorizontalSeparator {}

            @GUI::RadioButton {
                name: "custom_radio"
                text: "Custom:"
            }

            @GUI::TextBox {
                name: "custom_format_input"
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 4
            }

            @GUI::Label {
                text: "Preview:"
                text_alignment: "CenterLeft"
                preferred_width: 120
            }

            @GUI::Label {
                frame_style: "SunkenContainer"
                name: "clock_preview"
                text: "12:34:56"
            }
        }
    }
}
