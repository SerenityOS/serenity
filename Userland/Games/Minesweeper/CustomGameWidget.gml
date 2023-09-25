@Minesweeper::CustomGameWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
        spacing: 6
    }

    @GUI::GroupBox {
        title: "Field"
        layout: @GUI::HorizontalBoxLayout {
            margins: [6]
        }

        @GUI::Label {
            text: "Columns: "
            autosize: true
        }

        @GUI::SpinBox {
            name: "columns_spinbox"
            min: 9
            max: 50
            fixed_width: 40
        }

        @GUI::Layout::Spacer {}

        @GUI::Label {
            text: "Rows: "
            autosize: true
        }

        @GUI::SpinBox {
            name: "rows_spinbox"
            min: 9
            max: 50
            fixed_width: 40
        }

        @GUI::Layout::Spacer {}

        @GUI::Label {
            text: "Mines: "
            autosize: true
        }

        @GUI::SpinBox {
            name: "mines_spinbox"
            min: 1
            max: 2500
            fixed_width: 50
        }
    }

    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
            spacing: 10
        }

        @GUI::Layout::Spacer {}

        @GUI::DialogButton {
            name: "ok_button"
            text: "OK"
        }

        @GUI::DialogButton {
            name: "cancel_button"
            text: "Cancel"
        }
    }
}
