@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::HorizontalBoxLayout {}

    @GUI::Label {
        text: "Median filter radius"
        text_alignment: "CenterLeft"
        width: "shrink"
    }

    @GUI::SpinBox {
        name: "filter_radius"
        min: 1
        max: 5000
    }
}
