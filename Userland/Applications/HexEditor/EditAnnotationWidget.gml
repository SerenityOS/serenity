@HexEditor::EditAnnotationWidget {
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }
    fill_with_background_color: true

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            margins: [4]
        }
        preferred_height: "fit"

        @GUI::Label {
            text: "Start offset:"
            text_alignment: "CenterLeft"
        }

        @GUI::NumericInput {
            name: "start_offset"
            min: 0
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            margins: [4]
        }
        preferred_height: "fit"

        @GUI::Label {
            text: "End offset:"
            text_alignment: "CenterLeft"
        }

        @GUI::NumericInput {
            name: "end_offset"
            min: 0
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            margins: [4]
        }
        preferred_height: "fit"

        @GUI::Label {
            text: "Color:"
            text_alignment: "CenterLeft"
        }

        @GUI::ColorInput {
            name: "background_color"
        }
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {
            margins: [4]
        }
        preferred_height: "fit"

        @GUI::Label {
            text: "Comments:"
            text_alignment: "CenterLeft"
        }

        @GUI::TextEditor {
            name: "comments"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            margins: [4]
        }
        preferred_height: "fit"

        @GUI::Layout::Spacer {}

        @GUI::DialogButton {
            name: "save_button"
            text: "Save"
        }

        @GUI::DialogButton {
            name: "cancel_button"
            text: "Cancel"
        }
    }
}
