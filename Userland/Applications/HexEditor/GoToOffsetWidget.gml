@HexEditor::GoToOffsetWidget {
    name: "main"
    fixed_width: 300
    fixed_height: 80
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
        margins: [0]
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 2
            margins: [2]
        }

        @GUI::Label {
            text: "Offset"
            text_alignment: "CenterLeft"
            fixed_width: 50
        }

        @GUI::TextBox {
            name: "text_editor"
            fixed_width: 100
        }

        @GUI::ComboBox {
            name: "offset_type"
            fixed_width: 100
        }

        @GUI::Button {
            name: "go_button"
            text: "Go"
            fixed_width: 40
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 2
            margins: [2]
        }

        @GUI::Label {
            text: "From"
            text_alignment: "CenterLeft"
            fixed_width: 50
        }

        @GUI::ComboBox {
            name: "offset_from"
            fixed_width: 100
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
        segment_count: 2
    }
}
