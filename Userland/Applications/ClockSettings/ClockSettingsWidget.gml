@GUI::Frame {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Time Zone Settings"

        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 16
        }

        @GUI::Label {
            text: "Change the system's time zone used for the clock and other applications."
            text_alignment: "TopLeft"
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 4
            }

            @GUI::Label {
                text: "Time Zone:"
                fixed_width: 80
                name: "time_zone_label"
                text_alignment: "CenterLeft"
            }

            @GUI::ComboBox {
                name: "time_zone_input"
            }
        }

        @GUI::ImageWidget {
            name: "time_zone_map"
            auto_resize: true
        }
    }
}
