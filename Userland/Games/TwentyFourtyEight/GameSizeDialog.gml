@TwentyFourtyEight::GameSizeDialogWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::Label {
            text: "Board size:"
            text_alignment: "CenterLeft"
        }

        @GUI::SpinBox {
            name: "board_size_spinbox"
            max: 100
            min: 2
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::Label {
            text: "Target tile:"
            text_alignment: "CenterLeft"
        }

        @GUI::Label {
            name: "tile_value_label"
            text_alignment: "CenterRight"
        }

        @GUI::SpinBox {
            name: "target_spinbox"
            min: 3
        }
    }

    @GUI::CheckBox {
        name: "evil_ai_checkbox"
        text: "Evil AI"
    }

    @GUI::CheckBox {
        name: "temporary_checkbox"
        text: "Temporarily apply changes"
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
        }

        @GUI::Button {
            name: "ok_button"
            text: "OK"
        }
    }
}
