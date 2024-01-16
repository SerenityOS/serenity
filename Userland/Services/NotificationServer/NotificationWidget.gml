@NotificationServer::NotificationWidget {
    layout: @GUI::HorizontalBoxLayout {
        margins: [8]
        spacing: 6
    }
    fill_with_background_color: true

    @GUI::ImageWidget {
        name: "icon"
        visible: false
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {}

        @GUI::Label {
            name: "title"
            font_weight: "Bold"
            text_alignment: "CenterLeft"
            preferred_height: "shrink"
            max_height: "shrink"
        }

        @GUI::Label {
            name: "text"
            text_alignment: "TopLeft"
        }
    }
}
