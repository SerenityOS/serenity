@Calendar::CalendarWidget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    @GUI::ToolbarContainer {
        name: "toolbar_container"

        @GUI::Toolbar {
            name: "toolbar"
        }
    }

    @Calendar::EventCalendar {
        name: "calendar"
    }
}
