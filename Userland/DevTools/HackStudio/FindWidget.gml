@GUI::Widget {
    name: "find_widget"
    fill_with_background_color: true
    shrink_to_fit: true
    visible: false
    layout: @GUI::HorizontalBoxLayout {
        margins: [0, 0]
    }

    @GUI::TextBox {
        name: "input_field"
        max_width: 250
    }

    @GUI::Label {
        name: "index_label"
        max_width: 30
        text: ""
    }

    @GUI::Button {
        name: "next"
        icon: "/res/icons/16x16/go-down.png"
        max_width: 15
    }

    @GUI::Button {
        name: "previous"
        icon: "/res/icons/16x16/go-up.png"
        max_width: 15
    }
}
