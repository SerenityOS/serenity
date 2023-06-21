@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @GUI::ToolbarContainer {
        @GUI::Toolbar {
            layout: @GUI::HorizontalBoxLayout {
                margins: [0, 4]
            }

            @GUI::Label {
                text: "Device: "
                autosize: true
            }

            @GUI::ComboBox {
                name: "device_combobox"
                fixed_width: 100
            }
        }
    }

    @GUI::TableView {
        name: "partition_table_view"
    }
}
