@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        vertical_size_policy: "Fixed"
        preferred_height: 25

        @GUI::Label {
            text: "if..."
            horizontal_size_policy: "Fixed"
            vertical_size_policy: "Fixed"
            preferred_width: 40
            preferred_height: 25
        }

        @GUI::TextEditor {
            name: "formula_editor"
            horizontal_size_policy: "Fill"
            vertical_size_policy: "Fixed"
            preferred_height: 25
            tooltip: "Use 'value' to refer to the current cell's value"
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        vertical_size_policy: "Fixed"
        preferred_height: 25

        @GUI::Label {
            text: "Foreground..."
            horizontal_size_policy: "Fixed"
            vertical_size_policy: "Fixed"
            preferred_width: 150
            preferred_height: 25
        }

        @GUI::ColorInput {
            name: "foreground_input"
            vertical_size_policy: "Fixed"
            preferred_height: 25
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        vertical_size_policy: "Fixed"
        preferred_height: 25

        @GUI::Label {
            text: "Background..."
            horizontal_size_policy: "Fixed"
            vertical_size_policy: "Fixed"
            preferred_width: 150
            preferred_height: 25
        }

        @GUI::ColorInput {
            name: "background_input"
            vertical_size_policy: "Fixed"
            preferred_height: 25
        }
    }
}
