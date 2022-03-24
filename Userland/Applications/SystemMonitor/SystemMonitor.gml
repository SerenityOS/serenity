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

                @GUI::TableView {
                    name: "process_table"
                    column_headers_visible: true
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
                    layout: @GUI::VerticalBoxLayout {}
                }

                @GUI::GroupBox {
                    title: "Memory usage"
                    fixed_height: 120
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

            @SystemMonitor::HardwareTabWidget {
                title: "Hardware"
                name: "hardware"
                layout: @GUI::VerticalBoxLayout {
                    margins: [4]
                }

                @GUI::GroupBox {
                    title: "CPUs"
                    fixed_height: 128
                    layout: @GUI::VerticalBoxLayout {
                        margins: [6]
                    }

                    @GUI::TableView {
                        name: "cpus_table"
                    }
                }

                @GUI::GroupBox {
                    title: "PCI devices"
                    layout: @GUI::VerticalBoxLayout {
                        margins: [6]
                    }

                    @GUI::TableView {
                        name: "pci_dev_table"
                    }
                }
            }
        }
    }

    @GUI::Statusbar {
        segment_count: 3
        name: "statusbar"
    }
}
