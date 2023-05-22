@GUI::Widget {
    name: "wizards_tab"
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
        }

        @GUI::Button {
            name: "wizard_button"
            text: "Start Wizard"
        }

        @GUI::HorizontalSeparator {}

        @GUI::TextEditor {
            name: "wizard_output"
            mode: "ReadOnly"
        }
    }
}
