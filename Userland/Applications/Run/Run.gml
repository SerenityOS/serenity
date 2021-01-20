@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [10, 10, 10, 10]
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        @GUI::ImageWidget {
            name: "icon"
        }

        @GUI::Label {
            name: "info"
            text: "Type the name of a program, folder, document,\nor website, and SerenityOS will open it for you."
            text_alignment: "CenterLeft"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
        }

        @GUI::Label {
            text: "Open:"
            fixed_width: 50
            text_alignment: "CenterLeft"
        }

        @GUI::TextBox {
            name: "path"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
        }

        // HACK: using an empty widget as a spacer
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

        @GUI::Button {
            name: "browse_button"
            text: "Browse..."
            fixed_width: 75
        }
    }
}
