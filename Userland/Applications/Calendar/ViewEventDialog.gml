@Calendar::ViewEventWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        fill_with_background_color: true
        layout: @GUI::VerticalBoxLayout {}
        name: "events_list"
        min_width: 100
    }

    @GUI::Button {
        name: "add_event_button"
        text: "Add Event"
    }
}
