@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
    }

    @GUI::ToolBarContainer {
        name: "toolbar_container"

        @GUI::ToolBar {
            name: "toolbar"
        }
    }

    @GUI::Calendar {
        name: "calendar"
    }
}
