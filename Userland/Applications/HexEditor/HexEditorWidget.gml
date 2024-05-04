@HexEditor::HexEditorWidget {
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

        @GUI::DynamicWidgetContainer {
            name: "side_panel_container"
            visible: false
            section_label: "Panels"
            config_domain: "HexEditor"
            preferred_height: "grow"
            show_controls: false

            @GUI::DynamicWidgetContainer {
                name: "search_results_container"
                section_label: "Search Results"
                config_domain: "HexEditor"
                preferred_height: "grow"
                visible: false

                @GUI::TableView {
                    name: "search_results"
                }
            }

            @GUI::DynamicWidgetContainer {
                name: "value_inspector_container"
                section_label: "Value Inspector"
                config_domain: "HexEditor"
                preferred_height: "grow"
                visible: false

                @GUI::ToolbarContainer {
                    name: "value_inspector_toolbar_container"

                    @GUI::Toolbar {
                        name: "value_inspector_toolbar"

                        @GUI::Label {
                            text: "Mode:"
                            preferred_width: 40
                        }

                        @GUI::ComboBox {
                            name: "value_inspector_endianness"
                            preferred_width: 120
                        }
                    }
                }

                @GUI::TableView {
                    name: "value_inspector"
                    activates_on_selection: true
                }
            }

            @GUI::DynamicWidgetContainer {
                name: "annotations_container"
                section_label: "Annotations"
                config_domain: "HexEditor"
                preferred_height: "grow"
                visible: false

                @GUI::ToolbarContainer {
                    name: "annotations_toolbar_container"

                    @GUI::Toolbar {
                        name: "annotations_toolbar"
                    }
                }

                @GUI::TableView {
                    name: "annotations"
                }
            }
        }
    }

    @GUI::Statusbar {
        name: "statusbar"
        segment_count: 5
    }
}
