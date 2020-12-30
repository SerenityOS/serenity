@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        fixed_height: 25

        @GUI::Label {
            text: "if..."
            fixed_width: 40
            fixed_height: 25
        }

        @GUI::TextEditor {
            name: "formula_editor"
            fixed_height: 25
            tooltip: "Use 'value' to refer to the current cell's value"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        fixed_height: 25

        @GUI::Label {
            text: "Foreground..."
            fixed_width: 150
            fixed_height: 25
        }

        @GUI::ColorInput {
            name: "foreground_input"
            fixed_height: 25
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        fixed_height: 25

        @GUI::Label {
            text: "Background..."
            fixed_width: 150
            fixed_height: 25
        }

        @GUI::ColorInput {
            name: "background_input"
            fixed_height: 25
        }
    }
}
