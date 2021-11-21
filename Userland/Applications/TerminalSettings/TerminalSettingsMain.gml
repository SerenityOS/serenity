@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Bell Mode"
        shrink_to_fit: false
        fixed_height: 160

        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
            spacing: 16
        }

        @GUI::Label {
            text: "This setting controls the terminal's indication of an ANSI 0x07 bell (\\a)."
            text_alignment: "TopLeft"
        }

        @GUI::Widget {
            shrink_to_fit: true

            layout: @GUI::VerticalBoxLayout {
                spacing: 4
            }

            @GUI::RadioButton {
                name: "beep_bell_radio"
                text: "System beep"
            }

            @GUI::RadioButton {
                name: "visual_bell_radio"
                text: "Visual bell"
            }

            @GUI::RadioButton {
                name: "no_bell_radio"
                text: "No bell"
            }
        }
    }

    @GUI::GroupBox {
        title: "Scrollback Size (Lines)"
        shrink_to_fit: true

        layout: @GUI::VerticalBoxLayout {
            margins: [16, 8, 8]
        }

        @GUI::SpinBox {
            name: "history_size_spinbox"
            min: 0
            max: 40960
            orientation: "Horizontal"
        }
    }
}
