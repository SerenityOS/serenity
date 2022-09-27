@GUI::Widget {
    fixed_width: 260
    fixed_height: 85
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Title:"
            text_alignment: "CenterLeft"
            fixed_width: 30
        }

        @GUI::TextBox {
            name: "title_textbox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "URL:"
            text_alignment: "CenterLeft"
            fixed_width: 30
        }

        @GUI::TextBox {
            name: "url_textbox"
        }
    }

    @GUI::Widget {
        fixed_height: 24
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Layout::Spacer {}

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
    }
}
