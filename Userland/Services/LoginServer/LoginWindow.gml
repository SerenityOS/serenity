@LoginServer::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @GUI::ImageWidget {
        name: "banner"
        bitmap: "/res/graphics/brand-banner.png"
        auto_resize: true
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {
            margins: [8, 8, 8, 8]
        }

        @GUI::TextBox {
            name: "username"
            placeholder: "Username"
        }

        @GUI::PasswordBox {
            name: "password"
            placeholder: "Password"
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout {}

            @GUI::Label {
                name: "fail_message"
                text_alignment: "CenterLeft"
            }

            @GUI::Layout::Spacer {}

            @GUI::Button {
                name: "log_in"
                text: "Log in"
                fixed_width: 60
            }
        }
    }
}
