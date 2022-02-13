@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::HorizontalBoxLayout {
        margins: [8]
        spacing: 8
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::VerticalBoxLayout {}

        @GUI::Label {
            name: "key_icon_label"
            fixed_height: 32
            fixed_width: 32
        }
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {}

        @GUI::Widget {
            fixed_height: 24
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
                text: "Server:"
                fixed_width: 80
                text_alignment: "CenterLeft"
            }

            @GUI::Label {
                name: "server_label"
                text: "server.ip"
                text_alignment: "CenterLeft"
            }
        }

        @GUI::Widget {
            fixed_height: 24
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
                text: "Username:"
                fixed_width: 80
                text_alignment: "CenterLeft"
            }

            @GUI::Label {
                name: "username_label"
                text: "username"
                text_alignment: "CenterLeft"
            }
        }

        @GUI::Widget {
            fixed_height: 24
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
                text: "Password:"
                fixed_width: 80
                text_alignment: "CenterLeft"
            }

            @GUI::PasswordBox {
                name: "password_box"
            }
        }

        @GUI::Widget {}

        @GUI::Widget {
            shrink_to_fit: true
            layout: @GUI::HorizontalBoxLayout {
                spacing: 6
            }

            @GUI::Widget {}

            @GUI::Button {
                text: "OK"
                name: "ok_button"
                fixed_width: 75
            }

            @GUI::Button {
                text: "Cancel"
                name: "cancel_button"
                fixed_width: 75
            }
        }
    }
}
