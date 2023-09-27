@GameOfLife::MainWidget {
    layout: @GUI::VerticalBoxLayout {}

    @GUI::ToolbarContainer {
        @GUI::Toolbar {
            name: "toolbar"

            @GUI::Label {
                text: "Columns: "
                autosize: true
            }

            @GUI::SpinBox {
                name: "columns_spinbox"
                min: 10
                max: 999
                fixed_width: 40
            }

            @GUI::VerticalSeparator {}

            @GUI::Label {
                text: "Rows: "
                autosize: true
            }

            @GUI::SpinBox {
                name: "rows_spinbox"
                min: 10
                max: 999
                fixed_width: 40
            }

            @GUI::VerticalSeparator {}

            @GUI::Label {
                text: "Speed: "
                autosize: true
            }

            @GUI::SpinBox {
                name: "interval_spinbox"
                min: 10
                max: 5000
                fixed_width: 60
            }

            @GUI::Label {
                text: " ms"
                autosize: true
            }

            @GUI::VerticalSeparator {}
        }
    }

    @GUI::Frame {
        name: "board_widget_container"
        fill_with_background_color: true
    }

    @GUI::Statusbar {
        name: "statusbar"
        segment_count: 2
    }
}
