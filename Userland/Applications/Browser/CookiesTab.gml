@GUI::Widget {
    name: "cookies_tab"
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::GroupBox {
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
        }

        @GUI::TableView {
            name: "cookies_tableview"
        }
    }
}
