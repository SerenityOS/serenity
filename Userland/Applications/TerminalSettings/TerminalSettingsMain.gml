@TerminalSettings::MainWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Bell mode"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
            spacing: 8
        }

        @GUI::Widget {
            preferred_height: "fit"
            layout: @GUI::VerticalBoxLayout {
                spacing: 4
            }

            @GUI::RadioButton {
                name: "beep_bell_radio"
                text: "Audible bell"
            }

            @GUI::RadioButton {
                name: "visual_bell_radio"
                text: "Visible bell"
            }

            @GUI::RadioButton {
                name: "no_bell_radio"
                text: "No bell"
            }
        }
    }

    @GUI::GroupBox {
        title: "Exit behavior"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::CheckBox {
            name: "terminal_confirm_close"
            text: "Confirm exit when process is active"
        }
    }

    @GUI::GroupBox {
        title: "Auto-mark behavior"
        preferred_height: "fit"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::RadioButton {
            name: "automark_off"
            text: "Do not auto-mark"
        }

        @GUI::RadioButton {
            name: "automark_on_interactive_prompt"
            text: "Auto-mark on interactive shell prompts"
        }
    }
}
