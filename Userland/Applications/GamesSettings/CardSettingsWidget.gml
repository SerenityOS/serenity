@GUI::Frame {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [8]
    }

    @GUI::GroupBox {
        title: "Background Color"
        max_height: "shrink"
        layout: @GUI::VerticalBoxLayout {
            margins: [8]
        }

        @GUI::ColorInput {
            name: "cards_background_color"
            has_alpha_channel: false
        }
    }
}
