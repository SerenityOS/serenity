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
            text: "Restrict mask to luminosity values:"
            text_alignment: "CenterLeft"
        }

        @GUI::HorizontalRangeSlider {
            name: "full_masking"
            max: 255
            min: 0
            lower_range: 25
            upper_range: 230
            page_step: 10
        }

        @GUI::Widget {
            name: "range_illustration"
        }

        @GUI::HorizontalRangeSlider {
            name: "edge_masking"
            max: 255
            min: 0
            lower_range: 0
            upper_range: 255
            page_step: 10
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
