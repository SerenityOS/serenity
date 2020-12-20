@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
    }

    @GUI::ToolBarContainer {
        name: "toolbar_container"

        @GUI::ToolBar {
            name: "toolbar"
        }
    }

    @GUI::Widget {
        name: "webview_container"
        layout: @GUI::VerticalBoxLayout {
        }
    }

    @GUI::StatusBar {
        name: "statusbar"
    }
}
