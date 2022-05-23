@GUI::Widget {
    layout: @GUI::HorizontalBoxLayout {}
    fill_with_background_color: true

    @GUI::Frame {
        layout: @GUI::HorizontalBoxLayout {}
        name: "preview_frame"
    }

    @GUI::TabWidget {
        name: "property_tabs"
    }
}
