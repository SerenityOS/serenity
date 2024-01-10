@KeyboardSettings::KeymapDialog {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::ComboBox {
        name: "keymaps_combobox"
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 6
        }
        preferred_height: "fit"

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
