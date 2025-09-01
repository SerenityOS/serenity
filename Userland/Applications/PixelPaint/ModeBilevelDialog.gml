@GUI::Widget {
    fill_with_background_color: true
    min_width: 260
    min_height: 60
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        fixed_height: 24

        @GUI::Label {
            text: "Method:"
            fixed_width: 60
            text_alignment: "CenterRight"
        }

        @GUI::ComboBox {
            name: "method_combobox"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}

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
