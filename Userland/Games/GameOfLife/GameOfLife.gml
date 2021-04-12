@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
    }

    @GUI::ToolbarContainer {

        @GUI::Toolbar {
            name: "toolbar"

            @GUI::Label {
                text: "Columns:"
                fixed_width: 60
            }

            @GUI::SpinBox {
                name: "columns_spinbox"
                min: 10
                max: 999
                fixed_width: 40
            }
            @GUI::VerticalSeparator {
            }
            @GUI::Label {
                text: "Rows:"
                fixed_width: 40
            }

            @GUI::SpinBox {
                name: "rows_spinbox"
                min: 10
                max: 999
                fixed_width: 40
            }
            @GUI::VerticalSeparator {
            }
            @GUI::Label {
                text: "Update Speed:"
                fixed_width: 90
            }
            @GUI::SpinBox {
                name: "interval_spinbox"
                min: 10
                max: 5000
                fixed_width: 60
            }
        }
    }

    @GUI::Widget {
        name: "board_widget_container"
        fill_with_background_color: true
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
