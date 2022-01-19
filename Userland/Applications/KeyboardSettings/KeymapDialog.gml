@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::ComboBox {
        name: "keymaps_combobox"
    }

    @GUI::Widget {
        fixed_height: 24

        layout: @GUI::HorizontalBoxLayout {
        }

        @GUI::Widget {
        }

        @GUI::Button {
            name: "ok_button"
            text: "OK"
            fixed_width: 75
        }

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
            fixed_width: 75
        }
    }
}
