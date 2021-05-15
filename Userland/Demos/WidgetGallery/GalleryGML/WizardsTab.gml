@GUI::Widget {
    name: "wizards_tab"
    layout: @GUI::VerticalBoxLayout {
        margins: [4, 4, 4, 4]
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {
            margins: [8, 8, 8, 8]
        }

        @GUI::Button {
            name: "wizard_button"
            text: "Start wizard"
        }

        @GUI::HorizontalSeparator {
        }

        @GUI::TextEditor {
            name: "wizard_output"
            mode: "ReadOnly"
        }
    }
}
