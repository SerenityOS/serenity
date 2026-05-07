@UsersSettings::AddMemberDialog {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
        spacing: 4
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::Label {
            text: "User:"
            text_alignment: "CenterLeft"
            fixed_width: 50
        }

        @GUI::ComboBox {
            name: "user_combobox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::Layout::Spacer {}

        @GUI::DialogButton {
            name: "ok_button"
            text: "Add"
            fixed_width: 75
        }

        @GUI::DialogButton {
            name: "cancel_button"
            text: "Cancel"
            fixed_width: 75
        }
    }
}
