@UsersSettings::ChangePasswordDialog {
    fixed_width: 280
    fixed_height: 85
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "New Password:"
            text_alignment: "CenterLeft"
            fixed_width: 120
        }

        @GUI::PasswordBox {
            name: "new_password_textbox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Confirm Password:"
            text_alignment: "CenterLeft"
            fixed_width: 120
        }

        @GUI::PasswordBox {
            name: "confirm_password_textbox"
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
