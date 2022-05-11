@GUI::Frame {
    layout: @GUI::HorizontalBoxLayout {
        spacing: 4
    }
    shrink_to_fit: true

    @GUI::Label {
        name: "name"
        text: "Some path"
        text_alignment: "CenterLeft"
        fixed_width: 200
    }

    @GUI::TextBox {
        name: "path_input"
    }

    @GUI::Button {
        name: "path_picker_button"
        fixed_width: 20
        text: "..."
        tooltip: "Choose..."
    }
}
