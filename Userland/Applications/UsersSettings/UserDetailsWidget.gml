@UsersSettings::UserDetailsWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
        spacing: 4
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Username:"
            text_alignment: "CenterLeft"
            fixed_width: 120
        }

        @GUI::TextBox {
            name: "username_textbox"
            mode: "DisplayOnly"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Full Name:"
            text_alignment: "CenterLeft"
            fixed_width: 120
        }

        @GUI::TextBox {
            name: "full_name_textbox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Shell:"
            text_alignment: "CenterLeft"
            fixed_width: 120
        }

        @GUI::TextBox {
            name: "shell_textbox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Home Directory:"
            text_alignment: "CenterLeft"
            fixed_width: 120
        }

        @GUI::TextBox {
            name: "home_dir_textbox"
            mode: "DisplayOnly"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "UID / GID:"
            text_alignment: "CenterLeft"
            fixed_width: 120
        }

        @GUI::TextBox {
            name: "uid_gid_textbox"
            mode: "DisplayOnly"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Account Type:"
            text_alignment: "CenterLeft"
            fixed_width: 120
        }

        @GUI::ComboBox {
            name: "account_type_combobox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Layout::Spacer {}

        @GUI::Button {
            name: "change_password_button"
            text: "Change Password..."
            fixed_width: 150
        }
    }
}
