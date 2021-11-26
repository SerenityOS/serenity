@GUI::Widget {
    fill_with_background_color: true

    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::GroupBox {
        title: "Field"
        autosize: true

        layout: @GUI::HorizontalBoxLayout {
            margins: [16, 6, 6]
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

        @GUI::VerticalSeparator {
        }
        
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

        @GUI::VerticalSeparator {
        }

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
        max_height: 24

        layout: @GUI::HorizontalBoxLayout {
        }

        @GUI::Widget {
        }

        @GUI::Button {
            name: "ok_button"
            text: "OK"
            max_width: 75
        }

        @GUI::Button {
            name: "cancel_button"
            text: "Cancel"
            max_width: 75
        }
    }
}
