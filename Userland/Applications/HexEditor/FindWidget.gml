@HexEditor::FindWidget {
    name: "main"
    fixed_width: 280
    fixed_height: 146
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
        margins: [4]
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        fixed_height: 22

        @GUI::Label {
            text: "Value to find:"
            fixed_width: 80
            text_alignment: "CenterLeft"
        }

        @GUI::TextBox {
            name: "text_editor"
        }
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {}
        name: "radio_container"
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {}
        fixed_height: 22

        @GUI::Button {
            name: "find_button"
            text: "Find"
        }

        @GUI::Button {
            name: "find_all_button"
            text: "Find All"
        }

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
        }
    }
}
