@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {}

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "if..."
            fixed_width: 40
        }

        @GUI::TextEditor {
            name: "formula_editor"
            fixed_height: 25
            tooltip: "Use 'value' to refer to the current cell's value"
            font_type: "FixedWidth"
        }
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Foreground..."
            fixed_width: 150
        }

        @GUI::ColorInput {
            name: "foreground_input"
        }
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "Background..."
            fixed_width: 150
        }

        @GUI::ColorInput {
            name: "background_input"
        }
    }
}
