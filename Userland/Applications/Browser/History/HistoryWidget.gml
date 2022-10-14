@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::TextBox {
        name: "history_filter_textbox"
        placeholder: "Filter"
    }

    @GUI::TableView {
        name: "history_tableview"
    }
}
