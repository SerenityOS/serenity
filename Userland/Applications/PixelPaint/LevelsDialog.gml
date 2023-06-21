@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {}

        @GUI::Label {
            name: "context_label"
            enabled: true
            fixed_height: 20
            visible: true
            text: "Working on Background"
            text_alignment: "CenterLeft"
        }

        @GUI::Label {
            enabled: true
            fixed_height: 20
            visible: true
            text: "Brightness"
            text_alignment: "Left"
        }

        @GUI::ValueSlider {
            name: "brightness_slider"
            value: 0
            max: 255
            min: -255
            page_step: 10
        }

        @GUI::Label {
            enabled: true
            fixed_height: 20
            visible: true
            text: "Contrast"
            text_alignment: "Left"
        }

        @GUI::ValueSlider {
            name: "contrast_slider"
            value: 0
            max: 255
            min: -255
            page_step: 10
        }

        @GUI::Label {
            enabled: true
            fixed_height: 20
            visible: true
            text: "Gamma"
            text_alignment: "Left"
        }

        @GUI::ValueSlider {
            name: "gamma_slider"
            value: 100
            max: 350
            min: 1
            page_step: 10
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
