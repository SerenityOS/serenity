@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {
            margins: [4]
        }

        @GUI::GroupBox {
            title: "Orientation"
            preferred_height: "fit"
            layout: @GUI::HorizontalBoxLayout {
                margins: [10, 8, 8]
            }

            @GUI::RadioButton {
                name: "orientation_horizontal_radio"
                text: "Horizontal"
            }

            @GUI::RadioButton {
                name: "orientation_vertical_radio"
                text: "Vertical"
            }
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            margins: [4]
        }
        preferred_height: "fit"

        @GUI::Label {
            text: "Offset"
            text_alignment: "CenterLeft"
        }

        @GUI::TextBox {
            name: "offset_text_box"
        }
    }

    @GUI::Widget {
        max_height: 24
        layout: @GUI::HorizontalBoxLayout {
            margins: [4]
        }

        @GUI::Layout::Spacer {}

        @GUI::DialogButton {
            name: "ok_button"
            text: "OK"
        }

        @GUI::DialogButton {
            name: "cancel_button"
            text: "Cancel"
        }
    }
}
