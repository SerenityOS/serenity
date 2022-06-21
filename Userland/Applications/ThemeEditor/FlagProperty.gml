@GUI::Frame {
    layout: @GUI::HorizontalBoxLayout {
        spacing: 4
    }
    shrink_to_fit: true

    @GUI::CheckBox {
        name: "checkbox"
        text: "Some flag"
        checkbox_position: "Right"
    }
}
