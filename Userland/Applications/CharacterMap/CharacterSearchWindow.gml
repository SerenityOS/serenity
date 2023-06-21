@GUI::Frame {
    layout: @GUI::VerticalBoxLayout {
        margins: [2, 0, 0, 0]
    }
    fill_with_background_color: true

    @GUI::TextBox {
        name: "search_input"
    }

    @GUI::TableView {
        name: "results_table"
    }
}
