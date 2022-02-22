@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {}
    preferred_height: "fit"

    @GUI::Widget {
        preferred_height: "fit"
        layout: @GUI::HorizontalBoxLayout {}

        @GUI::Label {
            text: "if..."
            fixed_width: 40
        }

        @GUI::TextEditor {
            name: "formula_editor"
            fixed_height: 32
            tooltip: "Use 'value' to refer to the current cell's value"
            font_type: "FixedWidth"
        }
    }

    @GUI::Widget {
        preferred_height: "fit"
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
        preferred_height: "fit"
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
