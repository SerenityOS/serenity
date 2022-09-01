@GUI::Frame {
    shape: "Container"
    shadow: "Raised"
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        name: "emojis"
        layout: @GUI::VerticalBoxLayout {}
    }
}
