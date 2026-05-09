@UsersSettings::UserAddDialog {
    fixed_width: 260
    fixed_height: 136
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Account Type:"
            text_alignment: "CenterLeft"
            fixed_width: 90
        }

        @GUI::ComboBox {
            name: "account_type_combobox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Full Name:"
            text_alignment: "CenterLeft"
            fixed_width: 90
        }

        @GUI::TextBox {
            name: "full_name_textbox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Username:"
            text_alignment: "CenterLeft"
            fixed_width: 90
        }

        @GUI::TextBox {
            name: "username_textbox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Password:"
            text_alignment: "CenterLeft"
            fixed_width: 90
        }

        @GUI::PasswordBox {
            name: "password_textbox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

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
