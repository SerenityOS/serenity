@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {}

        @GUI::Label {
            name: "hint_label"
            enabled: true
            fixed_height: 20
            visible: true
            text: "Restrict mask to colors:"
            text_alignment: "CenterLeft"
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::VerticalSlider {
                name: "color_range"
                max: 180
                min: 0
                value: 15
                page_step: 10
            }

            @GUI::Widget {
                name: "color_wheel_container"
            }

            @GUI::VerticalSlider {
                name: "hardness"
                max: 100
                min: 0
                value: 50
                page_step: 10
            }
        }

        @GUI::HorizontalRangeSlider {
            name: "saturation_value"
            max: 100
            min: -100
            lower_range: -100
            upper_range: 100
            page_step: 5
            show_label: false
        }

        @GUI::CheckBox {
            name: "mask_visibility"
            text: "Show layer mask"
        }

        @GUI::HorizontalSeparator {}
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        fixed_height: 22

        @GUI::Layout::Spacer {}

        @GUI::DialogButton {
            name: "apply_button"
            text: "OK"
        }

        @GUI::DialogButton {
            name: "cancel_button"
            text: "Cancel"
        }
    }
}
