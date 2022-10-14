@GUI::Frame {
    layout: @GUI::HorizontalBoxLayout {
        spacing: 4
    }
    preferred_height: "fit"

    @GUI::CheckBox {
        name: "checkbox"
        text: "Some flag"
        checkbox_position: "Right"
    }
}
