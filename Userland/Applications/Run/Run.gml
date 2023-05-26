@Run::MainWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        max_height: 32
        layout: @GUI::HorizontalBoxLayout {
            spacing: 16
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
            margins: [4]
        }

        @GUI::Label {
            text: "Open:"
            fixed_width: 50
            text_alignment: "CenterLeft"
        }

        @GUI::ComboBox {
            name: "path"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        fixed_height: 22

        @GUI::Layout::Spacer {}

        @GUI::DialogButton {
            name: "ok_button"
            text: "OK"
        }

        @GUI::DialogButton {
            name: "cancel_button"
            text: "Cancel"
        }

        @GUI::DialogButton {
            name: "browse_button"
            text: "Browse..."
        }
    }
}
