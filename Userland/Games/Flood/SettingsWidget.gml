@Flood::SettingsWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::Label {
            text: "Board rows:"
            text_alignment: "CenterLeft"
        }

        @GUI::SpinBox {
            name: "board_rows_spinbox"
            max: 32
            min: 2
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 4
        }

        @GUI::Label {
            text: "Board columns:"
            text_alignment: "CenterLeft"
        }

        @GUI::SpinBox {
            name: "board_columns_spinbox"
            max: 32
            min: 2
        }
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
