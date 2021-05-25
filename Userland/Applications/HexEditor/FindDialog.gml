@GUI::Widget {
    name: "main"
    fixed_width: 280
    fixed_height: 146
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        spacing: 2
        margins: [4, 4, 4, 4]
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout

        @GUI::Label {
            text: "Value to find"
            fixed_width: 80
            text_alignment: "CenterLeft"
        }

        @GUI::TextBox {
            name: "text_editor"
            fixed_height: 20
        }
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout

        name: "radio_container"
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout

        @GUI::Button {
            name: "ok_button"
            text: "OK"
        }

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
        }
    }
}
