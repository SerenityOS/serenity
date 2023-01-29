@GUI::Frame {
    layout: @GUI::HorizontalBoxLayout {
        spacing: 4
    }
    preferred_height: "fit"

    @GUI::Label {
        name: "name"
        text: "Some alignment"
        text_alignment: "CenterLeft"
        fixed_width: 200
    }

    @GUI::ComboBox {
        name: "combo_box"
        model_only: true
    }
}
