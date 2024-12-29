@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {}

    // Add a tasteful separating line between the menu and the main UI.
    @GUI::HorizontalSeparator {
        fixed_height: 2
    }

    @GUI::Widget {
        layout: @GUI::VerticalBoxLayout {
            margins: [0, 4, 4]
        }

        @GUI::TabWidget {
            name: "main_tabs"

            @GUI::Widget {
                title: "Processes"
                name: "processes"
                layout: @GUI::VerticalBoxLayout {
                    margins: [4]
                    spacing: 0
                }

                @GUI::TreeView {
                    name: "process_table"
                    column_headers_visible: true
                    should_fill_selected_rows: true
                    selection_behavior: "SelectRows"
                }
            }

            @GUI::Widget {
                title: "Performance"
                name: "performance"
                background_role: "Button"
                fill_with_background_color: true
                layout: @GUI::VerticalBoxLayout {
                    margins: [4]
                }

                @GUI::GroupBox {
                    title: "CPU usage"
                    name: "cpu_graph"
                    preferred_height: "opportunistic_grow"
                    layout: @GUI::VerticalBoxLayout {}
                }

                @GUI::GroupBox {
                    title: "Memory usage"
                    preferred_height: "opportunistic_grow"
                    min_height: 120
                    layout: @GUI::VerticalBoxLayout {
                        margins: [6]
                    }

                    @SystemMonitor::GraphWidget {
                        stack_values: true
                        name: "memory_graph"
                    }
                }

                @SystemMonitor::MemoryStatsWidget {
                    name: "memory_stats"
                    memory_graph: "memory_graph"
                }
            }

            @SystemMonitor::StorageTabWidget {
                title: "Storage"
                name: "storage"
                layout: @GUI::VerticalBoxLayout {
                    margins: [4]
                }

                @GUI::TableView {
                    name: "storage_table"
                }
            }

            @SystemMonitor::NetworkStatisticsWidget {
                title: "Network"
                name: "network"
            }
        }
    }

    @GUI::Statusbar {
        segment_count: 3
        name: "statusbar"
    }
}
