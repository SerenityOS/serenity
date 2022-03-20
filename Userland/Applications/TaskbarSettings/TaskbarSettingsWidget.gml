@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [10]
        spacing: 5
    }

    @GUI::GroupBox {
        title: "Window List"
        shrink_to_fit: true
        layout: @GUI::VerticalBoxLayout {
            margins: [6]
        }

        @GUI::CheckBox {
            name: "close_on_middle_click"
            text: "Close window with middle click"
        }
    }
}
