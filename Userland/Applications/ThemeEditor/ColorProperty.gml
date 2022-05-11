@GUI::Frame {
    layout: @GUI::HorizontalBoxLayout {
        spacing: 4
    }
    shrink_to_fit: true

    @GUI::Label {
        name: "name"
        text: "Some color"
        text_alignment: "CenterLeft"
        fixed_width: 200
    }

    @GUI::ColorInput {
        name: "color_input"
    }
}
