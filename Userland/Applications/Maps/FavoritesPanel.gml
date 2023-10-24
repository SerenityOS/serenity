@Maps::FavoritesPanel {
    min_width: 100
    preferred_width: 200
    max_width: 350
    layout: @GUI::VerticalBoxLayout {}

    // Empty and favorites are toggled in visibility
    @GUI::Frame {
        name: "empty_container"
        frame_style: "SunkenPanel"
        layout: @GUI::VerticalBoxLayout {
            margins: [4]
        }

        @GUI::Label {
            text: "You don't have any favorite places"
            text_alignment: "CenterLeft"
        }
    }

    @GUI::ListView {
        name: "favorites_list"
        horizontal_padding: 6
        vertical_padding: 4
        should_hide_unnecessary_scrollbars: true
        alternating_row_colors: false
    }
}
