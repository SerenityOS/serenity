@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Time zone settings"
        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 14
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
                text: "Time zone:"
                fixed_width: 80
                name: "time_zone_label"
                text_alignment: "CenterLeft"
            }

            @GUI::ComboBox {
                name: "time_zone_input"
                max_visible_items: 24
            }
        }

        @GUI::ImageWidget {
            name: "time_zone_map"
            auto_resize: true
        }
    }
}
