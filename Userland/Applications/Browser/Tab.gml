@GUI::Widget {
    layout: @GUI::VerticalBoxLayout {
    }

    @GUI::ToolbarContainer {
        name: "toolbar_container"

        @GUI::Toolbar {
            name: "toolbar"
        }
    }

    @GUI::Widget {
        name: "webview_container"
        layout: @GUI::VerticalBoxLayout {
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
    }
}
