@Maps::FavoritesEditDialog {
    fixed_width: 260
    fixed_height: 61
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Name:"
            text_alignment: "CenterLeft"
            fixed_width: 30
        }

        @GUI::TextBox {
            name: "name_textbox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Layout::Spacer {}

        @GUI::DialogButton {
            name: "ok_button"
            text: "OK"
            fixed_width: 75
        }

        @GUI::DialogButton {
            name: "cancel_button"
            text: "Cancel"
            fixed_width: 75
        }
    }
}
