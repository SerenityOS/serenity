@Weather::SearchView {
    height: 400
    width: 600
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @GUI::Frame {
        @GUI::ScrollableContainerWidget {
            should_hide_unnecessary_scrollbars: true
            content_widget: @GUI::Widget {
                name: "content"
                layout: @GUI::VerticalBoxLayout {}
            }
        }
    }
}
