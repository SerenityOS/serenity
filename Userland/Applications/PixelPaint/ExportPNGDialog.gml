@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::VerticalBoxLayout {}

        @GUI::CheckBox {
            name: "transparency_checkbox"
            text: "Preserve transparency"
            checked: true
        }

        @GUI::Label {
            text: "Compression Level:"
            text_alignment: "CenterLeft"
        }

        @GUI::ValueSlider {
            name: "compression_slider"
            min: 0
            max: 3
            value: 3
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
