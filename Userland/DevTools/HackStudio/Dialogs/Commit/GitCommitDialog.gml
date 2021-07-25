@GUI::Frame {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        spacing: 3
        margins: [4, 4, 4, 4]
    }

    @GUI::Label {
        text: "Enter commit message:"
        text_alignment: "CenterLeft"
        fixed_height: 20
    }

    @GUI::TextEditor {
        name: "message_editor"
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout
        shrink_to_fit: true

        @GUI::Label {
            name: "line_and_column_label"
            text: "Line: 1, Col: 0"
            text_alignment: "CenterLeft"
            fixed_height: 20
        }

        @GUI::Widget

        @GUI::Button {
            name: "commit_button"
            text: "Commit"
            tooltip: "Commit (Ctrl+Enter)"
            fixed_width: 75
        }

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
            fixed_width: 75
        }
    }
}
