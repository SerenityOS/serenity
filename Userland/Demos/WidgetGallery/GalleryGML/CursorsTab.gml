@GUI::Widget {
    name: "cursors_tab"
    layout: @GUI::VerticalBoxLayout {
        margins: [4, 4, 4, 4]
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {
            margins: [8, 8, 8, 8]
        }

        @GUI::TableView {
            name: "cursors_tableview"
        }
    }
}
