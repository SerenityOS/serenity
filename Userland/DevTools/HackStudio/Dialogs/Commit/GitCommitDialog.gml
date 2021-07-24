@GUI::Frame {
    fixed_height: 260
    fixed_width: 400
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
        placeholder: ""
        fixed_height: 200
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout
        shrink_to_fit: true

        @GUI::Widget {}

        @GUI::Button {
            name: "commit_button"
            text: "Commit"
            fixed_width: 75
        }

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
            fixed_width: 75
        }
    }
}
