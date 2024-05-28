@Chess::NewGameWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::GroupBox {
        title: "Time Controls"
        preferred_height: "fit"
        min_width: 250
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::CheckBox {
            name: "unlimited_time_control"
            text: "Unlimited"
            checkbox_position: "Right"
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                text: "Minutes:"
                text_alignment: "CenterLeft"
            }

            @GUI::SpinBox {
                name: "minutes_spinbox"
                min: 0
                max: 180
                fixed_width: 50
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                text: "Seconds:"
                text_alignment: "CenterLeft"
            }

            @GUI::SpinBox {
                name: "seconds_spinbox"
                min: 0
                max: 59
                fixed_width: 50
            }
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {
                spacing: 16
            }

            @GUI::Label {
                text: "Increment in seconds:"
                text_alignment: "CenterLeft"
            }

            @GUI::SpinBox {
                name: "increment_spinbox"
                min: 0
                max: 180
                fixed_width: 50
            }
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        @GUI::Layout::Spacer {}

        @GUI::Button {
            name: "start_button"
            text: "Start"
            fixed_width: 80
        }
    }
}
