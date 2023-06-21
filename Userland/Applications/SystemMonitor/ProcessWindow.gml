@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        margins: [4]
    }

    @GUI::Widget {
        shrink_to_fit: true
        layout: @GUI::HorizontalBoxLayout {
            margins: [4]
            spacing: 8
        }

        @GUI::ImageWidget {
            name: "process_icon"
            fixed_size: [32, 32]
        }

        @GUI::Label {
            name: "process_name"
            font_weight: "Bold"
            text_alignment: "CenterLeft"
            text: "This is the process name."
            preferred_width: "grow"
        }
    }

    @GUI::HorizontalSeparator {
        fixed_height: 2
    }

    @GUI::StackWidget {
        name: "widget_stack"

        @SystemMonitor::UnavailableProcessWidget {
            name: "unavailable_process"
        }

        @GUI::TabWidget {
            name: "available_process"

            @SystemMonitor::ProcessStateWidget {
                name: "process_state"
                title: "State"
            }

            @SystemMonitor::ProcessMemoryMapWidget {
                name: "memory_map"
                title: "Memory map"
            }

            @SystemMonitor::ProcessFileDescriptorMapWidget {
                name: "open_files"
                title: "Open files"
            }

            @SystemMonitor::ProcessUnveiledPathsWidget {
                name: "unveiled_paths"
                title: "Unveiled paths"
            }

            @SystemMonitor::ThreadStackWidget {
                name: "thread_stack"
                title: "Stack"
            }
        }
    }
}
