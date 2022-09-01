@GUI::Frame {
    shape: "Container"
    shadow: "Raised"
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::ScrollableContainerWidget {
        name: "scrollable_container"
        content_widget: @GUI::Widget {
            name: "emojis"
            layout: @GUI::VerticalBoxLayout {}
        }
    }
}
