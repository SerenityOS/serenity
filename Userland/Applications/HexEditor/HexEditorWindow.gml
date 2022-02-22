@GUI::Widget {
    name: "main"
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolbarContainer {
        name: "toolbar_container"

        @GUI::Toolbar {
            name: "toolbar"
        }
    }

    @GUI::HorizontalSplitter {
        @HexEditor::HexEditor {
            name: "editor"
        }

        @GUI::Widget {
            name: "search_results_container"
            visible: false
            layout: @GUI::VerticalBoxLayout {}

            @GUI::TableView {
                name: "search_results"
            }
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
        segment_count: 5
    }
}
