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

    @GUI::Frame {
        name: "calendar_frame"
        layout: @GUI::VerticalBoxLayout {
            margins: [2, 2, 2, 2]
        }

        @GUI::Calendar {
            name: "calendar"
        }
    }
}
