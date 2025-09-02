@GUI::Widget {
    fill_with_background_color: true
    layout: @GUI::VerticalBoxLayout {
        spacing: 2
    }

    @GUI::ToolbarContainer {
        name: "toolbar_container"

        @GUI::Toolbar {
            name: "main_toolbar"
            collapsible: true
            grouped: true
        }

        @GUI::Toolbar {
            name: "breadcrumb_toolbar"

            @GUI::Label {
                text: "Location: "
                autosize: true
            }

            @GUI::PathBreadcrumbbar {
                name: "breadcrumbbar"
            }
        }
    }

    @GUI::HorizontalSplitter {
        name: "splitter"

        @GUI::TreeView {
            name: "tree_view"
            preferred_width: 175
        }
    }

    @GUI::Statusbar {
        name: "statusbar"

        @GUI::Progressbar {
            name: "progressbar"
            text: "Generating thumbnails: "
            preferred_width: 200
            visible: false
        }

        @GUI::Slider {
            name: "icon_view_size"
            orientation: "Horizontal"
            max: 240
            min: 16
            preferred_width: 100
            visible: false
        }
    }
}
