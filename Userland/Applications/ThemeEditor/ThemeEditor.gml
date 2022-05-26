@GUI::Widget {
    layout: @GUI::HorizontalBoxLayout {}
    fill_with_background_color: true

    @GUI::Frame {
        layout: @GUI::HorizontalBoxLayout {}
        name: "preview_frame"
        fixed_width: 400
    }

    @GUI::TabWidget {
        name: "property_tabs"
        min_width: 300
    }
}
