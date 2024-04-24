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

    @GUI::Frame {
        name: "webdriver_banner"
        frame_style: "SunkenPanel"
        preferred_height: "shrink"
        foreground_role: "TooltipText"
        background_role: "Tooltip"
        fill_with_background_color: true
        visible: false
        layout: @GUI::HorizontalBoxLayout {
            margins: [0, 4]
        }

        @GUI::Label {
            text: "This Browser window is controlled by WebDriver."
            text_alignment: "CenterLeft"
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
