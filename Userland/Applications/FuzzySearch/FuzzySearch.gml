@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
        margins: [2, 2, 2, 2]
    }
    @GUI::ListView{
        name: "haystack_listview"
    }
    @GUI::Widget {
        layout: @GUI::HorizontalBoxLayout {
        }
        fixed_height: 20
        @GUI::Label {
            name: "needle_label"
            text: "(0/0) >"
            autosize: true
            text_alignment: "CenterLeft"
        }
        @GUI::TextBox {
            name: "needle_textbox"
            autosize: true
        }
    }
}
