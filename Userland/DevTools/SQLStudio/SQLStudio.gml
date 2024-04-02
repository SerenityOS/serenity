@SQLStudio::MainWidget {
    layout: @GUI::VerticalBoxLayout {}
    fill_with_background_color: true

    @GUI::ToolbarContainer {
        @GUI::Toolbar {
            name: "toolbar"
        }
    }

    @GUI::VerticalSplitter {
        @GUI::TabWidget {
            name: "script_tab_widget"
            reorder_allowed: true
            close_button_enabled: true
        }

        @GUI::TabWidget {
            name: "action_tab_widget"
            close_button_enabled: true
            visible: false
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
        segment_count: 3
    }
}
