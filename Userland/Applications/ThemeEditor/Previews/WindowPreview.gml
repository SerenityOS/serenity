@GUI::Frame {
    layout: @GUI::VerticalBoxLayout {}

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {
            margins: [8, 100, 8, 8]
        }

        @GUI::Button {
            text: "Button"
        }

        @GUI::CheckBox {
            text: "Check box"
        }

        @GUI::RadioButton {
            text: "Radio button"
        }

        @GUI::TextEditor {
            text: "Text editor\nwith multiple\nlines."
        }
    }

    @GUI::Statusbar {
        text: "Status bar"
    }
}
