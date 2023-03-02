@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Bell Mode"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
            spacing: 8
        }

        @GUI::Label {
            text: "This setting controls the terminal's indication of an ANSI 0x07 bell (\\a)."
            text_alignment: "TopLeft"
        }

        @GUI::Widget {
            preferred_height: "fit"
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
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::CheckBox {
            name: "terminal_show_scrollbar"
            text: "Show scrollbar"
        }

        @GUI::SpinBox {
            name: "history_size_spinbox"
            min: 0
            max: 40960
            orientation: "Horizontal"
        }
    }

    @GUI::GroupBox {
        title: "Exit Behaviour"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::CheckBox {
            name: "terminal_confirm_close"
            text: "Ask before closing if processes are running in the terminal"
        }
    }
}
