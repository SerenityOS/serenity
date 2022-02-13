@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolbarContainer {
        name: "toolbar_container"

        @GUI::Toolbar {
            name: "toolbar"
        }
    }

    @GUI::Widget {
        name: "webview_container"
        layout: @GUI::VerticalBoxLayout {}
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
