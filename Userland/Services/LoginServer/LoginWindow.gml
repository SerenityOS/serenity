@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout

    @GUI::ImageWidget {
        name: "banner"
        auto_resie: true
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {
            margins: [8, 8, 8, 8]
        }

        @GUI::TextBox {
            name: "username"
            placeholder: "username"
        }

        @GUI::PasswordBox {
            name: "password"
            placeholder: "password"
        }

        @GUI::Widget {
            layout: @GUI::HorizontalBoxLayout

            @GUI::Widget

            @GUI::Button {
                name: "log_in"
                text: "Log in"
                fixed_width: 60
            }
        }
    }
}
