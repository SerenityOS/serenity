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
        opportunistic_resizee: "First"

        @HexEditor::HexEditor {
            name: "editor"
        }

        @GUI::VerticalSplitter {
            name: "side_panel_container"
            visible: false

            @GUI::Widget {
                name: "search_results_container"
                visible: false
                layout: @GUI::VerticalBoxLayout {}

                @GUI::TableView {
                    name: "search_results"
                }
            }

            @GUI::Widget {
                name: "value_inspector_container"
                visible: false
                layout: @GUI::VerticalBoxLayout {}

                @GUI::TableView {
                    name: "value_inspector"
                    activates_on_selection: true
                }
            }
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
        segment_count: 5
    }
}
